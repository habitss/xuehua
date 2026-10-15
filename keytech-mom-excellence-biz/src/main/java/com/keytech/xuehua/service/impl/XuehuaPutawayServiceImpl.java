package com.keytech.xuehua.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.keytech.base.service.impl.BaseServiceImpl;
import com.keytech.xuehua.entity.XuehuaBin;
import com.keytech.xuehua.entity.XuehuaPutaway;
import com.keytech.xuehua.mapper.XuehuaBinMapper;
import com.keytech.xuehua.mapper.XuehuaPutawayMapper;
import com.keytech.xuehua.ptl.PtlClient;
import com.keytech.xuehua.service.IXuehuaPutawayService;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 上架任务 服务实现类
 */
@Service
public class XuehuaPutawayServiceImpl extends BaseServiceImpl<XuehuaPutawayMapper, XuehuaPutaway> implements IXuehuaPutawayService {

    @Resource
    private XuehuaBinMapper xuehuaBinMapper;

    @Resource
    private PtlClient ptlClient;

    @Resource
    private com.keytech.xuehua.service.IXuehuaMonitorService xuehuaMonitorService;

    @Override
    public Map<String, Object> stat() {
        Map<String, Object> result = new HashMap<>(8);
        List<Map<String, Object>> rows = this.listMaps(
                new QueryWrapper<XuehuaPutaway>().select("status", "count(*) as cnt").groupBy("status"));
        long total = 0L;
        for (int i = 0; i <= 1; i++) {
            result.put("s" + i, 0L);
        }
        for (Map<String, Object> row : rows) {
            Object status = row.get("status");
            Object cnt = row.get("cnt");
            long count = cnt == null ? 0L : Long.parseLong(cnt.toString());
            result.put("s" + status, count);
            total += count;
        }
        result.put("total", total);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmPutaway(Integer id) {
        // 【勿删此注解】页面路径 Controller 调的是本单参重载；内部 this.confirmPutaway(id,null)
        // 自调用不走代理，两参方法的 @Transactional 对它无效——事务必须在本方法上声明。
        // 真实教训（2026-09-13 压测暴露）：缺注解时混装拦截抛异常无法回滚已提交的
        // 任务置1更新，造成"任务已上架但库存没加"的半截状态
        return confirmPutaway(id, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmPutawayByInboundNo(String inboundNo, String operator) {
        XuehuaPutaway task = this.getOne(
                new QueryWrapper<XuehuaPutaway>().eq("inbound_no", inboundNo).eq("status", 0).last("limit 1"));
        if (task == null) {
            return false;
        }
        return confirmPutaway(task.getId(), operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmPutawayByTagAdd(Integer tagAdd, String operator) {
        if (tagAdd == null) {
            return false;
        }
        XuehuaBin bin = xuehuaBinMapper.selectOne(
                new QueryWrapper<XuehuaBin>().eq("tag_add", tagAdd).last("limit 1"));
        if (bin == null) {
            return false;
        }
        XuehuaPutaway task = this.getOne(
                new QueryWrapper<XuehuaPutaway>().eq("bin_code", bin.getBinCode()).eq("status", 0).last("limit 1"));
        if (task == null) {
            return false;
        }
        return confirmPutaway(task.getId(), operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmPutaway(Integer id, String operator) {
        XuehuaPutaway task = this.getById(id);
        if (task == null) {
            throw new IllegalStateException("上架任务不存在");
        }
        if (task.getStatus() == null || task.getStatus() != 0) {
            throw new IllegalStateException("该任务已上架，不可重复确认");
        }
        // 条件更新（WHERE status=0）：并发重复确认时只有一个事务能改到，其余影响行数=0
        UpdateWrapper<XuehuaPutaway> uw = new UpdateWrapper<XuehuaPutaway>()
                .eq("id", task.getId())
                .eq("status", 0)
                .set("status", 1)
                .set("putaway_time", new Date())
                .set("update_time", new Date());
        if (StrUtil.isNotBlank(operator)) {
            uw.set("putaway_by", operator);
        }
        boolean ok = this.update(uw);
        if (!ok) {
            throw new IllegalStateException("该任务已上架，不可重复确认");
        }
        if (ok && StrUtil.isNotBlank(task.getBinCode())) {
            XuehuaBin bin = xuehuaBinMapper.selectOne(
                    new QueryWrapper<XuehuaBin>().eq("bin_code", task.getBinCode()).last("limit 1"));
            if (bin != null) {
                // 【混装拦截】口径：一个库位只存一种备件（PTL 标签按库位点亮、屏显单一物料）。
                // 库位上已有其他备件时拒绝上架并抛异常——整个事务回滚，上架任务退回"待确认"，
                // 现场换空库位或先把原备件出清后再来。库位无货(part_code 空)视为首放不拦截
                if (StrUtil.isNotBlank(bin.getPartCode())
                        && StrUtil.isNotBlank(task.getPartCode())
                        && !bin.getPartCode().equals(task.getPartCode())) {
                    throw new IllegalStateException("库位 " + bin.getBinCode()
                            + " 已存放备件 " + bin.getPartCode() + "，不可混装 " + task.getPartCode()
                            + "，请换空库位");
                }
                // 【库存联动-加】上架确认 = 实物落到库位，库存原子累加 + 备件归属落档。
                // 必须用 SET qty = qty + n 的原子 SQL，不能用"读出 qty→内存加一→updateById 写回"：
                // 两个并发确认读到同一个旧值会互相覆盖（丢失更新），台账悄悄少记。
                // 原子算术在数据库行锁内串行执行，qty 永远不会因并发而算错。
                // qty 状态与数量必须同一条 UPDATE（分开写并发下会错位），qty>0 即在库
                int putQty = task.getQty() == null ? 0 : task.getQty();
                xuehuaBinMapper.update(null, new UpdateWrapper<XuehuaBin>()
                        .eq("bin_code", bin.getBinCode())
                        .setSql("qty = qty + " + putQty)
                        .set(StrUtil.isNotBlank(task.getPartCode()), "part_code", task.getPartCode())
                        .set("status", 1)
                        .set("update_time", new Date()));
                // 上架完成熄灭该库位 PTL 电子标签
                ptlClient.lightOff(PtlClient.TASK_INBOUND, task.getInboundNo(), bin.getTagAdd());
            }
            // 【入库完成判定（监控管理/EMIS 回传）】该单上架任务全部完成 → 通知回传 EMIS
            long pendingTasks = this.count(new QueryWrapper<XuehuaPutaway>()
                    .eq("inbound_no", task.getInboundNo()).eq("status", 0));
            if (pendingTasks == 0) {
                xuehuaMonitorService.notifyAfterCommit(() -> xuehuaMonitorService.notifyInboundDone(task.getInboundNo()));
            }
        }
        return ok;
    }
}
