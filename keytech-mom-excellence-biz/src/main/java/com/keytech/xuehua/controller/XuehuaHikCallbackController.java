package com.keytech.xuehua.controller;

import com.keytech.xuehua.service.IXuehuaMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 海康事件回调 Controller（监控管理，匿名端点——shiro 白名单 /xuehua/hik/**）
 *
 * <p>事件契约（我方定义，海康联调期适配其真实报文结构——iSecure 事件订阅
 * 的告警事件信息体解析后归一化为下面的字段）：
 * POST /xuehua/hik/event
 * {direction:"in|out", empNo?(工号), cardNo?(卡号), personName?, zone?, cameraIndex?, eventTime?"yyyy-MM-dd HH:mm:ss"}
 *
 * <p>降级注入：hik.enabled=false 时用 /xuehua/hik/mock 模拟门禁事件（自测/演示用）。
 */
@Tag(name = "海康事件回调")
@RestController
@RequestMapping("/xuehua/hik")
@Slf4j
public class XuehuaHikCallbackController {

    @Autowired
    private IXuehuaMonitorService xuehuaMonitorService;

    @Autowired
    private com.keytech.xuehua.service.IXuehuaPersonService xuehuaPersonService;

    /** 海康门禁/视频事件入口（真实来源） */
    @AutoLog(value = "海康事件-门禁进出")
    @Operation(summary = "海康事件-门禁进出(匿名)")
    @PostMapping(value = "/event")
    public Map<String, Object> event(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return handle(body, request.getRemoteAddr(), "hik");
    }

    /**
     * 人员注册信息推送（海康 → WCS，档案源头在海康——2026-09-18 用户纠正后的数据流向）。
     * 契约：{empNo(必), name(必), dept?, cardNo?, authMethod?, faceStatus?(1/0/2), hikPersonId, zones?}
     * 按 empNo upsert 幂等；WCS 只记录，不回推任何人员数据
     */
    @AutoLog(value = "海康事件-人员注册推送")
    @Operation(summary = "海康-人员注册推送(匿名)")
    @PostMapping(value = "/person")
    public Map<String, Object> person(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> resp = new LinkedHashMap<>(4);
        try {
            com.keytech.xuehua.entity.XuehuaPerson person = xuehuaPersonService.upsertFromHik(body);
            resp.put("code", 200);
            resp.put("msg", "人员注册信息已记录");
            resp.put("empNo", person.getEmpNo());
        } catch (IllegalStateException e) {
            resp.put("code", 500);
            resp.put("msg", e.getMessage());
        } catch (Exception e) {
            log.warn("[HIK] 人员推送处理失败: {}", e.getMessage());
            resp.put("code", 500);
            resp.put("msg", "处理失败：" + e.getMessage());
        }
        return resp;
    }

    /** 模拟注入（hik.enabled=false 的自测通道；启用后建议下线） */
    @Operation(summary = "海康事件-模拟注入(匿名)")
    @PostMapping(value = "/mock")
    public Map<String, Object> mock(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return handle(body, request.getRemoteAddr(), "mock");
    }

    private Map<String, Object> handle(Map<String, Object> body, String ip, String source) {
        Map<String, Object> resp = new LinkedHashMap<>(4);
        try {
            String direction = str(body.get("direction"));
            String empNo = str(body.get("empNo"));
            String cardNo = str(body.get("cardNo"));
            if (!"in".equalsIgnoreCase(direction) && !"out".equalsIgnoreCase(direction)) {
                resp.put("code", 500);
                resp.put("msg", "direction 必须是 in 或 out");
                return resp;
            }
            if ((empNo == null || empNo.isBlank()) && (cardNo == null || cardNo.isBlank())) {
                resp.put("code", 500);
                resp.put("msg", "empNo / cardNo 至少一个");
                return resp;
            }
            Date eventTime = parseTime(str(body.get("eventTime")));
            Map<String, Object> result = xuehuaMonitorService.handleHikEvent(direction, empNo, cardNo,
                    str(body.get("personName")), str(body.get("zone")), str(body.get("cameraIndex")), eventTime);
            log.info("[HIK] {}事件: dir={} emp={} card={} ip={} -> {}", source, direction, empNo, cardNo, ip, result.get("msg"));
            return result;
        } catch (Exception e) {
            log.warn("[HIK] 事件处理失败: {}", e.getMessage());
            resp.put("code", 500);
            resp.put("msg", "处理失败：" + e.getMessage());
            return resp;
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Date parseTime(String s) {
        if (s == null || s.isBlank()) {
            return new Date();
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception e) {
            return new Date();
        }
    }
}
