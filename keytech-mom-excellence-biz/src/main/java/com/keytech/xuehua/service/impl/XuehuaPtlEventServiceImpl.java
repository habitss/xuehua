package com.keytech.xuehua.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keytech.xuehua.entity.XuehuaAccessLog;
import com.keytech.xuehua.entity.XuehuaBin;
import com.keytech.xuehua.entity.XuehuaOutbound;
import com.keytech.xuehua.entity.XuehuaOutboundDetail;
import com.keytech.xuehua.mapper.XuehuaBinMapper;
import com.keytech.xuehua.ptl.PtlClient;
import com.keytech.xuehua.service.IXuehuaOpLogService;
import com.keytech.xuehua.service.IXuehuaOutboundService;
import com.keytech.xuehua.service.IXuehuaPtlEventService;
import com.keytech.xuehua.service.IXuehuaPutawayService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Description: PTL 设备事件回调 服务实现类
 * 响应契约（2026-09-09 精简版文档）：code=200 成功；可反控标签——
 * 出库确认后若同货位还有未完成工单，按文档 2.7 直接在响应里发起下一任务（orderNo=下一单+蓝闪+controls）；
 * 否则 orderNo 空串清屏 + ledState=0 熄灯；忽略/异常场景不返回这些字段（标签保持原样不误清屏）。
 */
@Slf4j
@Service
public class XuehuaPtlEventServiceImpl implements IXuehuaPtlEventService {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 接口文档：确认键 */
    private static final String BT_OK = "OKKey";
    private static final String OP_PTL = "PTL按键";
    private static final String LOG_BIZ_TYPE = "PTL_CALLBACK";

    @Resource
    private IXuehuaPutawayService xuehuaPutawayService;

    @Resource
    private IXuehuaOutboundService xuehuaOutboundService;

    @Resource
    private IXuehuaOpLogService xuehuaOpLogService;

    @Resource
    private XuehuaBinMapper xuehuaBinMapper;

    @Resource
    private PtlClient ptlClient;

    @Resource
    private com.keytech.xuehua.service.IXuehuaMonitorService xuehuaMonitorService;

    @Resource
    private com.keytech.xuehua.service.IXuehuaPersonService xuehuaPersonService;

    @Resource
    private com.keytech.xuehua.service.IXuehuaInboundService xuehuaInboundService;

