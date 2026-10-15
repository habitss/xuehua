package com.keytech.xuehua.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.keytech.base.service.impl.BaseServiceImpl;
import com.keytech.xuehua.entity.XuehuaBin;
import com.keytech.xuehua.entity.XuehuaInbound;
import com.keytech.xuehua.entity.XuehuaPart;
import com.keytech.xuehua.entity.XuehuaPerson;
import com.keytech.xuehua.entity.XuehuaPutaway;
import com.keytech.xuehua.mapper.XuehuaBinMapper;
import com.keytech.xuehua.mapper.XuehuaInboundMapper;
import com.keytech.xuehua.mapper.XuehuaPartMapper;
import com.keytech.xuehua.mapper.XuehuaPutawayMapper;
import com.keytech.xuehua.ptl.PtlClient;
import com.keytech.xuehua.service.IXuehuaInboundService;
import com.keytech.xuehua.service.IXuehuaInventoryService;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description:  入库单 服务实现类（登记域：0待登记 1已登记）
 */
@Service
public class XuehuaInboundServiceImpl extends BaseServiceImpl<XuehuaInboundMapper, XuehuaInbound> implements IXuehuaInboundService {

    @Resource
    private XuehuaBinMapper xuehuaBinMapper;

    @Resource
    private XuehuaPutawayMapper xuehuaPutawayMapper;

    @Resource
    private IXuehuaInventoryService xuehuaInventoryService;

    @Resource
    private XuehuaPartMapper xuehuaPartMapper;

    @Resource
    private PtlClient ptlClient;

    @Resource
    private com.keytech.xuehua.service.IXuehuaPersonService xuehuaPersonService;

