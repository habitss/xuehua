package com.keytech.xuehua.emis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * EMIS 回传客户端——入库完成 / 出库完成 时把汇总信息推回 EMIS
 *
 * <p>契约（我方定义，2026-09-18，EMIS 侧适配）：
 * POST {base-url}/emis/wcs/inboundDone  body {externalNo, inboundNo, deliveryNo, operator,
 *   lines:[{binCode, qty}], enterTime, exitTime, timeline:[{event, person, time}]}
 * POST {base-url}/emis/wcs/outboundDone body {externalNo, woNo, partCode, qty, receiveUser,
 *   lines:[{binCode, qty}], enterTime, exitTime, timeline:[...]}
 * 应答 2xx 即成功；失败记 op_log(success=0) 可手动重推。
 *
 * <p>降级：xuehua.emis.enabled=false 时跳过推送并返回 false（完成判定照常，回传留痕可重推）。
 */
@Slf4j
@Component
public class EmisClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Value("${xuehua.emis.enabled:false}")
    private boolean enabled;

    @Value("${xuehua.emis.base-url:}")
    private String baseUrl;

    @Value("${xuehua.emis.timeout-ms:5000}")
    private int timeoutMs;

    public boolean isEnabled() {
        return enabled;
    }

    /** 入库完成回传 */
    public boolean inboundDone(Map<String, Object> summary) {
        return post("/emis/wcs/inboundDone", summary);
    }

    /** 出库完成回传 */
    public boolean outboundDone(Map<String, Object> summary) {
        return post("/emis/wcs/outboundDone", summary);
    }

    /** 盘点完成回传（说明书 5.1：盘点结果实时回传 Emis） */
    public boolean inventoryDone(Map<String, Object> summary) {
        return post("/emis/wcs/inventoryDone", summary);
    }

    private boolean post(String path, Map<String, Object> body) {
        if (!enabled) {
            log.info("[EMIS] 开关关闭，跳过回传: {} externalNo={}", path, body.get("externalNo"));
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
            log.info("[EMIS] {} -> {} {}", path, response.statusCode(),
                    response.body() == null ? "" : response.body().substring(0, Math.min(120, response.body().length())));
            return ok;
        } catch (Exception e) {
            log.warn("[EMIS] 回传失败: {} err={}", path, e.getMessage());
            return false;
        }
    }
}