    @Override
    public Map<String, Object> handleTagEvent(Map<String, Object> event, String clientIp) {
        String orderNo = str(event.get("orderNo"));
        String btName = str(event.get("btName"));
        log.info("[PTL] 标签按键回调: orderNo={} tagAdd={} btName={} ip={}",
                orderNo, event.get("tagAdd"), btName, clientIp);

        Map<String, Object> resp = new LinkedHashMap<>(8);
        String msg;
        String errorMsg = null;
        boolean success = true;
        try {
            if (BT_OK.equals(btName)) {
                Integer tagAdd = parseTagAdd(event.get("tagAdd"));
                if (StrUtil.isNotBlank(orderNo)) {
                    // 标准路径：标签回显当前任务单号，按单号前缀路由
                    if (orderNo.startsWith("IN-")) {
                        boolean ok = xuehuaPutawayService.confirmPutawayByInboundNo(orderNo, OP_PTL);
                        if (ok) {
                            clearTag(resp);
                            // 时间线（监控管理）：放置确认事件（person=入库单操作人）
                            com.keytech.xuehua.entity.XuehuaInbound inb = xuehuaInboundService.findByInboundNo(orderNo);
                            if (inb != null) {
                                com.keytech.xuehua.entity.XuehuaPerson op = xuehuaPersonService.resolveByOperator(inb.getOperator());
                                xuehuaMonitorService.recordAccess(op, inb.getOperator(), com.keytech.xuehua.entity.XuehuaAccessLog.EVT_PUTAWAY,
                                        inb.getBinCode(), "PTL", "ptl", null, orderNo, null, new java.util.Date());
                            }
                            msg = "放置确认成功";
                        } else {
                            msg = "放置确认：无进行中的上架任务（已完成或单号不存在）";
                        }
                    } else if (orderNo.startsWith("WO-")) {
                        // 行级确认：一张单拆多库位时，按键标签(tagAdd→库位)决定确认哪一行
                        XuehuaOutboundDetail picked = xuehuaOutboundService.confirmPickByWoNo(orderNo, tagAdd, OP_PTL);
                        if (picked != null) {
                            // 链式：① 同单下一行(跨库位) ② 刚拣库位压着的其他单 ③ 清屏
                            boolean chained = chainNext(resp, orderNo, picked.getBinCode(), tagAdd);
                            // 时间线（监控管理）：拣配确认事件落进出流水，与进仓/出仓构成完整作业链
                            xuehuaMonitorService.recordAccess(null, picked.getPickBy(), XuehuaAccessLog.EVT_PICK,
                                    picked.getBinCode(), "PTL", "ptl", null, orderNo, null, new java.util.Date());
                            msg = chained ? "拣配确认成功，已下发下一任务" : "拣配确认成功";
                        } else {
                            msg = "拣配确认：无进行中的出库工单（已完成或单号不存在）";
                        }
                    } else {
                        msg = "未识别的单号前缀，忽略";
                    }
                } else if (tagAdd != null) {
                    // 兜底：平台未回带单号时，按标签地址反查该货位的进行中任务
                    boolean ok = xuehuaPutawayService.confirmPutawayByTagAdd(tagAdd, OP_PTL);
                    if (ok) {
                        clearTag(resp);
                        msg = "放置确认成功（按标签" + tagAdd + "反查）";
                    } else {
                        XuehuaOutboundDetail picked = xuehuaOutboundService.confirmPickByTagAdd(tagAdd, OP_PTL);
                        if (picked != null) {
                            boolean chained = chainNext(resp, picked.getWoNo(), picked.getBinCode(), tagAdd);
                            msg = chained ? "拣配确认成功（按标签" + tagAdd + "反查），已下发下一任务"
                                    : "拣配确认成功（按标签" + tagAdd + "反查）";
                        } else {
                            msg = "确认失败：该标签对应货位无进行中任务（或标签未绑定货位）";
                        }
                    }
                } else {
                    msg = "忽略按键 " + btName + "：无单号且无标签地址";
                }
            } else {
                // UpKey/DownKey/MenuKey 等按键当前无业务动作，正常应答且不动标签显示
                msg = "忽略按键 " + btName;
            }
        } catch (IllegalStateException e) {
            // 业务拒绝（并发双按键的输家等）：终态已达成属幂等重放；
            // 不返回标签控制字段——同一瞬间的胜者响应才是标签应有的最终状态
            msg = e.getMessage();
        } catch (Exception e) {
            // 真实处理失败：如实返回错误码（处理幂等，网关若重试是安全的）
            log.warn("[PTL] 按键回调处理失败: orderNo={} err={}", orderNo, e.getMessage());
            msg = "处理失败：" + e.getMessage();
            errorMsg = e.getMessage();
            success = false;
        }
        resp.put("code", success ? 200 : 500);
        resp.put("msg", msg);
        // 审计留痕：确认/忽略/异常全记录
        xuehuaOpLogService.record(LOG_BIZ_TYPE, orderNo, "TAG_" + btName, OP_PTL,
                clientIp, toJson(event), toJson(resp), success, errorMsg);
        return resp;
    }

    /**
     * 链式下发（2026-09-18 升级为行级 + 跨标签主动点亮），三级优先：
     * ① 同工单下一张待拣行（跨库位——一张领料单拆多库位时，拣完 seq=1 的库位
     *    点亮 seq=2 的库位标签，拣货人走到下一库位按键即拣）；
     * ② 工单已完成 → 刚拣库位上压着的其他工单待拣行（原"同货位下一单"语义）；
     * ③ 都没有 → 清屏熄灯（orderNo 空串 + ledState=0）
     *
     * @param woNo              刚确认的工单号
     * @param justPickedBinCode 刚确认行所在的库位
     * @param justPressedTagAdd 刚按键的标签地址（跨标签分流用）
     * @return true=已链下一任务，false=无下一任务（resp 已清屏）
     */
    private boolean chainNext(Map<String, Object> resp, String woNo, String justPickedBinCode, Integer justPressedTagAdd) {
        // ① 同单下一行（跨库位链式）
        XuehuaOutboundDetail next = xuehuaOutboundService.findNextLineByWoNo(woNo);
        if (next != null) {
            return dispatchLineTask(resp, woNo, next, justPressedTagAdd);
        }
        // ② 刚拣库位上其他工单的待拣行
        if (StrUtil.isNotBlank(justPickedBinCode)) {
            XuehuaOutboundDetail other = xuehuaOutboundService.findNextLineByBinCode(justPickedBinCode);
            if (other != null) {
                return dispatchLineTask(resp, other.getWoNo(), other, justPressedTagAdd);
            }
        }
        // ③ 链尾：清屏熄灯
        clearTag(resp);
        return false;
    }

