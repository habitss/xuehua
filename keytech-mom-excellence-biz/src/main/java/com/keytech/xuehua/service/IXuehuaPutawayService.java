package com.keytech.xuehua.service;

import com.keytech.base.service.BaseService;
import com.keytech.xuehua.entity.XuehuaPutaway;

import java.util.Map;

/**
 * @Description: 上架任务 服务接口
 */
public interface IXuehuaPutawayService extends BaseService<XuehuaPutaway> {

    /**
     * 各状态数量统计（供流程概览卡）
     *
     * @return key: s0待上架/s1已上架/total
     */
    Map<String, Object> stat();

    /**
     * 确认上架（PTL 放置完成后回调）：待上架 -> 已上架，记录上架人/时间，并更新库位状态为在库
     */
    boolean confirmPutaway(Integer id);

    /**
     * 确认上架（可记录操作人来源：页面确认传登录人，PTL 按键回调传 PTL按键）
     */
    boolean confirmPutaway(Integer id, String operator);

    /**
     * 按入库单号确认上架（PTL 标签 OKKey 回调用）：无待上架任务返回 false（幂等，不抛异常）
     */
    boolean confirmPutawayByInboundNo(String inboundNo, String operator);

    /**
     * 按标签地址确认上架（回调未回带单号时的兜底）：tag_add→货位→该货位待上架任务；
     * 无对应任务返回 false（幂等，不抛异常）
     */
    boolean confirmPutawayByTagAdd(Integer tagAdd, String operator);
}
