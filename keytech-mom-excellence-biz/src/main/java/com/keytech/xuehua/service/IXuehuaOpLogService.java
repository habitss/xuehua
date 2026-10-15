package com.keytech.xuehua.service;

import com.keytech.base.service.BaseService;
import com.keytech.xuehua.entity.XuehuaOpLog;

/**
 * @Description: 操作日志（审计） 服务接口
 */
public interface IXuehuaOpLogService extends BaseService<XuehuaOpLog> {

    /**
     * 记录一条审计日志：任何异常吞掉只记 warn，绝不影响主业务
     */
    void record(String bizType, String bizNo, String opAction, String opUser,
                String opIp, String reqBody, String respBody, boolean success, String errorMsg);
}