    /**
     * 把下一站任务送到对应标签。
     *
     * <p>【平台限制（2026-09-18 真机实测）】按键应答的反控只作用于"被按的那张标签"——
     * 应答体里写其他标签的 tagAdd 平台不执行（WO-2026-0020 跨标签首验失败：被按的标签1
     * 被应答重亮、标签2不亮）。因此分流：
     * <ul>
     *   <li>下一站在被按的标签上（同库位压单）：应答体携带任务字段，平台重亮自己（已验证可行）；</li>
     *   <li>下一站在别的标签（跨库位拆单常态）：应答体清屏（熄掉被按标签），
     *       <b>主动调 DispTag/Control</b> 点亮下一站标签——此刻确认事务已提交，
     *       主动推送读到的台账是提交后状态；推送失败仅 warn 不阻断（与全项目口径一致）。</li>
     * </ul>
     */
    private boolean dispatchLineTask(Map<String, Object> resp, String orderNo, XuehuaOutboundDetail line, Integer justPressedTagAdd) {
        XuehuaBin bin = xuehuaBinMapper.selectOne(
                new QueryWrapper<XuehuaBin>().eq("bin_code", line.getBinCode()).last("limit 1"));
        Integer nextTagAdd = bin == null ? null : bin.getTagAdd();
        if (nextTagAdd != null && nextTagAdd.equals(justPressedTagAdd)) {
            // 同标签：应答体携带任务，平台重亮自己
            fillLineTask(resp, orderNo, line, bin);
            return true;
        }
        // 跨标签：应答清屏 + 主动点亮下一站
        clearTag(resp);
        if (nextTagAdd != null) {
            XuehuaOutbound header = xuehuaOutboundService.findByWoNo(orderNo);
            ptlClient.lightOn(PtlClient.TASK_OUTBOUND, orderNo, nextTagAdd,
                    line.getQty() == null ? 0 : line.getQty(),
                    PtlClient.COLOR_BLUE,
                    StrUtil.blankToDefault(line.getPartCode(), ""),
                    header == null ? "" : StrUtil.blankToDefault(header.getPartName(), ""),
                    bin.getQty());
            log.info("[PTL] 跨标签链式主动点亮: woNo={} seq={} bin={} tagAdd={} qty={}",
                    orderNo, line.getSeq(), line.getBinCode(), nextTagAdd, line.getQty());
        } else {
            log.warn("[PTL] 下一站库位未绑定标签，跳过点亮: woNo={} bin={}", orderNo, line.getBinCode());
        }
        return true;
    }

    /**
     * 应答体填充一张待拣行任务（仅同标签链式使用）：orderNo + 蓝闪 +
     * controls{6000=行数量, 4005/4006=物料名/编码, 4010=该库位现存量}
     */
    private void fillLineTask(Map<String, Object> resp, String orderNo, XuehuaOutboundDetail line, XuehuaBin bin) {
        // 标签屏"库存"控件(4010)显示该库位当前现存量：上一行已确认扣减过，查到的就是实时值
        Integer stockNow = bin == null ? null : bin.getQty();
        // 物料名从头表取（行表只冗余了编码）
        XuehuaOutbound header = xuehuaOutboundService.findByWoNo(orderNo);
        resp.put("orderNo", orderNo);
        resp.put("ledColor", PtlClient.COLOR_BLUE);
        resp.put("ledState", 1);
        Map<String, Object> controls = new LinkedHashMap<>(8);
        controls.put("6000", String.valueOf(line.getQty() == null ? 0 : line.getQty()));
        controls.put("4005", header == null ? "" : StrUtil.blankToDefault(header.getPartName(), ""));
        controls.put("4006", StrUtil.blankToDefault(line.getPartCode(), ""));
        controls.put("4010", stockNow == null ? "" : String.valueOf(stockNow));
        resp.put("controls", controls);
        log.info("[PTL] 链式下发下一任务(同标签): woNo={} 行seq={} bin={} qty={}",
                orderNo, line.getSeq(), line.getBinCode(), line.getQty());
    }

    /**
     * 清屏熄灯：orderNo 空串清除任务，ledState=0 熄灯（与 confirmXxx 里的主动 lightOff 互为双保险）
     */
    private void clearTag(Map<String, Object> resp) {
        resp.put("orderNo", "");
        resp.put("ledColor", "0,0,0");
        resp.put("ledState", 0);
    }

    /**
     * tagAdd 兼容解析：JSON 里可能是数字也可能是字符串
     */
    private static Integer parseTagAdd(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            String s = String.valueOf(o).trim();
            return s.isEmpty() ? null : Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String toJson(Object o) {
        try {
            return JSON.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
