package com.keytech.xuehua.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.keytech.xuehua.entity.XuehuaBin;
import com.keytech.xuehua.entity.XuehuaInbound;
import com.keytech.xuehua.entity.XuehuaInventory;
import com.keytech.xuehua.entity.XuehuaOutbound;
import com.keytech.xuehua.entity.XuehuaOutboundDetail;
import com.keytech.xuehua.entity.XuehuaPart;
import com.keytech.xuehua.entity.XuehuaPerson;
import com.keytech.xuehua.entity.XuehuaPutaway;
import com.keytech.xuehua.service.IXuehuaBinService;
import com.keytech.xuehua.service.IXuehuaInboundService;
import com.keytech.xuehua.service.IXuehuaOutboundService;
import com.keytech.xuehua.service.IXuehuaPartService;
import com.keytech.xuehua.service.IXuehuaPersonService;
import com.keytech.xuehua.service.IXuehuaPutawayService;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.aspect.annotation.AutoLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 扫码枪统一网关（手持终端 DS-MDT201/4&64/GLE，安卓 PDA App 调用）
 *
 * <p>PDA App 扫到任何码都先调 /resolve 识别类型（备件条码/库位码/业务单号），
 * 返回上下文与建议动作；出库拣配场景用 /pick 逐行核销（与页面/PTL 按键同轨，
 * 最终都收敛 confirmPickLine——行级条件更新 + 台账原子扣减 + 链式点亮）。
 *
 * <p>鉴权口径与 EMIS receive 同款：匿名白名单 + operator 必须已注册（人员注册模块，
 * 未注册人员不能进仓作业）；应答 {code:200/500, msg, ...}，重试安全（核销幂等由行级
 * 条件更新保证——已拣行再扫自动落到下一行）。
 */
@Tag(name = "扫码枪网关")
@RestController
@RequestMapping("/xuehua/scan")
@Slf4j
public class XuehuaScanController {

    @Autowired
    private IXuehuaPartService partService;
    @Autowired
    private IXuehuaBinService binService;
    @Autowired
    private IXuehuaInboundService inboundService;
    @Autowired
    private IXuehuaOutboundService outboundService;
    @Autowired
    private IXuehuaPutawayService putawayService;
    @Autowired
    private IXuehuaPersonService personService;

    /**
     * 统一扫码识别：扫任何码先来这——识别类型并返回上下文 + 建议动作。
     *
     * <p>识别优先级：业务单号（IN-/WO-/PV- 前缀）→ 备件条码（part_code 精确）→
     * 库位码（bin_code 精确）。返回 type + 上下文 + actions（PDA App 直接渲染按钮）。
     */
    @AutoLog(value = "扫码枪-统一识别")
    @Operation(summary = "扫码枪-统一识别(匿名)")
    @GetMapping(value = "/resolve")
    public Map<String, Object> resolve(@RequestParam(name = "code") String code,
                                       @RequestParam(name = "operator", required = false) String operator) {
        Map<String, Object> resp = new LinkedHashMap<>(16);
        try {
            String c = code == null ? "" : code.trim();
            if (c.isEmpty()) {
                throw new IllegalStateException("扫码内容为空");
            }
            // 单号优先：前缀路由
            if (c.startsWith("IN-")) {
                resolveInbound(c, resp);
            } else if (c.startsWith("WO-")) {
                resolveOutbound(c, resp);
            } else if (c.startsWith("PV-")) {
                resolveInventory(c, resp);
            } else {
                // 备件条码 → 库位码
                XuehuaPart part = partService.getOne(
                        new QueryWrapper<XuehuaPart>().eq("part_code", c).last("limit 1"));
                if (part != null) {
                    resolvePart(part, resp);
                } else {
                    XuehuaBin bin = binService.getOne(
                            new QueryWrapper<XuehuaBin>().eq("bin_code", c).last("limit 1"));
                    if (bin != null) {
                        resolveBin(bin, resp);
                    } else {
                        throw new IllegalStateException("未识别的条码：" + c);
                    }
                }
            }
            resp.put("code", 200);
        } catch (IllegalStateException e) {
            resp.put("code", 500);
            resp.put("msg", e.getMessage());
        } catch (Exception e) {
            log.error("扫码识别异常", e);
            resp.put("code", 500);
            resp.put("msg", "识别失败：" + e.getMessage());
        }
        return resp;
    }

