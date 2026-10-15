package com.keytech.xuehua.service;

import java.util.Map;

/**
 * @Description: PTL 设备事件回调 服务接口（《PTL接口文档》设备事件触发的接口）
 */
public interface IXuehuaPtlEventService {

    /**
     * 常规标签按钮按下触发：btName=OKKey 时按单号前缀路由——
     * IN- 放置确认（上架完成熄灯），WO- 拣配确认（出库完成熄灯）；
     * 返回报文遵循接口文档 {code,msg,orderNo,tagAdd,ledColor,ledState,str}。
     * 应答语义：处理成功/幂等重放（重复按键、并发输家）返回 code=200；
     * 真实处理失败返回 code=500 如实暴露——处理逻辑幂等，网关若重试是安全的
     * （文档未定义网关重试策略，现场联调时与厂商确认）。每次回调落审计表。
     *
     * @param event    网关报文 {orderNo,tagAdd,str,btName}
     * @param clientIp 来源IP（审计用）
     */
    Map<String, Object> handleTagEvent(Map<String, Object> event, String clientIp);
}
