package com.keytech.xuehua.ptl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PTL 彩屏标签客户端（对接灯光指引拣选系统，契约见《08-设备接口文档(精简版)》2026-09-09 版）
 *
 * 控制接口 POST /api/DispTag/Control（匿名，异步下发——受理即返回，真实成败看平台后台日志）：
 *   请求 {taskType, taskDetails:[{orderNo, tagAdd, ledColor, ledState, controls}]}
 *     - orderNo：任务单号；空字符串=熄灭屏幕（清除任务）
 *     - ledState：0熄灭(颜色强制黑)/1闪烁/2常亮
 *     - controls：{控件ID:显示内容}，4000段=文本、6000段=数码管(Seg 6000 约定为任务数量)
 *   响应 {success, message, successCount, ...}，success=true 即受理成功
 *
 * 按键回调(DispTagButtonClick)的响应同理控灯：见 XuehuaPtlEventServiceImpl。
 * 网关不可达/返回异常仅记录日志，不阻断业务。
 */
@Slf4j
@Component
public class PtlClient {

    public static final String TASK_INBOUND = "Inbound";
    public static final String TASK_OUTBOUND = "Outbound";

    public static final String COLOR_RED = "255,0,0";
    public static final String COLOR_BLUE = "0,102,255";

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 彩屏标签控制接口（2026-09-09 精简版文档为准；旧文档 /api/v2/ctltfttag 平台未实现） */
    private static final String API_CONTROL = "/api/DispTag/Control";

    @Value("${xuehua.ptl.enabled:true}")
    private boolean enabled;

    @Value("${xuehua.ptl.base-url:http://192.168.82.161:5000}")
    private String baseUrl;

    @Value("${xuehua.ptl.timeout-ms:3000}")
    private int timeoutMs;

    /**
     * 点亮标签（闪烁）：数码管6000显示任务数量，文本4005/4006显示备件名称/编码，
     * 文本4010显示库位库存（模板"库存:"标签4002旁边的数值控件，2026-09-11 从模板编辑器实测确认）
     *
     * @param qty    任务数量（这单要入库/拣配多少）
     * @param stock  库位当前实物库存（亮灯时刻，不含本单任务量），null 显示空白
     */
    public boolean lightOn(String taskType, String orderNo, Integer tagAdd, Integer qty,
                           String ledColor, String partCode, String partName, Integer stock) {
        Map<String, Object> detail = new LinkedHashMap<>(8);
        detail.put("orderNo", orderNo == null ? "" : orderNo);
        detail.put("tagAdd", tagAdd);
        detail.put("ledColor", ledColor);
        detail.put("ledState", 1);
        Map<String, Object> controls = new LinkedHashMap<>(8);
        controls.put("6000", String.valueOf(qty == null ? 0 : qty));
        controls.put("4005", partName == null ? "" : partName);
        controls.put("4006", partCode == null ? "" : partCode);
        controls.put("4010", stock == null ? "" : String.valueOf(stock));
        detail.put("controls", controls);
        return control(taskType, orderNo, tagAdd, detail);
    }

    /**
     * 熄灭标签：orderNo 空串清除任务熄屏，ledState=0 熄灯（平台强制颜色为黑）
     */
    public boolean lightOff(String taskType, String orderNo, Integer tagAdd) {
        Map<String, Object> detail = new LinkedHashMap<>(8);
        detail.put("orderNo", "");
        detail.put("tagAdd", tagAdd);
        detail.put("ledColor", "0,0,0");
        detail.put("ledState", 0);
        return control(taskType, orderNo, tagAdd, detail);
    }

    private boolean control(String taskType, String orderNo, Integer tagAdd, Map<String, Object> detail) {
        if (!enabled) {
            log.info("[PTL] 开关关闭，跳过亮灯: taskType={} orderNo={} tagAdd={}", taskType, orderNo, tagAdd);
            return false;
        }
        if (tagAdd == null) {
            log.warn("[PTL] 库位未绑定标签地址(tag_add)，跳过亮灯: taskType={} orderNo={}", taskType, orderNo);
            return false;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>(4);
            body.put("taskType", taskType);
            List<Map<String, Object>> taskDetails = new ArrayList<>(1);
            taskDetails.add(detail);
            body.put("taskDetails", taskDetails);

            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + API_CONTROL))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            String resp = response.body();
            log.info("[PTL] DispTag/Control: taskType={} orderNo={} tagAdd={} ledState={} -> {} {}",
                    taskType, detail.get("orderNo"), tagAdd, detail.get("ledState"), response.statusCode(), resp);
            return response.statusCode() == 200 && resp != null && resp.contains("\"success\":true");
        } catch (Exception e) {
            log.warn("[PTL] 亮灯调用失败(不阻断业务): taskType={} orderNo={} tagAdd={} err={}",
                    taskType, orderNo, tagAdd, e.getMessage());
            return false;
        }
    }
}
