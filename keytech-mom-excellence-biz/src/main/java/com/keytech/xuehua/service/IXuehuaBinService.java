package com.keytech.xuehua.service;

import com.keytech.base.service.BaseService;
import com.keytech.xuehua.entity.XuehuaBin;

import java.util.List;
import java.util.Map;

/**
 * @Description: 库位 服务接口
 */
public interface IXuehuaBinService extends BaseService<XuehuaBin> {

    /**
     * 库位地图数据（全量行 + 备件名称/安全库存 + 低库存预警装饰）。
     * 预警口径：在库（status=1）、有备件归属、qty>0 且 qty<=安全库存；
     * 安全库存取备件主数据 safe_stock，未设置时兜底字典 xuehua_safe_stock"默认安全库存"。
     */
    List<Map<String, Object>> mapRows();

    /**
     * 安全库存兜底阈值：字典 xuehua_safe_stock"默认安全库存"，无配置回退常量（工作台/地图共用口径）
     */
    int fallbackSafeStock();
}
