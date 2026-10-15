package com.keytech.xuehua.hik;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 海康威视平台客户端（iSecure Center / HikCentral OpenAPI，Artemis 网关）
 *
 * <p>职责（2026-09-18 用户纠正后的数据流向——人员档案源头在海康，WCS 只记录/查询/发授权指令）：
 * <ul>
 *   <li>人员注册：海康侧采集人脸/卡后<b>推送人员信息给 WCS</b>（/xuehua/hik/person，WCS 只记录）；
 *       WCS 不向海康推送人员档案；</li>
 *   <li>进仓授权：EMIS 推单校验操作人已注册后，WCS 调 authorizeEntry 让海康放行该人进仓
 *       （授权失败拒单，hik.enabled=false 降级跳过）；</li>
 *   <li>事件接入：门禁进出事件由海康推送，WCS 在 /xuehua/hik/event 接收并记时间线；</li>
 *   <li>回放取流：按 点位编码+时间段 换取回放地址。</li>
 * </ul>
 *
 * <p>降级口径：xuehua.hik.enabled=false（当前未联调）时所有方法直接返回未启用，
 * 业务侧降级——授权记"待推送"、事件用 /xuehua/hik/mock 注入、回放仅展示时间线。
 * 真机联调时换配置即可，业务代码零改动。
 *
 * <p>Artemis 网关调用规范：路径前缀 /artemis，Header 带 X-Ca-Key/X-Ca-Signature
 * （appKey/appSecret HMAC 签名）。骨架阶段用简化 Header 直连，联调期按海康
 * artemis-http-client 签名规则替换 {@link #artemisPost} 内部实现即可。
 */
@Slf4j
@Component
public class HikClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Value("${xuehua.hik.enabled:false}")
    private boolean enabled;

    @Value("${xuehua.hik.base-url:}")
    private String baseUrl;

    @Value("${xuehua.hik.app-key:}")
    private String appKey;

    @Value("${xuehua.hik.app-secret:}")
    private String appSecret;

    @Value("${xuehua.hik.timeout-ms:5000}")
    private int timeoutMs;

    // ===== ISAPI 设备直连（视频AI服务器/超脑NVR 本体，无 iSecure Center——设备选型 0914 已移除 ISC）=====
    // 设备协议 = ISAPI（HTTP REST，Digest 认证）+ RTSP 取流；以文搜图/以图搜图为设备本地智搜能力

    @Value("${xuehua.hik.isapi.enabled:false}")
    private boolean isapiEnabled;

    @Value("${xuehua.hik.isapi.base-url:}")
    private String isapiBaseUrl;

    @Value("${xuehua.hik.isapi.username:admin}")
    private String isapiUsername;

    @Value("${xuehua.hik.isapi.password:}")
    private String isapiPassword;

    public boolean isIsapiEnabled() {
        return isapiEnabled && StrUtil.isNotBlank(isapiBaseUrl);
    }

    /**
     * ISAPI 设备直连 GET（Digest 认证）：拉通道列表/设备信息/抓图等。
     * JDK HttpClient 的 Authenticator 自动完成 401 Digest 质询握手。
     *
     * @return 响应正文（XML）；未启用/失败返回 null（调用方降级提示，不阻断）
     */
    public String isapiGet(String path) {
        if (!isIsapiEnabled()) {
            log.info("[HIK-ISAPI] 开关关闭，跳过设备调用: {}", path);
            return null;
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .authenticator(new java.net.Authenticator() {
                        @Override
                        protected java.net.PasswordAuthentication getPasswordAuthentication() {
                            return new java.net.PasswordAuthentication(isapiUsername, isapiPassword.toCharArray());
                        }
                    })
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(isapiBaseUrl + path))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            log.info("[HIK-ISAPI] GET {} -> {} {}", path, response.statusCode(),
                    body == null ? "" : body.substring(0, Math.min(120, body.length())));
            return response.statusCode() == 200 ? body : null;
        } catch (Exception e) {
            log.warn("[HIK-ISAPI] 调用失败(不阻断业务): {} err={}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 拉取 AI 服务器/NVR 上的摄像机通道列表（ISAPI ContentMgmt InputProxy）。
     *
     * @return 设备返回的 XML 原文（含通道 id/名称/源 IP）；未启用/失败返回 null
     */
    public String fetchChannels() {
        return isapiGet("/ISAPI/ContentMgmt/InputProxy/channels");
    }

    /** 设备信息（/ISAPI/System/deviceInfo）——连通性自检用，XML 原文 */
    public String deviceInfo() {
        return isapiGet("/ISAPI/System/deviceInfo");
    }

    /**
     * 生成设备直连 RTSP 实时流地址（主码流 01 / 子码流 02）。
     * 浏览器不能直接播 RTSP——二期经流媒体代理转 HLS/HTTP-FLV；当前供 VLC/现场调试与点位校验。
     */
    public String rtspLiveUrl(String deviceIp, int rtspPort, int channelNo, boolean mainStream, String username, String password) {
        String cred = StrUtil.isNotBlank(username) ? username + ":" + (password == null ? "" : password) + "@" : "";
        return "rtsp://" + cred + deviceIp + ":" + rtspPort + "/Streaming/Channels/" + channelNo + (mainStream ? "01" : "02");
    }

    /**
     * 生成设备直连 RTSP 录像回放地址（按通道+时间段）。
     */
    public String rtspPlaybackUrl(String deviceIp, int rtspPort, int channelNo, String startTime, String endTime, String username, String password) {
        String live = rtspLiveUrl(deviceIp, rtspPort, channelNo, true, username, password);
        return live.replace("/Streaming/Channels/" + channelNo + "01",
                "/Streaming/tracks/" + channelNo + "01?starttime=" + startTime + "&endtime=" + endTime);
    }

    public String getIsapiUsername() {
        return isapiUsername;
    }

    public String getIsapiPassword() {
        return isapiPassword;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ===== 门禁 ISAPI 直连（K1T34x 人脸门禁一体机，无 iSecure Center）=====
    // 报价单 0914：人脸识别门禁机×1（7寸 200万双目 人脸/IC/NFC/CPU/密码）+ 电磁锁 DS-K4H + 闭门器 DS-K4DC104
    // 海康门禁 ISAPI（公开标准协议，Digest 认证）：
    //   人员下发 PUT /ISAPI/AccessControl/UserInfo/Record?format=json（employeeNo=工号，主键幂等可重复下发）
    //   卡下发   PUT /ISAPI/AccessControl/CardInfo/Record?format=json
    //   远程开门 PUT /ISAPI/AccessControl/RemoteControl/door/{n}（联调期连通性验证）
    // 单机模式人员下发即具备本地认证开门能力；权限组模板（计划/门组）按真机固件行为联调期适配

    @Value("${xuehua.hik.door.enabled:false}")
    private boolean doorEnabled;

    @Value("${xuehua.hik.door.base-url:}")
    private String doorBaseUrl;

    @Value("${xuehua.hik.door.username:admin}")
    private String doorUsername;

    @Value("${xuehua.hik.door.password:}")
    private String doorPassword;

    public boolean isDoorEnabled() {
        return doorEnabled && StrUtil.isNotBlank(doorBaseUrl);
    }

    /**
     * 门禁机直连 PUT/POST（Digest 认证，JSON）。
     *
     * @return 响应正文；未启用/失败返回 null
     */
    private String doorRequest(String method, String path, Map<String, Object> body) {
        if (!isDoorEnabled()) {
            log.info("[HIK-DOOR] 开关关闭，跳过门禁调用: {}", path);
            return null;
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .authenticator(new java.net.Authenticator() {
                        @Override
                        protected java.net.PasswordAuthentication getPasswordAuthentication() {
                            return new java.net.PasswordAuthentication(doorUsername, doorPassword.toCharArray());
                        }
                    })
                    .build();
            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(doorBaseUrl + path))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json");
            if ("GET".equals(method)) {
                rb.GET();
            } else {
                rb.method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body == null ? Map.of() : body)));
            }
            HttpResponse<String> response = client.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            String respBody = response.body();
            log.info("[HIK-DOOR] {} {} -> {} {}", method, path, response.statusCode(),
                    respBody == null ? "" : respBody.substring(0, Math.min(150, respBody.length())));
            return response.statusCode() == 200 ? respBody : null;
        } catch (Exception e) {
            log.warn("[HIK-DOOR] 调用失败: {} err={}", path, e.getMessage());
            return null;
        }
    }

    /** 海康 ISAPI JSON 应答惯例：statusCode==1 / statusString 含 OK 视为成功 */
    private static boolean isapiOk(String resp) {
        if (resp == null) {
            return false;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode n = JSON.readTree(resp);
            return n.path("statusCode").asInt(-1) == 1
                    || String.valueOf(n.path("statusString").asText("")).toUpperCase().contains("OK");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 门禁人员+卡下发（进仓授权，设备直连版）：
     * employeeNo=工号作人员主键（重复下发=更新，幂等），有卡号再补一张卡。
     * 人员下发成功即视为授权成功（单机本地认证模式）。
     */
    public boolean authorizeEntryByDoor(String empNo, String name, String cardNo) {
        if (!isDoorEnabled() || StrUtil.isBlank(empNo)) {
            return false;
        }
        Map<String, Object> userInfo = new HashMap<>(8);
        userInfo.put("employeeNo", empNo);
        userInfo.put("name", StrUtil.blankToDefault(name, empNo));
        userInfo.put("userType", "normal");
        Map<String, Object> body = new HashMap<>(4);
        body.put("UserInfo", userInfo);
        String resp = doorRequest("PUT", "/ISAPI/AccessControl/UserInfo/Record?format=json", body);
        if (!isapiOk(resp)) {
            return false;
        }
        if (StrUtil.isNotBlank(cardNo)) {
            Map<String, Object> card = new HashMap<>(8);
            card.put("employeeNo", empNo);
            card.put("cardNo", cardNo);
            doorRequest("PUT", "/ISAPI/AccessControl/CardInfo/Record?format=json", card);
            // 卡失败不判整体失败——人员已可人脸开门，卡联调期补（日志已留痕）
        }
        return true;
    }

    /** 远程开门（联调期连通性/应急放行验证）：PUT RemoteControl/door/{doorNo} */
    public boolean remoteOpen(int doorNo) {
        return isapiOk(doorRequest("PUT", "/ISAPI/AccessControl/RemoteControl/door/" + doorNo, Map.of()));
    }

    /**
     * 门禁进仓授权：人员档案在海康侧已存在（由海康推送注册信息给 WCS），WCS 只下发
     * "该人员可通行仓库门禁点"的授权指令（EMIS 推单校验通过后触发——用户口径 2026-09-18）。
     *
     * @return true=已下发；false=未启用/缺少海康人员ID/失败
     */
    public boolean authorizeEntry(String hikPersonId, String empNo, String cardNo, String zones) {
        if (!enabled || hikPersonId == null) {
            return false;
        }
        Map<String, Object> body = new HashMap<>(8);
        body.put("personIds", java.util.List.of(hikPersonId));
        body.put("resourceIndexes", java.util.List.of());
        return artemisPost("/artemis/api/accessControl/v1/personAuthority/add", body) != null;
    }

    /**
     * 按点位+时间段取录像回放地址（hls/rtsp 由海康平台决定）。
     *
     * @return 回放 URL；未启用/失败返回 null（前端展示"海康未接入"）
     */
    public String fetchPlaybackUrl(String cameraIndex, String startTime, String endTime) {
        if (!enabled) {
            log.info("[HIK] 开关关闭，跳过回放取流: {} {}~{}", cameraIndex, startTime, endTime);
            return null;
        }
        Map<String, Object> body = new HashMap<>(8);
        body.put("cameraIndexCode", cameraIndex);
        body.put("beginTime", startTime);
        body.put("endTime", endTime);
        body.put("protocol", "hls");
        String resp = artemisPost("/artemis/api/video/v2/cameras/playbackURLs", body);
        return resp == null ? null : extractString(resp, "url");
    }

    /**
     * Artemis 网关 POST。骨架实现：直连 + 简化鉴权 Header；
     * 联调期替换为 artemis-http-client 的 HMAC 签名（改这里一处，业务零感知）。
     */
    private String artemisPost(String path, Map<String, Object> body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .header("X-Ca-Key", appKey)
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            String resp = response.body();
            log.info("[HIK] {} -> {} {}", path, response.statusCode(), resp == null ? "" : resp.substring(0, Math.min(120, resp.length())));
            return response.statusCode() == 200 ? resp : null;
        } catch (Exception e) {
            log.warn("[HIK] 调用失败(不阻断业务): {} err={}", path, e.getMessage());
            return null;
        }
    }

    private static String extractString(String json, String key) {
        try {
            com.fasterxml.jackson.databind.JsonNode node = JSON.readTree(json);
            // 海康返回结构常见 {"code":"0","data":{key:...}}，data 层兼容
            com.fasterxml.jackson.databind.JsonNode data = node.has("data") ? node.get("data") : node;
            com.fasterxml.jackson.databind.JsonNode v = data.get(key);
            if (v == null || v.isNull()) {
                return null;
            }
            return v.isTextual() ? v.asText() : v.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
