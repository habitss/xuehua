package com.keytech.xuehua.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.keytech.base.service.impl.BaseServiceImpl;
import com.keytech.xuehua.entity.XuehuaDisposal;
import com.keytech.xuehua.mapper.XuehuaAgingMapper;
import com.keytech.xuehua.mapper.XuehuaDisposalMapper;
import com.keytech.xuehua.service.IXuehuaAgingService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 库龄分析 服务实现（说明书 3.5.2：库龄分布 / 超期预警 / 分类分析 + 原型处置审批流）
 *
 * <p>库龄行 = 在库库位（bin.status≠0 且 qty>0），库龄 = 该库位最早确认上架距今
 * （MIN putaway_time，与出库 FIFO 同源——用户拍板口径）。
 * 状态分档：&gt;90 超期 / 60-90 偏高 / &lt;60 正常（说明书只定义 >90 超期，偏高为原型沿用）。
 *
 * <p>为什么要有这一层：处置审批（先查重再插入、状态流转）是跨多条语句的业务动作，
 * 必须有事务边界——原来写在 Controller 里没有 @Transactional，将来二期
 * "审批通过联动库存扣减"时就会踩半截状态的坑（项目在出库链式点亮上真实踩过）。
 */
@Slf4j
@Service
public class XuehuaAgingServiceImpl extends BaseServiceImpl<XuehuaDisposalMapper, XuehuaDisposal> implements IXuehuaAgingService {

    /** 超期阈值（天）——说明书 3.5.2 */
    private static final int OVER_DAYS = 90;
    /** 偏高阈值（天）——原型沿用 */
    private static final int WARN_DAYS = 60;

    @Resource
    private XuehuaAgingMapper agingMapper;

    @Override
    public List<Map<String, Object>> agingRows(String keyword, Integer status) {
        List<Map<String, Object>> rows = decorate(agingMapper.agingRows());
        if (StrUtil.isNotBlank(keyword)) {
            String k = keyword.trim().toLowerCase();
            rows.removeIf(r -> !String.join(" ",
                    str(r.get("binCode")), str(r.get("partCode")), str(r.get("partName")),
                    str(r.get("category")), str(r.get("zoneCode"))).toLowerCase().contains(k));
        }
        if (status != null) {
            rows.removeIf(r -> !status.equals(r.get("status")));
        }
        return rows;
    }

    @Override
    public Map<String, Object> stat() {
        List<Map<String, Object>> rows = decorate(agingMapper.agingRows());
        Map<String, Object> result = new LinkedHashMap<>(8);

        // KPI：在库件数/在库库位/超期件数/超期库位/最高库龄
        // 注意必须 r -> toLong(r.get("qty"))：方法引用 toLong 会把整个 Map 传进去（2026-09-28 页面回归抓过）
        long pieces = rows.stream().mapToLong(r -> toLong(r.get("qty"))).sum();
        long overPieces = rows.stream().filter(r -> r.get("status").equals(2)).mapToLong(r -> toLong(r.get("qty"))).sum();
        long overBins = rows.stream().filter(r -> r.get("status").equals(2)).count();
        int maxDays = rows.stream().mapToInt(r -> (int) r.get("agingDays")).max().orElse(0);
        Map<String, Object> kpi = new LinkedHashMap<>(8);
        kpi.put("pieces", pieces);
        kpi.put("bins", rows.size());
        kpi.put("overPieces", overPieces);
        kpi.put("overBins", overBins);
        kpi.put("maxDays", maxDays);
        result.put("kpi", kpi);

        // 五档分布（内存分桶 + 补零，保证前端拿到固定 5 桶；件数=qty 求和）
        Map<String, long[]> buckets = new LinkedHashMap<>(8);
        buckets.put("lt30", new long[2]);
        buckets.put("d30_60", new long[2]);
        buckets.put("d60_90", new long[2]);
        buckets.put("d90_180", new long[2]);
        buckets.put("gt180", new long[2]);
        for (Map<String, Object> r : rows) {
            int d = (int) r.get("agingDays");
            String b = d < 30 ? "lt30" : d < 60 ? "d30_60" : d < 90 ? "d60_90" : d <= 180 ? "d90_180" : "gt180";
            buckets.get(b)[0] += toLong(r.get("qty"));
            buckets.get(b)[1] += 1;
        }
        List<Map<String, Object>> dist = new ArrayList<>(5);
        for (Map.Entry<String, long[]> e : buckets.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>(4);
            item.put("bucket", e.getKey());
            item.put("pieces", e.getValue()[0]);
            item.put("bins", e.getValue()[1]);
            dist.add(item);
        }
        result.put("distribution", dist);
        result.put("byCategory", agingMapper.byCategory());
        return result;
    }

