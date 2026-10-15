package com.keytech.xuehua.controller;

import com.keytech.xuehua.service.IXuehuaPtlEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @Description: PTL 设备事件回调（《PTL接口文档》设备事件触发的接口——
 *                由 PTLDeviceControl 网关反向调用，非 jeecg 登录态，已加入 shiro 白名单）
 */
@Tag(name = "PTL标签回调")
@RestController
@RequestMapping("/xuehua/ptl")
@Slf4j
public class XuehuaPtlCallbackController {

    @Resource
    private IXuehuaPtlEventService xuehuaPtlEventService;

    /**
     * 常规标签按钮按下触发接口（UpKey/DownKey/OKKey）
     * 现场部署时将该地址配置到 PTLDeviceControl：http://<wcs>:8286/keytech-boot/xuehua/ptl/tagEvent
     */
    @Operation(summary = "PTL常规标签按键回调")
    @PostMapping("/tagEvent")
    public Map<String, Object> tagEvent(@RequestBody Map<String, Object> event, HttpServletRequest request) {
        return xuehuaPtlEventService.handleTagEvent(event, clientIp(request));
    }

    private static String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}
