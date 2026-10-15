package com.keytech.xuehua.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.keytech.xuehua.entity.XuehuaAccessLog;
import com.keytech.xuehua.entity.XuehuaInbound;
import com.keytech.xuehua.entity.XuehuaOutbound;
import com.keytech.xuehua.entity.XuehuaPerson;
import com.keytech.xuehua.mapper.XuehuaAccessLogMapper;
import com.keytech.xuehua.mapper.XuehuaInboundMapper;
import com.keytech.xuehua.mapper.XuehuaOutboundMapper;
import com.keytech.xuehua.mapper.XuehuaPersonMapper;
import com.keytech.xuehua.mapper.XuehuaPutawayMapper;
import com.keytech.xuehua.emis.EmisClient;
import com.keytech.xuehua.service.IXuehuaMonitorService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 监控管理 聚合服务实现
 *
 * <p>三类职责：① 时间线统一落库入口（海康事件/PTL确认/模拟注入共用）；
 * ② 海康门禁事件处理（人单关联 + 非授权告警）；③ 入库/出库完成回传 EMIS。
 * 全部旁路设计——recordAccess/notify 失败只记日志，绝不阻断主业务。
 */
@Slf4j
@Service
public class XuehuaMonitorServiceImpl implements IXuehuaMonitorService {

    @Resource
    private XuehuaAccessLogMapper accessLogMapper;
    @Resource
    private XuehuaPersonMapper personMapper;
    @Resource
    private XuehuaInboundMapper inboundMapper;
    @Resource
    private XuehuaOutboundMapper outboundMapper;
    @Resource
    private XuehuaPutawayMapper putawayMapper;
    @Resource
    private EmisClient emisClient;

    /** 回传失败留痕与业务共用审计表（biz_type=EMIS_PUSH），重推有据可查 */
    @Resource
    private com.keytech.xuehua.service.IXuehuaOpLogService opLogService;

    @Resource
    private com.keytech.xuehua.mapper.XuehuaInventoryMapper inventoryMapper;

    @Resource
    private com.keytech.xuehua.mapper.XuehuaInventoryDetailMapper inventoryDetailMapper;

    @Resource
    private com.keytech.xuehua.mapper.XuehuaOpLogMapper opLogMapper;

