package com.keytech.xuehua.service.impl;

import com.keytech.base.service.impl.BaseServiceImpl;
import com.keytech.xuehua.entity.XuehuaAccessLog;
import com.keytech.xuehua.hik.HikClient;
import com.keytech.xuehua.mapper.XuehuaAccessLogMapper;
import com.keytech.xuehua.service.IXuehuaAccessLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 进出流水 服务实现：回放取流 = 流水事件时间 ±30 秒窗口 + 流水上的海康点位编码
 */
@Service
public class XuehuaAccessLogServiceImpl extends BaseServiceImpl<XuehuaAccessLogMapper, XuehuaAccessLog> implements IXuehuaAccessLogService {

    @Resource
    private HikClient hikClient;

    @Override
    public String playbackUrl(Integer logId) {
        XuehuaAccessLog log1 = this.getById(logId);
        if (log1 == null || log1.getCameraIndex() == null) {
            return null;
        }
        long t = log1.getEventTime() == null ? System.currentTimeMillis() : log1.getEventTime().getTime();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS+08:00");
        return hikClient.fetchPlaybackUrl(log1.getCameraIndex(),
                fmt.format(new Date(t - 30_000L)), fmt.format(new Date(t + 30_000L)));
    }
}
