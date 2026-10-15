package com.keytech.xuehua.service.impl;

import com.keytech.base.service.impl.BaseServiceImpl;
import com.keytech.xuehua.entity.XuehuaOpLog;
import com.keytech.xuehua.mapper.XuehuaOpLogMapper;
import com.keytech.xuehua.service.IXuehuaOpLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @Description: 操作日志（审计） 服务实现类
 */
@Slf4j
@Service
public class XuehuaOpLogServiceImpl extends BaseServiceImpl<XuehuaOpLogMapper, XuehuaOpLog> implements IXuehuaOpLogService {

    @Override
    public void record(String bizType, String bizNo, String opAction, String opUser,
                       String opIp, String reqBody, String respBody, boolean success, String errorMsg) {
        try {
            XuehuaOpLog opLog = new XuehuaOpLog()
                    .setBizType(bizType)
                    .setBizNo(bizNo)
                    .setOpAction(opAction)
                    .setOpUser(opUser)
                    .setOpIp(opIp)
                    .setReqBody(reqBody)
                    .setRespBody(respBody)
                    .setSuccess(success ? 1 : 0)
                    .setErrorMsg(errorMsg)
                    .setCreateBy(opUser)
                    .setCreateTime(new Date());
            this.save(opLog);
        } catch (Exception e) {
            // 审计日志失败不阻断业务
            log.warn("[操作日志] 记录失败: bizNo={} err={}", bizNo, e.getMessage());
        }
    }
}
