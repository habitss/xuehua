package com.keytech.xuehua.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.keytech.base.service.impl.BaseServiceImpl;
import com.keytech.xuehua.entity.XuehuaBin;
import com.keytech.xuehua.entity.XuehuaInventory;
import com.keytech.xuehua.entity.XuehuaInventoryDetail;
import com.keytech.xuehua.mapper.XuehuaBinMapper;
import com.keytech.xuehua.mapper.XuehuaInventoryDetailMapper;
import com.keytech.xuehua.mapper.XuehuaInventoryMapper;
import com.keytech.xuehua.service.IXuehuaInventoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 盘点单 服务实现类
 */
@Service
public class XuehuaInventoryServiceImpl extends BaseServiceImpl<XuehuaInventoryMapper, XuehuaInventory> implements IXuehuaInventoryService {

    /** 盘点阶段总数：0待开始 1期初账面 2入库核实 3出库核对 4期末账面 5差异处理 6完成 */
    private static final int MAX_STAGE = 6;

    @Resource
    private XuehuaInventoryDetailMapper inventoryDetailMapper;

    @Resource
    private XuehuaBinMapper xuehuaBinMapper;

    @Resource
    private com.keytech.xuehua.service.IXuehuaMonitorService xuehuaMonitorService;

    @Override
    public Map<String, Object> stat() {
        Map<String, Object> result = new HashMap<>(8);
        List<Map<String, Object>> rows = this.listMaps(
                new QueryWrapper<XuehuaInventory>().select("status", "count(*) as cnt").groupBy("status"));
        long total = 0L;
        for (int i = 0; i <= 4; i++) {
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
    public boolean addInventory(XuehuaInventory entity) {
        // 事务级咨询锁串行化盘点单号生成（并发新建时防撞号，事务结束自动释放）
        this.baseMapper.advisoryLock();
        if (StrUtil.isBlank(entity.getPvNo())) {
            entity.setPvNo(generateNo("PV"));
        }
        if (StrUtil.isBlank(entity.getSource())) {
            entity.setSource("EMIS下发");
        }
        if (StrUtil.isBlank(entity.getIdentifyType())) {
            entity.setIdentifyType("人脸+IC卡");
        }
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        if (entity.getStage() == null) {
            entity.setStage(0);
        }
        if (entity.getProgress() == null) {
            entity.setProgress(0);
        }
        if (entity.getFrozenFlag() == null) {
            entity.setFrozenFlag(0);
        }
        if (entity.getBookQty() == null) {
            entity.setBookQty(0);
        }
        entity.setCreateTime(new Date());
        return this.save(entity);
    }

    @Override
    public boolean isFrozen() {
        return this.count(new QueryWrapper<XuehuaInventory>()
                .eq("frozen_flag", 1)
                .in("status", 1, 2)) > 0;
    }

    @Override
    public boolean start(Integer id) {
        XuehuaInventory inventory = require(id);
        if (inventory.getStatus() == null || inventory.getStatus() != 0) {
            throw new IllegalStateException("仅【待盘点】任务可开始");
        }
        inventory.setStatus(1);
        inventory.setStage(0);
        inventory.setProgress(0);
        inventory.setUpdateTime(new Date());
        return this.updateById(inventory);
    }

    @Override
    public boolean advance(Integer id) {
        XuehuaInventory inventory = require(id);
        if (inventory.getStatus() == null || (inventory.getStatus() != 1 && inventory.getStatus() != 2)) {
            throw new IllegalStateException("仅【盘点中/有差异】任务可推进");
        }
        int stage = inventory.getStage() == null ? 0 : inventory.getStage();
        if (stage >= MAX_STAGE) {
            throw new IllegalStateException("盘点已到末阶段，请执行差异处置或终止");
        }
        int next = stage + 1;
        inventory.setStage(next);
        inventory.setProgress(next * 100 / MAX_STAGE);
        // 期末账面阶段起，实盘数与账面数不一致即标记有差异
        if (next >= 4 && inventory.getRealQty() != null
                && inventory.getBookQty() != null
                && !inventory.getRealQty().equals(inventory.getBookQty())) {
            inventory.setStatus(2);
        } else if (inventory.getStatus() != null && inventory.getStatus() == 2) {
            inventory.setStatus(1);
        }
        if (next == MAX_STAGE) {
            inventory.setStatus(3);
            inventory.setFrozenFlag(0);
        }
        inventory.setUpdateTime(new Date());
        return this.updateById(inventory);
    }

    @Override
    public boolean freeze(Integer id) {
        XuehuaInventory inventory = require(id);
        if (inventory.getStatus() == null || (inventory.getStatus() != 1 && inventory.getStatus() != 2)) {
            throw new IllegalStateException("仅【盘点中/有差异】任务可冻结库存");
        }
        inventory.setFrozenFlag(1);
        inventory.setUpdateTime(new Date());
        return this.updateById(inventory);
    }

    @Override
    public boolean dispose(Integer id, Integer realQty) {
        XuehuaInventory inventory = require(id);
        if (inventory.getStatus() == null || (inventory.getStatus() != 1 && inventory.getStatus() != 2)) {
            throw new IllegalStateException("仅【盘点中/有差异】任务可执行差异处置");
        }
        if (realQty != null) {
            inventory.setRealQty(realQty);
        }
        inventory.setStatus(3);
        inventory.setStage(MAX_STAGE);
        inventory.setProgress(100);
        inventory.setFrozenFlag(0);
        inventory.setUpdateTime(new Date());
        return this.updateById(inventory);
    }

    @Override
    public boolean stop(Integer id) {
        XuehuaInventory inventory = require(id);
        if (inventory.getStatus() == null || inventory.getStatus() >= 3) {
            throw new IllegalStateException("任务已完成或已终止");
        }
        inventory.setStatus(4);
        inventory.setFrozenFlag(0);
        inventory.setUpdateTime(new Date());
        return this.updateById(inventory);
    }

    private XuehuaInventory require(Integer id) {
        XuehuaInventory inventory = this.getById(id);
        if (inventory == null) {
            throw new IllegalStateException("盘点任务不存在");
        }
        return inventory;
    }

    /**
     * 生成库位级盘点明细：对每个"在库"库位快照一张盘点行（账面数=当时 bin.qty）
     *
     * <p>范围口径：盘点单 zone_code 非空时只盘该区域，否则全库在库库位。
     * 重复调用会清掉本单未应用的旧明细重新生成（盘错重来用）；
     * 已经应用过台账的明细绝不清——那是已经生效的历史。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<XuehuaInventoryDetail> generateDetails(Integer id) {
        XuehuaInventory inventory = require(id);
        if (inventory.getStatus() == null || inventory.getStatus() != 1) {
            throw new IllegalStateException("仅【盘点中】任务可生成盘点明细");
        }
        // 清旧未应用明细（重新生成场景）；已应用的不动
        inventoryDetailMapper.delete(new QueryWrapper<XuehuaInventoryDetail>()
                .eq("inventory_id", id).eq("applied", 0));
        QueryWrapper<XuehuaBin> binQw = new QueryWrapper<XuehuaBin>()
                .eq("del_flag", 0).eq("status", 1).orderByAsc("bin_code");
        if (StrUtil.isNotBlank(inventory.getZoneCode())) {
            binQw.eq("zone_code", inventory.getZoneCode());
        }
        List<XuehuaBin> bins = xuehuaBinMapper.selectList(binQw);
        Date now = new Date();
        for (XuehuaBin bin : bins) {
            XuehuaInventoryDetail detail = new XuehuaInventoryDetail()
                    .setInventoryId(id)
                    .setPvNo(inventory.getPvNo())
                    .setBinCode(bin.getBinCode())
                    .setPartCode(bin.getPartCode())
                    .setBookQty(bin.getQty() == null ? 0 : bin.getQty())
                    .setApplied(0)
                    .setCreateTime(now);
            inventoryDetailMapper.insert(detail);
        }
        // 单据上的账面数 = 各库位快照之和（单据级汇总仅作展示口径）
        int bookSum = bins.stream().mapToInt(b -> b.getQty() == null ? 0 : b.getQty()).sum();
        inventory.setBookQty(bookSum);
        inventory.setUpdateTime(now);
        this.updateById(inventory);
        return inventoryDetailMapper.selectList(new QueryWrapper<XuehuaInventoryDetail>()
                .eq("inventory_id", id).eq("del_flag", 0).orderByAsc("bin_code"));
    }

    /**
     * 录入某个库位的实盘数：算差异，不动台账（台账统一在差异处置时应用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitReal(Integer detailId, Integer realQty) {
        if (realQty == null || realQty < 0) {
            throw new IllegalStateException("实盘数必须为非负整数");
        }
        XuehuaInventoryDetail detail = inventoryDetailMapper.selectById(detailId);
        if (detail == null) {
            throw new IllegalStateException("盘点明细不存在");
        }
        if (detail.getApplied() != null && detail.getApplied() == 1) {
            throw new IllegalStateException("该明细差异已处置，不可重复录入");
        }
        detail.setRealQty(realQty);
        detail.setDiffQty(realQty - (detail.getBookQty() == null ? 0 : detail.getBookQty()));
        detail.setUpdateTime(new Date());
        return inventoryDetailMapper.updateById(detail) > 0;
    }

    /**
     * 差异处置：把每行差异原子的加到 bin.qty，盘点单转【已完成】并自动解冻
     *
     * <p>为什么用 qty = qty + diff 而不是直接把实盘数覆盖进台账：
     * 生成明细到差异处置之间库位可能发生了正常出入库（ Movements），直接覆盖会把
     * 这段合法变动冲掉；"快照+差额"只修正盘点发现的盈亏，两不干扰。
     * 原子算术 + 行锁与出库扣减同一套防丢失更新套路；
     * WHERE 保证台账扣不出负数（实盘比账面少且货已被正常出库时，差额自然收敛为不亏）。
     * 全部明细应用成功才算完成——任一行失败整个事务回滚，盘点单留在差异状态等人工处理。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyDifferences(Integer id) {
        XuehuaInventory inventory = require(id);
        if (inventory.getStatus() == null || (inventory.getStatus() != 1 && inventory.getStatus() != 2)) {
            throw new IllegalStateException("仅【盘点中/有差异】任务可执行差异处置");
        }
        List<XuehuaInventoryDetail> details = inventoryDetailMapper.selectList(
                new QueryWrapper<XuehuaInventoryDetail>()
                        .eq("inventory_id", id).eq("del_flag", 0).eq("applied", 0));
        int appliedCount = 0;
        int realSum = 0;
        for (XuehuaInventoryDetail detail : details) {
            if (detail.getRealQty() == null) {
                throw new IllegalStateException("库位 " + detail.getBinCode() + " 尚未录入实盘数，无法处置");
            }
            int diff = detail.getDiffQty() == null ? 0 : detail.getDiffQty();
            int updated = xuehuaBinMapper.update(null, new UpdateWrapper<XuehuaBin>()
                    .eq("bin_code", detail.getBinCode())
                    // 台账扣不出负数：差额比现有台账还大时，说明期间的出库已把账抹平，收敛即可
                    .setSql("qty = CASE WHEN qty + " + diff + " < 0 THEN 0 ELSE qty + " + diff + " END")
                    .setSql("status = CASE WHEN qty + " + diff + " <= 0 THEN 0 ELSE 1 END")
                    .set("update_time", new Date()));
            if (updated == 0) {
                throw new IllegalStateException("库位 " + detail.getBinCode() + " 台账调整失败，请重试");
            }
            detail.setApplied(1);
            detail.setUpdateTime(new Date());
            inventoryDetailMapper.updateById(detail);
            appliedCount++;
            realSum += detail.getRealQty();
        }
        inventory.setRealQty(realSum);
        inventory.setStatus(3);
        inventory.setStage(MAX_STAGE);
        inventory.setProgress(100);
        inventory.setFrozenFlag(0);
        inventory.setUpdateTime(new Date());
        boolean ok = this.updateById(inventory);
        if (ok) {
            // 【盘点完成判定（EMIS 回传）】差异处置完成即回传 EMIS（说明书 5.1），旁路失败不阻断
            xuehuaMonitorService.notifyAfterCommit(() -> xuehuaMonitorService.notifyInventoryDone(inventory.getPvNo()));
        }
        return ok;
    }

    @Override
    public List<XuehuaInventoryDetail> listDetails(Integer id) {
        return inventoryDetailMapper.selectList(new QueryWrapper<XuehuaInventoryDetail>()
                .eq("inventory_id", id).eq("del_flag", 0).orderByAsc("bin_code"));
    }

    /**
     * EMIS 盘点任务下发接收（原型 pvData source=EMIS下发 / 说明书 5.1）：
     * external_no 可空唯一索引幂等兜底；盘点人注册校验在 Controller 层完成（保持
     * 与入库/出库 receive 同构：人员校验+海康授权在落库事务之前）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public XuehuaInventory receiveFromEmis(XuehuaInventory body, String executor) {
        body.setSource("EMIS下发");
        body.setExecutor(executor);
        this.addInventory(body);
        return body;
    }

    /**
     * 生成盘点单号：PV-4位序号（冲突时继续+1直到不重复）
     */
    private String generateNo(String prefix) {
        long seq = this.count() + 1;
        String no;
        do {
            no = prefix + "-" + String.format("%04d", seq++);
        } while (this.baseMapper.countNoAll(no) > 0);
        return no;
    }
}
