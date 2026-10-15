package com.keytech.xuehua.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.keytech.base.service.impl.BaseServiceImpl;
import com.keytech.xuehua.entity.XuehuaPartUsage;
import com.keytech.xuehua.mapper.XuehuaPartUsageMapper;
import com.keytech.xuehua.service.IXuehuaPartUsageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 备件使用报表 服务实现（说明书 3.5.3）
 *
 * <p>数据来源 = 出库明细行（已拣配）：不在出库确认链路里插使用记录，
 * 而是查列表时幂等回填（INSERT...SELECT WHERE NOT EXISTS + detail_id 唯一索引兜底）——
 * 出库核心链路零改动，历史数据天然补齐，两条路径（部署 SQL / 列表触发）同一条语句。
 *
 * <p>为什么快照：领用人/产线/类别从出库单拷贝——出库时刻的领用事实
 * 不随主数据改名变化（与茅台"台账=唯一事实源"稳定键关联同思路）。
 */
@Slf4j
@Service
public class XuehuaPartUsageServiceImpl extends BaseServiceImpl<XuehuaPartUsageMapper, XuehuaPartUsage> implements IXuehuaPartUsageService {

    /** 超期未确认阈值（天）——超 1 周未使用 */
    private static final int OVER_DAYS = 7;

    @Override
    public List<Map<String, Object>> usageRows(String keyword, String status, Date from, Date to) {
        // 幂等回填（量级=已拣配行数，毫秒级；失败不阻断查询，下轮再补）
        try {
            baseMapper.insertFromOutbound();
        } catch (Exception e) {
            log.warn("使用记录回填失败（并发冲突可忽略）: {}", e.getMessage());
        }
        List<XuehuaPartUsage> rows = this.list(new QueryWrapper<XuehuaPartUsage>().orderByDesc("out_time", "id"));
        Date now = new Date();
        List<Map<String, Object>> result = new java.util.ArrayList<>(rows.size());
        for (XuehuaPartUsage u : rows) {
            int days = u.getOutTime() == null ? 0 : (int) Math.floorDiv(now.getTime() - u.getOutTime().getTime(), 86400000L);
            boolean over = (u.getStatus() == null || u.getStatus() != 1) && days > OVER_DAYS;
            Map<String, Object> r = new LinkedHashMap<>(16);
            r.put("id", u.getId());
            r.put("woNo", u.getWoNo());
            r.put("partCode", u.getPartCode());
            r.put("partName", u.getPartName());
            r.put("category", u.getCategory());
            r.put("workshop", u.getWorkshop());
            r.put("receiver", u.getReceiver());
            r.put("qty", u.getQty());
            r.put("outTime", u.getOutTime());
            r.put("days", days);
            r.put("usedQty", u.getUsedQty());
            r.put("purpose", u.getPurpose());
            r.put("photos", u.getPhotos());
            r.put("status", u.getStatus());
            r.put("statusText", u.getStatus() != null && u.getStatus() == 1 ? "已使用"
                    : over ? "超1周未使用"
                    : u.getStatus() != null && u.getStatus() == 2 ? "未使用" : "待确认");
            r.put("remindCount", u.getRemindCount());
            r.put("lastRemindTime", u.getLastRemindTime());
            // 过滤：关键字 / 状态（over 为派生态） / 出库日期区间
            if (StrUtil.isNotBlank(keyword)) {
                String k = keyword.trim().toLowerCase();
                String hay = String.join(" ", StrUtil.nullToEmpty(u.getPartCode()), StrUtil.nullToEmpty(u.getPartName()),
                        StrUtil.nullToEmpty(u.getCategory()), StrUtil.nullToEmpty(u.getWorkshop()),
                        StrUtil.nullToEmpty(u.getReceiver()), StrUtil.nullToEmpty(u.getWoNo())).toLowerCase();
                if (!hay.contains(k)) {
                    continue;
                }
            }
            if ("over".equals(status) && !over) {
                continue;
            }
            if (status != null && !"over".equals(status) && !String.valueOf(u.getStatus()).equals(status)) {
                continue;
            }
            if (from != null && u.getOutTime() != null && u.getOutTime().before(from)) {
                continue;
            }
            if (to != null && u.getOutTime() != null && u.getOutTime().after(to)) {
                continue;
            }
            result.add(r);
        }
        return result;
    }

    /**
     * 确认用途：先查给友好提示，再条件更新 WHERE status=0 防并发双确认
     * （与处置审批/PTL 按键确认同款并发口径）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Integer id, boolean used, Integer usedQty, String photos, String purpose, String operator) {
        XuehuaPartUsage u = baseMapper.selectById(id);
        if (u == null) {
            throw new IllegalStateException("使用记录不存在");
        }
        if (u.getStatus() != null && u.getStatus() != 0) {
            throw new IllegalStateException("该记录已确认过（已使用/未使用）");
        }
        if (used) {
            if (usedQty == null || usedQty < 1) {
                throw new IllegalStateException("请填写实际使用数量");
            }
            if (u.getQty() != null && usedQty > u.getQty()) {
                throw new IllegalStateException("实际使用数量不能超过领用数量 " + u.getQty());
            }
        }
        Date now = new Date();
        int updated = baseMapper.update(null, new UpdateWrapper<XuehuaPartUsage>()
                .eq("id", id).eq("status", 0)
                .set("status", used ? 1 : 2)
                .set("used_qty", used ? usedQty : 0)
                .set("photos", photos)
                .set("purpose", StrUtil.blankToDefault(purpose, used ? "" : "未使用"))
                .set("confirm_by", StrUtil.blankToDefault(operator, "管理员"))
                .set("confirm_time", now)
                .set("update_time", now));
        if (updated == 0) {
            throw new IllegalStateException("该记录已确认过（已使用/未使用）");
        }
    }

    @Override
    public int remind(List<Integer> ids) {
        UpdateWrapper<XuehuaPartUsage> uw = new UpdateWrapper<XuehuaPartUsage>()
                .ne("status", 1)
                .setSql("remind_count = COALESCE(remind_count, 0) + 1")
                .set("last_remind_time", new Date())
                .set("update_time", new Date());
        if (ids != null && !ids.isEmpty()) {
            uw.in("id", ids);
        }
        return baseMapper.update(null, uw);
    }

    @Override
    public int remindAll() {
        return remind(null);
    }

    /** 供统计卡使用的计数（暂留扩展） */
    public Map<String, Object> quickStat() {
        Map<String, Object> m = new HashMap<>(4);
        m.put("total", this.count(new QueryWrapper<XuehuaPartUsage>()));
        m.put("pending", this.count(new QueryWrapper<XuehuaPartUsage>().eq("status", 0)));
        return m;
    }
}