    /**
     * 出库扫码核销：扫备件条码确认一行拣配（原型 scanOut/obScanOne 的行级版）。
     *
     * <p>扫码 = 该工单下一待拣行的拣配完成——行级条件更新（WHERE status=0）+
     * 该行库位台账原子扣减 + 跨库位链式点亮下一站，与页面/PTL 按键同一条链路；
     * 备件编码不匹配拒绝（防拿错料），已扫完自动提示工单完成。
     */
    @AutoLog(value = "扫码枪-出库核销")
    @Operation(summary = "扫码枪-出库核销(匿名)")
    @PostMapping(value = "/pick")
    public Map<String, Object> pick(@RequestParam(name = "woNo") String woNo,
                                    @RequestParam(name = "code") String code,
                                    @RequestParam(name = "operator", required = false) String operator) {
        Map<String, Object> resp = new LinkedHashMap<>(16);
        try {
            if (StrUtil.isBlank(woNo)) {
                throw new IllegalStateException("woNo(出库工单号)不能为空");
            }
            String scanned = code == null ? "" : code.trim();
            if (scanned.isEmpty()) {
                throw new IllegalStateException("扫码内容为空");
            }
            XuehuaOutbound order = outboundService.getOne(
                    new QueryWrapper<XuehuaOutbound>().eq("wo_no", woNo).last("limit 1"));
            if (order == null) {
                throw new IllegalStateException("工单不存在：" + woNo);
            }
            if (order.getStatus() != null && order.getStatus() == 2) {
                throw new IllegalStateException("工单已出库完成，无需再扫");
            }
            // 防拿错料：扫的备件码必须与工单一致（单备件工单口径）
            if (StrUtil.isNotBlank(order.getPartCode()) && !order.getPartCode().equals(scanned)) {
                throw new IllegalStateException("备件不符：工单为 " + order.getPartCode() + "，扫到 " + scanned);
            }
            // 下一待拣行（FIFO 顺序）
            List<XuehuaOutboundDetail> pending = outboundService.listLinesByWoNo(woNo);
            XuehuaOutboundDetail line = pending.stream()
                    .filter(l -> l.getStatus() != null && l.getStatus() == 0).findFirst()
                    .orElseThrow(() -> new IllegalStateException("该工单无待拣行（可能已全部扫码完成）"));
            String op = requireOperator(operator);
            XuehuaOutboundDetail done = outboundService.confirmPickLine(line.getId(), op);
            long remaining = outboundService.listLinesByWoNo(woNo).stream()
                    .filter(l -> l.getStatus() != null && l.getStatus() == 0).count();
            resp.put("code", 200);
            resp.put("msg", remaining == 0 ? "扫码出库成功，工单已完成" : "已核销库位 " + done.getBinCode() + "，剩余 " + remaining + " 行");
            resp.put("pickedBin", done.getBinCode());
            resp.put("remainingLines", remaining);
            resp.put("woStatus", remaining == 0 ? 2 : 1);
        } catch (IllegalStateException e) {
            resp.put("code", 500);
            resp.put("msg", e.getMessage());
        } catch (Exception e) {
            log.error("扫码出库异常", e);
            resp.put("code", 500);
            resp.put("msg", "核销失败：" + e.getMessage());
        }
        return resp;
    }

    // ================= 各类型上下文组装 =================

    private void resolveInbound(String no, Map<String, Object> resp) {
        XuehuaInbound ib = inboundService.getOne(
                new QueryWrapper<XuehuaInbound>().eq("inbound_no", no).last("limit 1"));
        if (ib == null) {
            throw new IllegalStateException("入库单不存在：" + no);
        }
        resp.put("type", "inbound");
        resp.put("inboundNo", ib.getInboundNo());
        resp.put("status", ib.getStatus());
        resp.put("statusText", ib.getStatus() != null && ib.getStatus() == 1 ? "已登记" : "待登记");
        resp.put("partCode", ib.getPartCode());
        resp.put("partName", ib.getPartName());
        resp.put("qty", ib.getQty());
        resp.put("binCode", ib.getBinCode());
        // 建议动作：有上架任务 → 扫码确认上架
        List<XuehuaPutaway> tasks = putawayService.list(
                new QueryWrapper<XuehuaPutaway>().eq("inbound_no", no).eq("status", 0));
        List<Map<String, Object>> actions = new ArrayList<>();
        if (!tasks.isEmpty()) {
            XuehuaPutaway t = tasks.get(0);
            Map<String, Object> a = new LinkedHashMap<>(8);
            a.put("action", "confirmPutaway");
            a.put("text", "确认上架 → " + t.getBinCode());
            a.put("api", "POST /xuehua/putaway/confirmPutaway?id=" + t.getId());
            actions.add(a);
            resp.put("putawayTaskId", t.getId());
            resp.put("putawayBin", t.getBinCode());
        }
        resp.put("actions", actions);
    }