    @Override
    public Map<String, Object> stat() {
        Map<String, Object> result = new HashMap<>(8);
        List<Map<String, Object>> rows = this.listMaps(
                new QueryWrapper<XuehuaInbound>().select("status", "count(*) as cnt").groupBy("status"));
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
    public boolean addInbound(XuehuaInbound entity) {
        if (xuehuaInventoryService.isFrozen()) {
            throw new IllegalStateException("库存盘点冻结中，禁止入库登记");
        }
        // 登记三要素硬校验：送货单、全景照片、检验报告
        if (StrUtil.isBlank(entity.getDeliveryNo())) {
            throw new IllegalStateException("入库登记必须上传送货单");
        }
        if (StrUtil.isBlank(entity.getPhoto())) {
            throw new IllegalStateException("入库登记必须上传全景照片");
        }
        if (StrUtil.isBlank(entity.getReport())) {
            throw new IllegalStateException("入库登记必须上传检验报告");
        }
        // 事务级咨询锁串行化单号生成（并发登记防撞号：拿到锁后 do-while 查重必能看到已提交的最大号）
        this.baseMapper.advisoryLock();
        if (StrUtil.isBlank(entity.getInboundNo())) {
            entity.setInboundNo(generateNo("IN"));
        }
        entity.setStatus(1);
        entity.setCreateTime(new Date());
        boolean ok = this.save(entity);
        if (ok) {
            allocateAndPutaway(entity);
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public XuehuaInbound receiveArrival(XuehuaInbound entity, String operatorName) {
        // EMIS 到货单同步：操作人必须是已注册人员（监控管理口径——未注册不能进仓作业）
        XuehuaPerson person = xuehuaPersonService.resolveByOperator(operatorName);
        if (person == null) {
            throw new IllegalStateException("操作人 " + operatorName + " 未在人员注册中登记，请先注册并推送门禁授权");
        }
        if (xuehuaInventoryService.isFrozen()) {
            throw new IllegalStateException("库存盘点冻结中，暂停到货单接收");
        }
        if (StrUtil.isBlank(entity.getPartCode())) {
            throw new IllegalStateException("partCode(备件编码)不能为空");
        }
        if (entity.getQty() == null || entity.getQty() <= 0) {
            throw new IllegalStateException("qty 必须大于 0");
        }
        this.baseMapper.advisoryLock();
        if (StrUtil.isBlank(entity.getInboundNo())) {
            entity.setInboundNo(generateNo("IN"));
        }
        // 到货单 = 待登记：只落单据与操作人（库位分配/亮灯等登记动作在补齐三要素后 /register 触发）
        entity.setOperator(person.getEmpNo());
        entity.setStatus(0);
        entity.setCreateTime(new Date());
        this.save(entity);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean register(Integer id, XuehuaInbound patch) {
        XuehuaInbound entity = this.getById(id);
        if (entity == null) {
            throw new IllegalStateException("到货单不存在");
        }
        if (entity.getStatus() != null && entity.getStatus() != 0) {
            throw new IllegalStateException("该到货单已登记，不可重复登记");
        }
        if (xuehuaInventoryService.isFrozen()) {
            throw new IllegalStateException("库存盘点冻结中，禁止入库登记");
        }
        // 现场补齐三要素（EMIS 到货单没有照片/报告，送货单可能已随单带入，patch 非空覆盖）
        if (StrUtil.isNotBlank(patch.getDeliveryNo())) {
            entity.setDeliveryNo(patch.getDeliveryNo());
        }
        if (StrUtil.isNotBlank(patch.getPhoto())) {
            entity.setPhoto(patch.getPhoto());
        }
        if (StrUtil.isNotBlank(patch.getReport())) {
            entity.setReport(patch.getReport());
        }
        if (StrUtil.isBlank(entity.getDeliveryNo())) {
            throw new IllegalStateException("入库登记必须上传送货单");
        }
        if (StrUtil.isBlank(entity.getPhoto())) {
            throw new IllegalStateException("入库登记必须上传全景照片");
        }
        if (StrUtil.isBlank(entity.getReport())) {
            throw new IllegalStateException("入库登记必须上传检验报告");
        }
        // 【顺序关键】先分配（把库位写进内存实体）再 updateById——
        // 反过来的话分配到的 bin_code 不会落库（2026-09-18 自测真实踩坑：入库单 bin_code 为空）
        allocateAndPutaway(entity);
        entity.setStatus(1);
        entity.setUpdateTime(new Date());
        return this.updateById(entity);
    }

    /**
     * 登记公共尾段：自动分配库位（排除待上架占用 + SFOU）→ 生成上架任务 →
     * 注册 afterCommit 亮灯。addInbound（页面登记）与 register（到货单补登记）共用。
     * 调用方负责事务与状态置位。
     */
    private void allocateAndPutaway(XuehuaInbound entity) {
        if (StrUtil.isBlank(entity.getBinCode())) {
            // 排除已被待上架任务占用的空库位（已提交的占位）；FOR UPDATE SKIP LOCKED 挡住
            // 并发重叠事务（别人锁定的库位直接跳过），二者配合保证不重复分配
            List<Object> pendingBins = xuehuaPutawayMapper.selectObjs(
                    new QueryWrapper<XuehuaPutaway>().select("distinct bin_code").eq("status", 0).isNotNull("bin_code"));
            QueryWrapper<XuehuaBin> binWrapper = new QueryWrapper<XuehuaBin>().eq("status", 0);
            if (!pendingBins.isEmpty()) {
                binWrapper.notIn("bin_code", pendingBins);
            }
            XuehuaBin emptyBin = xuehuaBinMapper.selectOne(
                    binWrapper.orderByAsc("bin_code").last("limit 1 for update skip locked"));
            if (emptyBin != null) {
                entity.setBinCode(emptyBin.getBinCode());
            }
        }
        // 自动生成上架任务（待上架）
        XuehuaPutaway task = new XuehuaPutaway()
                .setInboundId(entity.getId())
                .setInboundNo(entity.getInboundNo())
                .setPartCode(entity.getPartCode())
                .setPartName(entity.getPartName())
                .setModel(entity.getModel())
                .setQty(entity.getQty())
                .setUnit(entity.getUnit())
                .setBinCode(entity.getBinCode())
                .setStatus(0)
                .setRegisterBy(StrUtil.blankToDefault(entity.getOperator(), entity.getCreateBy()))
                .setCreateBy(entity.getCreateBy())
                .setCreateTime(new Date());
        try {
            xuehuaPutawayMapper.insert(task);
        } catch (DuplicateKeyException e) {
            // uk_xuehua_putaway_pending_bin 兜底命中（理论极小概率），事务回滚并提示重试
            throw new IllegalStateException("库位分配冲突，请重试");
        }
        // 登记成功即点亮目标库位 PTL 电子标签（红色闪烁）。
        // 挪到事务提交后触发：分配查询持有库位行锁，锁内不做网络调用，避免拖长其他登记的锁等待
        if (StrUtil.isNotBlank(entity.getBinCode())) {
            XuehuaBin bin = xuehuaBinMapper.selectOne(
                    new QueryWrapper<XuehuaBin>().eq("bin_code", entity.getBinCode()).last("limit 1"));
            if (bin != null) {
                final String orderNo = entity.getInboundNo();
                final Integer tagAdd = bin.getTagAdd();
                final Integer qty = entity.getQty();
                final String partCode = entity.getPartCode();
                final String partName = entity.getPartName();
                // 标签屏"库存"控件显示亮灯时刻的现存量（不含本单——本单数量由数码管6000单独显示，
                // 两者语义不同：库存=当前实物，任务=这单要放多少。2026-09-11 用户纠正：不要预填本单量）
                final Integer stockNow = bin.getQty() == null ? 0 : bin.getQty();
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        ptlClient.lightOn(PtlClient.TASK_INBOUND, orderNo, tagAdd, qty,
                                PtlClient.COLOR_RED, partCode, partName, stockNow);
                    }
                });
            }
        }
    }

    @Override
    public XuehuaInbound findByInboundNo(String inboundNo) {
        if (StrUtil.isBlank(inboundNo)) {
            return null;
        }
        return this.getOne(new QueryWrapper<XuehuaInbound>()
                .eq("inbound_no", inboundNo).orderByDesc("id").last("limit 1"));
    }

    @Override
    public Map<String, Object> scanFill(String code) {
        if (StrUtil.isBlank(code)) {
            throw new IllegalStateException("扫码内容为空");
        }
        // 优先按备件条码匹配启用状态的主数据，供登记表单回填
        XuehuaPart part = xuehuaPartMapper.selectOne(
                new QueryWrapper<XuehuaPart>().eq("part_code", code).eq("status", 1).last("limit 1"));
        if (part != null) {
            Map<String, Object> result = new HashMap<>(8);
            result.put("type", "part");
            result.put("partCode", part.getPartCode());
            result.put("partName", part.getPartName());
            result.put("model", part.getModel());
            result.put("category", part.getCategory());
            result.put("unit", part.getUnit());
            result.put("defaultZone", part.getDefaultZone());
            return result;
        }
        // 再按送货单号查重：已登记过的送货单提示原入库单号
        List<XuehuaInbound> exists = this.list(
                new QueryWrapper<XuehuaInbound>().eq("delivery_no", code).orderByDesc("create_time"));
        if (!exists.isEmpty()) {
            XuehuaInbound first = exists.get(0);
            Map<String, Object> result = new HashMap<>(8);
            result.put("type", "delivery");
            result.put("duplicated", true);
            result.put("inboundNo", first.getInboundNo());
            result.put("status", first.getStatus());
            return result;
        }
        throw new IllegalStateException("未识别的条码：" + code);
    }

    /**
     * 生成业务单号：前缀-年份-4位序号（冲突时继续+1直到不重复）
     */
    private String generateNo(String prefix) {
        String year = String.valueOf(new Date().getYear() + 1900);
        long seq = this.count() + 1;
        String no;
        do {
            no = prefix + "-" + year + "-" + String.format("%04d", seq++);
        } while (this.baseMapper.countNoAll(no) > 0);
        return no;
    }
}
