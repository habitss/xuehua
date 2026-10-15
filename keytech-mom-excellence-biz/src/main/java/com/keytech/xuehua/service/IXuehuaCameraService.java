package com.keytech.xuehua.service;

import com.keytech.base.service.BaseService;
import com.keytech.xuehua.entity.XuehuaCamera;

import java.util.List;
import java.util.Map;

/**
 * 视频点位 服务接口（库区/货架/库位 ↔ 海康设备通道 绑定）
 */
public interface IXuehuaCameraService extends BaseService<XuehuaCamera> {

    /**
     * 从 AI 服务器/NVR 拉取摄像机通道列表（ISAPI InputProxy channels）。
     *
     * @return channels: [{id, name, sourceIp}]；未启用/失败时 success=false + message
     */
    Map<String, Object> discoverChannels();

    /**
     * 生成点位的 RTSP 地址（实时主/子码流 + 回放）。
     *
     * @return {live, liveSub, playback}；未启用/点位不完整时 message 说明
     */
    Map<String, Object> previewUrls(Integer id, String startTime, String endTime);
}