    private void resolveOutbound(String no, Map<String, Object> resp) {
        XuehuaOutbound ob = outboundService.getOne(
                new QueryWrapper<XuehuaOutbound>().eq("wo_no", no).last("limit 1"));
        if (ob == null) {
            throw new IllegalStateException("出库工单不存在：" + no);
        }
        resp.put("type", "outbound");
        resp.put("woNo", ob.getWoNo());
        resp.put("status", ob.getStatus());
        resp.put("statusText", ob.getStatus() != null && ob.getStatus() == 2 ? "已出库"
                : ob.getStatus() != null && ob.getStatus() == 1 ? "拣配中" : "待拣配");
        resp.put("partCode", ob.getPartCode());
        resp.put("partName", ob.getPartName());
        resp.put("qty", ob.getQty());
        resp.put("receiveUser", ob.getReceiveUser());
        List<Map<String, Object>> lines = new ArrayList<>();
        for (XuehuaOutboundDetail l : outboundService.listLinesByWoNo(no)) {
            Map<String, Object> row = new LinkedHashMap<>(8);
            row.put("seq", l.getSeq());
            row.put("binCode", l.getBinCode());
            row.put("qty", l.getQty());
            row.put("status", l.getStatus());
            lines.add(row);
        }
        resp.put("lines", lines);
        List<Map<String, Object>> actions = new ArrayList<>();
        if (ob.getStatus() != null && ob.getStatus() < 2) {
            Map<String, Object> a = new LinkedHashMap<>(8);
            a.put("action", "scanPick");
            a.put("text", "逐行扫码核销（扫备件条码 " + ob.getPartCode() + "）");
            a.put("api", "POST /xuehua/scan/pick?woNo=" + no + "&code=" + ob.getPartCode());
            actions.add(a);
        }
        resp.put("actions", actions);
    }

    private void resolveInventory(String no, Map<String, Object> resp) {
        resp.put("type", "inventory");
        resp.put("pvNo", no);
        resp.put("msg", "盘点单识别成功——盘点明细请到盘点页面操作（submitReal 逐行实盘）");
        resp.put("actions", new ArrayList<>());
    }

    private void resolvePart(XuehuaPart part, Map<String, Object> resp) {
        resp.put("type", "part");
        resp.put("partCode", part.getPartCode());
        resp.put("partName", part.getPartName());
        resp.put("model", part.getModel());
        resp.put("category", part.getCategory());
        resp.put("unit", part.getUnit());
        // 在库分布
        List<Map<String, Object>> stock = new ArrayList<>();
        long total = 0;
        for (XuehuaBin b : binService.list(new QueryWrapper<XuehuaBin>()
                .eq("part_code", part.getPartCode()).ne("status", 0).gt("qty", 0))) {
            Map<String, Object> row = new LinkedHashMap<>(8);
            row.put("binCode", b.getBinCode());
            row.put("qty", b.getQty());
            stock.add(row);
            total += b.getQty() == null ? 0 : b.getQty();
        }
        resp.put("stock", stock);
        resp.put("totalQty", total);
        resp.put("safeStock", part.getSafeStock());
        // 该备件在途拣配任务
        List<XuehuaOutbound> picking = outboundService.list(new QueryWrapper<XuehuaOutbound>()
                .eq("part_code", part.getPartCode()).in("status", 0, 1));
        List<Map<String, Object>> actions = new ArrayList<>();
        for (XuehuaOutbound ob : picking) {
            Map<String, Object> a = new LinkedHashMap<>(8);
            a.put("action", "scanPick");
            a.put("text", "核销出库 " + ob.getWoNo() + "（" + ob.getQty() + " 件）");
            a.put("api", "POST /xuehua/scan/pick?woNo=" + ob.getWoNo() + "&code=" + part.getPartCode());
            actions.add(a);
        }
        resp.put("actions", actions);
    }

    private void resolveBin(XuehuaBin bin, Map<String, Object> resp) {
        resp.put("type", "bin");
        resp.put("binCode", bin.getBinCode());
        resp.put("status", bin.getStatus());
        resp.put("partCode", bin.getPartCode());
        resp.put("qty", bin.getQty());
        resp.put("tagAdd", bin.getTagAdd());
        List<Map<String, Object>> actions = new ArrayList<>();
        // 该库位待上架任务
        for (XuehuaPutaway t : putawayService.list(new QueryWrapper<XuehuaPutaway>()
                .eq("bin_code", bin.getBinCode()).eq("status", 0))) {
            Map<String, Object> a = new LinkedHashMap<>(8);
            a.put("action", "confirmPutaway");
            a.put("text", "确认上架 " + t.getInboundNo() + " → 此库位");
            a.put("api", "POST /xuehua/putaway/confirmPutaway?id=" + t.getId());
            actions.add(a);
        }
        resp.put("actions", actions);
    }

    /** 操作人解析：可空（记"扫码枪"）；给了就必须已注册（与 EMIS 接收同口径） */
    private String requireOperator(String operator) {
        if (StrUtil.isBlank(operator)) {
            return "扫码枪";
        }
        XuehuaPerson p = personService.resolveByOperator(operator.trim());
        if (p == null) {
            throw new IllegalStateException("操作人 " + operator + " 未在人员注册中登记");
        }
        return p.getEmpNo();
    }
}
