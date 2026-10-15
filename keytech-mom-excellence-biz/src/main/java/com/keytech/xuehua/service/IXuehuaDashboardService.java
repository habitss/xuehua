package com.keytech.xuehua.service;

import java.util.Map;

/**
 * 工作台（运营驾驶舱）聚合查询服务
 *
 * <p>一次 overview() 返回页面全部区块数据：KPI/今日待办/备件分类/出入库趋势/库龄结构/
 * 分区占用/实时出入库/库存预警。纯只读聚合，无副作用。
 */
public interface IXuehuaDashboardService {

    /**
     * 工作台总览数据
     *
     * @return kpi{workOrders,inbound,outbound,partKinds,overAged}
     *         todos{toPutaway,toPick,toInventory,diffInventory,lowStock}
     *         catDonut[{n,v}] / trend[{ym,cin,cout}] / aging[{n,v}]
     *         zones[{zone,total,inStock,pct}] / liveFeed[6条] / alarms{errCount,warnCount,items[]}
     */
    Map<String, Object> overview();
}