    /**
     * 事务提交后执行通知：注册 afterCommit（与入库亮灯同款模式）。
     * afterCommit 回调里连接已归还，内部各 notify 方法自带独立 SQL 会话与 try-catch，
     * 失败只留痕不抛——不会干扰已提交的业务事务。
     */
    @Override
    public void notifyAfterCommit(Runnable notifyTask) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            notifyTask.run();
                        }
                    });
        } else {
            // 防御：无事务上下文（如未来被定时任务直接调）时直接执行，不静默丢通知
            notifyTask.run();
        }
    }

    /** 回传请求体序列化（留痕 req_body 供重推原样重发） */
    private static String toJson(Map<String, Object> summary) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(summary);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 失败回传手动重推：从 op_log 取 req_body 原样重发（按 op_action 路由三向回传），
     * 成功回写 success=1——"失败留痕可重推"承诺的闭环入口。
     * EMIS 侧按 externalNo 幂等，重复投递安全。
     */
    @Override
    public String repush(Integer opLogId) {
        com.keytech.xuehua.entity.XuehuaOpLog op = opLogMapper.selectById(opLogId);
        if (op == null) {
            throw new IllegalStateException("留痕记录不存在：id=" + opLogId);
        }
        if (!"EMIS_PUSH".equals(op.getBizType())) {
            throw new IllegalStateException("仅支持重推 EMIS_PUSH 类型留痕");
        }
        if (op.getSuccess() != null && op.getSuccess() == 1) {
            throw new IllegalStateException("该回传已成功，无需重推");
        }
        if (StrUtil.isBlank(op.getReqBody())) {
            throw new IllegalStateException("历史留痕未存请求体，无法重推（请重新触发业务完成点）");
        }
        Map<String, Object> body;
        try {
            body = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(op.getReqBody(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("留痕请求体解析失败：" + e.getMessage());
        }
        boolean ok;
        String action = String.valueOf(op.getOpAction());
        switch (action) {
            case "INBOUND_DONE":
                ok = emisClient.inboundDone(body);
                break;
            case "OUTBOUND_DONE":
                ok = emisClient.outboundDone(body);
                break;
            case "INVENTORY_DONE":
                ok = emisClient.inventoryDone(body);
                break;
            default:
                throw new IllegalStateException("未知回传类型：" + action);
        }
        // 成功才回写留痕状态；失败保持 success=0 允许再次重推
        if (ok) {
            op.setSuccess(1);
            op.setErrorMsg(null);
            op.setRespBody("repush ok " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            op.setUpdateTime(new Date());
            opLogMapper.updateById(op);
            return "重推成功：" + op.getBizNo() + " " + action;
        }
        return "重推仍失败（开关关闭或 EMIS 不可达），留痕保持可重推：" + op.getBizNo();
    }

    /**
     * 盘点完成回传 EMIS：差异处置完成时触发。汇总 = 盘点单头 + 差异明细行
     * （库位/账面/实盘/差额），EMIS 侧对账用；失败记 op_log 可重推。
     */
    @Override
    public void notifyInventoryDone(String pvNo) {
        try {
            com.keytech.xuehua.entity.XuehuaInventory inv = inventoryMapper.selectOne(
                    new QueryWrapper<com.keytech.xuehua.entity.XuehuaInventory>()
                            .eq("pv_no", pvNo).last("limit 1"));
            if (inv == null) {
                return;
            }
            Map<String, Object> summary = new LinkedHashMap<>(16);
            summary.put("externalNo", inv.getExternalNo());
            summary.put("pvNo", inv.getPvNo());
            summary.put("source", inv.getSource());
            summary.put("zoneCode", inv.getZoneCode());
            summary.put("scope", inv.getScope());
            summary.put("executor", inv.getExecutor());
            summary.put("bookQty", inv.getBookQty());
            summary.put("realQty", inv.getRealQty());
            summary.put("status", 3);
            // 差异明细行（EMIS 对账核心：哪个库位差了多少）
            List<Map<String, Object>> lines = new ArrayList<>();
            for (com.keytech.xuehua.entity.XuehuaInventoryDetail d : inventoryDetailMapper.selectList(
                    new QueryWrapper<com.keytech.xuehua.entity.XuehuaInventoryDetail>()
                            .eq("inventory_id", inv.getId()).eq("del_flag", 0).orderByAsc("bin_code"))) {
                Map<String, Object> line = new LinkedHashMap<>(8);
                line.put("binCode", d.getBinCode());
                line.put("partCode", d.getPartCode());
                line.put("bookQty", d.getBookQty());
                line.put("realQty", d.getRealQty());
                line.put("diffQty", d.getDiffQty());
                lines.add(line);
            }
            summary.put("lines", lines);
            boolean ok = emisClient.inventoryDone(summary);
            opLogService.record("EMIS_PUSH", pvNo, "INVENTORY_DONE", "WCS",
                    "127.0.0.1", toJson(summary), "result=" + ok, ok, ok ? null : "EMIS 推送失败（开关关闭或网络异常），可重推");
        } catch (Exception e) {
            log.warn("[EMIS] 盘点回传失败(不阻断): {} err={}", pvNo, e.getMessage());
        }
    }

    @Override
    public void recordAccess(XuehuaPerson person, String fallbackName, int eventType, String zone,
                             String method, String source, String direction, String bizNo,
                             String cameraIndex, Date eventTime) {
        try {
            XuehuaAccessLog log1 = new XuehuaAccessLog()
                    .setEventType(eventType)
                    .setZone(zone)
                    .setMethod(method)
                    .setSource(source)
                    .setDirection(direction)
                    .setBizNo(bizNo)
                    .setCameraIndex(cameraIndex)
                    .setEventTime(eventTime == null ? new Date() : eventTime);
            if (person != null) {
                log1.setPersonId(person.getId())
                        .setPersonName(person.getName())
                        .setEmpNo(person.getEmpNo())
                        .setDept(person.getDept());
            } else {
                log1.setPersonName(StrUtil.blankToDefault(fallbackName, "(未识别)"));
            }
            accessLogMapper.insert(log1);
        } catch (Exception e) {
            log.warn("[监控] 时间线记录失败(不阻断): type={} biz={} err={}", eventType, bizNo, e.getMessage());
        }
    }

    @Override
    public Map<String, Object> handleHikEvent(String direction, String empNo, String cardNo,
                                              String personName, String zone, String cameraIndex, Date eventTime) {
        Map<String, Object> result = new LinkedHashMap<>(4);
        // 人员匹配：工号优先、卡号兜底（海康事件按其推送的标识来）
        XuehuaPerson person = null;
        if (StrUtil.isNotBlank(empNo)) {
            person = personMapper.selectOne(new QueryWrapper<XuehuaPerson>()
                    .eq("emp_no", empNo).eq("status", 1).last("limit 1"));
        }
        if (person == null && StrUtil.isNotBlank(cardNo)) {
            person = personMapper.selectOne(new QueryWrapper<XuehuaPerson>()
                    .eq("card_no", cardNo).eq("status", 1).last("limit 1"));
        }
        boolean isIn = "in".equalsIgnoreCase(direction);
        if (person == null) {
            // 未匹配注册人员：非授权进入（工作台报警数据源）
            recordAccess(null, StrUtil.blankToDefault(personName, cardNo), XuehuaAccessLog.EVT_UNAUTHORIZED,
                    zone, "人脸", "hik", direction, null, cameraIndex, eventTime);
            result.put("code", 200);
            result.put("msg", "事件已记录：非授权进入");
            result.put("person", null);
            return result;
        }
        // 人单关联：该人员当前在途单据（入库=操作人匹配且上架未完；出库=工单未完成）
        String bizNo = findOpenBizForPerson(person.getEmpNo());
        recordAccess(person, null, isIn ? XuehuaAccessLog.EVT_IN : XuehuaAccessLog.EVT_OUT,
                zone, StrUtil.blankToDefault(person.getAuthMethod(), "人脸"), "hik",
                direction, bizNo, cameraIndex, eventTime);
        result.put("code", 200);
        result.put("msg", "事件已记录：" + (isIn ? "进仓" : "出仓"));
        result.put("person", person.getName());
        result.put("bizNo", bizNo);
        return result;
    }

    /** 该人员在途单据：优先未完成的入库上架单，其次未完成的出库工单 */
    private String findOpenBizForPerson(String empNo) {
        List<XuehuaInbound> inbounds = inboundMapper.selectList(new QueryWrapper<XuehuaInbound>()
                .eq("operator", empNo).ne("status", -1).orderByDesc("id").last("limit 3"));
        for (XuehuaInbound inbound : inbounds) {
            long pending = putawayMapper.selectCount(new QueryWrapper<com.keytech.xuehua.entity.XuehuaPutaway>()
                    .eq("inbound_no", inbound.getInboundNo()).eq("status", 0));
            if (pending > 0) {
                return inbound.getInboundNo();
            }
        }
        XuehuaOutbound outbound = outboundMapper.selectOne(new QueryWrapper<XuehuaOutbound>()
                .eq("receive_user", empNo).in("status", 0, 1).orderByDesc("id").last("limit 1"));
        return outbound == null ? null : outbound.getWoNo();
    }

    @Override
    public void notifyInboundDone(String inboundNo) {
        try {
            XuehuaInbound inbound = inboundMapper.selectOne(new QueryWrapper<XuehuaInbound>()
                    .eq("inbound_no", inboundNo).last("limit 1"));
            if (inbound == null) {
                return;
            }
            Map<String, Object> summary = new LinkedHashMap<>(16);
            summary.put("externalNo", inbound.getExternalNo());
            summary.put("inboundNo", inbound.getInboundNo());
            summary.put("deliveryNo", inbound.getDeliveryNo());
            summary.put("partCode", inbound.getPartCode());
            summary.put("partName", inbound.getPartName());
            summary.put("qty", inbound.getQty());
            summary.put("binCode", inbound.getBinCode());
            summary.put("operator", inbound.getOperator());
            summary.put("timeline", timeline(inboundNo));
            boolean ok = emisClient.inboundDone(summary);
            opLogService.record("EMIS_PUSH", inboundNo, "INBOUND_DONE", "WCS",
                    "127.0.0.1", toJson(summary), "result=" + ok, ok, ok ? null : "EMIS 推送失败（开关关闭或网络异常），可重推");
        } catch (Exception e) {
            log.warn("[EMIS] 入库回传失败(不阻断): {} err={}", inboundNo, e.getMessage());
        }
    }

    @Override
    public void notifyOutboundDone(String woNo) {
        try {
            XuehuaOutbound outbound = outboundMapper.selectOne(new QueryWrapper<XuehuaOutbound>()
                    .eq("wo_no", woNo).last("limit 1"));
            if (outbound == null) {
                return;
            }
            Map<String, Object> summary = new LinkedHashMap<>(16);
            summary.put("externalNo", outbound.getExternalNo());
            summary.put("woNo", outbound.getWoNo());
            summary.put("partCode", outbound.getPartCode());
            summary.put("partName", outbound.getPartName());
            summary.put("qty", outbound.getQty());
            summary.put("receiveUser", outbound.getReceiveUser());
            summary.put("workshop", outbound.getWorkshop());
            summary.put("lines", outboundMapper.selectList(new QueryWrapper<XuehuaOutbound>()
                    .eq("wo_no", woNo)).size());
            summary.put("timeline", timeline(woNo));
            boolean ok = emisClient.outboundDone(summary);
            opLogService.record("EMIS_PUSH", woNo, "OUTBOUND_DONE", "WCS",
                    "127.0.0.1", toJson(summary), "result=" + ok, ok, ok ? null : "EMIS 推送失败（开关关闭或网络异常），可重推");
        } catch (Exception e) {
            log.warn("[EMIS] 出库回传失败(不阻断): {} err={}", woNo, e.getMessage());
        }
    }

    /** 单据时间线：进仓/出仓/确认事件按时间升序 */
    private List<Map<String, Object>> timeline(String bizNo) {
        List<XuehuaAccessLog> logs = accessLogMapper.selectList(new QueryWrapper<XuehuaAccessLog>()
                .eq("biz_no", bizNo).orderByAsc("event_time"));
        List<Map<String, Object>> out = new ArrayList<>(logs.size());
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<Integer, String> names = Map.of(XuehuaAccessLog.EVT_IN, "进仓", XuehuaAccessLog.EVT_OUT, "出仓",
                XuehuaAccessLog.EVT_PUTAWAY, "放置确认", XuehuaAccessLog.EVT_PICK, "拣配确认",
                XuehuaAccessLog.EVT_UNAUTHORIZED, "非授权进入", XuehuaAccessLog.EVT_TAILGATE, "尾随进入");
        for (XuehuaAccessLog l : logs) {
            Map<String, Object> item = new LinkedHashMap<>(4);
            item.put("event", names.getOrDefault(l.getEventType(), String.valueOf(l.getEventType())));
            item.put("person", l.getPersonName());
            item.put("time", fmt.format(l.getEventTime()));
            item.put("camera", l.getCameraIndex());
            out.add(item);
        }
        return out;
    }
}
