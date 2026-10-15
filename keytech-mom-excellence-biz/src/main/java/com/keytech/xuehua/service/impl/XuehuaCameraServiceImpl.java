package com.keytech.xuehua.service.impl;

import cn.hutool.core.util.StrUtil;
import com.keytech.base.service.impl.BaseServiceImpl;
import com.keytech.xuehua.entity.XuehuaCamera;
import com.keytech.xuehua.hik.HikClient;
import com.keytech.xuehua.mapper.XuehuaCameraMapper;
import com.keytech.xuehua.service.IXuehuaCameraService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频点位 服务实现
 *
 * <p>通道发现走 ISAPI（/ISAPI/ContentMgmt/InputProxy/channels，设备 Digest 认证）——
 * 设备到货联调时在 yml 打开 xuehua.hik.isapi 开关即可，业务零改动；
 * 未启用时返回友好提示（与海康对接整体降级口径一致）。
 * RTSP 地址生成：实时 /Streaming/Channels/{ch}01(02)、回放 /Streaming/tracks/{ch}01?starttime&endtime。
 */
@Slf4j
@Service
public class XuehuaCameraServiceImpl extends BaseServiceImpl<XuehuaCameraMapper, XuehuaCamera> implements IXuehuaCameraService {

    @Resource
    private HikClient hikClient;

    @Override
    public Map<String, Object> discoverChannels() {
        Map<String, Object> result = new LinkedHashMap<>(4);
        String xml = hikClient.fetchChannels();
        if (xml == null) {
            result.put("success", false);
            result.put("message", "视频AI服务器未接入（xuehua.hik.isapi.enabled=false 或设备不可达），请先在 yml 配置设备地址与凭据");
            return result;
        }
        List<Map<String, Object>> channels = new ArrayList<>();
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            NodeList list = doc.getElementsByTagName("InputProxyChannel");
            for (int i = 0; i < list.getLength(); i++) {
                Element ch = (Element) list.item(i);
                Map<String, Object> item = new LinkedHashMap<>(8);
                item.put("id", text(ch, "id"));
                item.put("name", text(ch, "name"));
                item.put("sourceIp", text(ch, "address"));
                channels.add(item);
            }
        } catch (Exception e) {
            log.warn("[CAMERA] 解析通道列表失败: {}", e.getMessage());
        }
        result.put("success", true);
        result.put("channels", channels);
        result.put("raw", xml.length() > 2000 ? xml.substring(0, 2000) : xml);
        return result;
    }

    @Override
    public Map<String, Object> previewUrls(Integer id, String startTime, String endTime) {
        Map<String, Object> result = new LinkedHashMap<>(8);
        XuehuaCamera c = this.getById(id);
        if (c == null) {
            result.put("success", false);
            result.put("message", "点位不存在");
            return result;
        }
        if (StrUtil.isBlank(c.getDeviceIp()) || c.getChannelNo() == null) {
            result.put("success", false);
            result.put("message", "点位未配置设备IP或通道号");
            return result;
        }
        boolean live = hikClient.isIsapiEnabled();
        String user = live ? hikClient.getIsapiUsername() : "";
        String pwd = live ? hikClient.getIsapiPassword() : "";
        int port = c.getRtspPort() == null ? 554 : c.getRtspPort();
        result.put("success", true);
        result.put("live", hikClient.rtspLiveUrl(c.getDeviceIp(), port, c.getChannelNo(), true, user, pwd));
        result.put("liveSub", hikClient.rtspLiveUrl(c.getDeviceIp(), port, c.getChannelNo(), false, user, pwd));
        if (StrUtil.isNotBlank(startTime) && StrUtil.isNotBlank(endTime)) {
            result.put("playback", hikClient.rtspPlaybackUrl(c.getDeviceIp(), port, c.getChannelNo(), startTime, endTime, user, pwd));
        }
        if (!live) {
            result.put("message", "ISAPI 未启用：地址不含设备凭据，仅用于点位校验；现场可用 VLC 替换凭据测试，浏览器内播放待流媒体代理（二期）");
        }
        return result;
    }

    private static String text(Element parent, String tag) {
        NodeList n = parent.getElementsByTagName(tag);
        return n.getLength() > 0 ? n.item(0).getTextContent().trim() : "";
    }
}