    @Override
    public List<XuehuaDisposal> disposalList(String binCode, boolean pendingOnly) {
        QueryWrapper<XuehuaDisposal> qw = new QueryWrapper<XuehuaDisposal>()
                .eq("del_flag", 0).orderByDesc("id");
        if (StrUtil.isNotBlank(binCode)) {
            qw.eq("bin_code", binCode);
        }
        if (pendingOnly) {
            qw.eq("status", 0);
        }
        return this.list(qw);
    }

    /**
     * 发起处置申请：三字段校验 + 同库位防重复（待审批中再申请直接拒绝）。
     *
     * <p>防重查询与插入虽在事务内，但 PG 默认 READ COMMITTED 下两个并发事务都可能查重通过，
     * 极小概率双插——量级上处置申请是人工低频操作，且列表按待审批展示重复可见，
     * 一期不加唯一索引；二期若开放 EMIS 自动发起再上 (bin_code, status=0) 部分唯一索引兜底。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyDisposal(XuehuaDisposal body) {
        if (StrUtil.isBlank(body.getBinCode())) {
            throw new IllegalStateException("库位不能为空");
        }
        if (StrUtil.isBlank(body.getMethod())) {
            throw new IllegalStateException("处置方式不能为空");
        }
        if (StrUtil.isBlank(body.getReason())) {
            throw new IllegalStateException("申请理由不能为空");
        }
        Long pending = baseMapper.selectCount(new QueryWrapper<XuehuaDisposal>()
                .eq("bin_code", body.getBinCode()).eq("status", 0).eq("del_flag", 0));
        if (pending != null && pending > 0) {
            throw new IllegalStateException("该库位已有待审批的处置申请，勿重复发起");
        }
        body.setStatus(0)
                .setApplicant(StrUtil.blankToDefault(body.getApplicant(), "管理员"))
                .setApplyTime(new Date())
                .setCreateTime(new Date());
        baseMapper.insert(body);
    }

    /**
     * 处置审批（通过 1 / 驳回 2）。
     *
     * <p>并发口径与 PTL 按键确认/出库行确认一致：先 selectById 给出友好提示（"已审批过"），
     * 再用真正的条件更新 UPDATE ... WHERE id=? AND status=0 做状态流转——两个主管同时点通过，
     * 只有一个影响行数=1，另一个落到 updated==0 分支提示重试，天然防双审。
     * 二期"通过后联动库存"加在本方法事务尾部（已有 @Transactional，直接追加即可）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveDisposal(Integer id, boolean pass, String note) {
        XuehuaDisposal d = baseMapper.selectById(id);
        if (d == null) {
            throw new IllegalStateException("处置申请不存在");
        }
        if (d.getStatus() == null || d.getStatus() != 0) {
            throw new IllegalStateException("该申请已审批过（已通过/已驳回）");
        }
        Date now = new Date();
        int updated = baseMapper.update(null, new UpdateWrapper<XuehuaDisposal>()
                .eq("id", id).eq("status", 0)
                .set("status", pass ? 1 : 2)
                .set("approver", "库房主管")
                .set("approve_time", now)
                .set("approve_note", note)
                .set("update_time", now));
        if (updated == 0) {
            // 条件更新影响 0 行 = 并发下已被另一审批人流转，如实提示
            throw new IllegalStateException("该申请已审批过（已通过/已驳回）");
        }
    }

    /** 行装饰：计算 agingDays/status（0正常 1偏高 2超期），无上架记录的行库龄置 0 */
    private List<Map<String, Object>> decorate(List<Map<String, Object>> rows) {
        Date now = new Date();
        for (Map<String, Object> r : rows) {
            Object first = r.get("firstPutaway");
            int days = 0;
            if (first instanceof Date d) {
                days = (int) Math.floorDiv(now.getTime() - d.getTime(), 86400000L);
            }
            r.put("agingDays", days);
            r.put("status", days > OVER_DAYS ? 2 : days > WARN_DAYS ? 1 : 0);
        }
        return rows;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static long toLong(Object o) {
        return o == null ? 0L : Long.parseLong(String.valueOf(o));
    }
}
