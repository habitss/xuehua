package com.keytech.xuehua.service;

import com.keytech.base.service.BaseService;
import com.keytech.xuehua.entity.XuehuaAccessLog;

/**
 * 进出流水 服务接口（查询为主；写入统一走 IXuehuaMonitorService.recordAccess）
 */
public interface IXuehuaAccessLogService extends BaseService<XuehuaAccessLog> {

    /**
     * 取回放地址：按流水的时间±30秒窗口调海康 playbackURLs；
     * hik.enabled=false 返回 null（前端展示"海康未接入，仅时间线"）
     */
    String playbackUrl(Integer logId);
}
