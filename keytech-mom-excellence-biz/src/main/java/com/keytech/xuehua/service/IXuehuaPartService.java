package com.keytech.xuehua.service;

import com.keytech.base.service.BaseService;
import com.keytech.xuehua.entity.XuehuaPart;

/**
 * @Description: 备件主数据 服务接口
 */
public interface IXuehuaPartService extends BaseService<XuehuaPart> {

    /**
     * EMIS 备件主数据同步（说明书 5.1：备件主数据从 Emis 同步）：
     * partCode 为幂等键——已存在则更新主数据字段（保留本地 safe_stock），新编码插入。
     *
     * @return 落库实体（isUpdate 由调用方返回给 EMIS）
     */
    XuehuaPart receiveFromEmis(XuehuaPart body, String externalNo);
}
