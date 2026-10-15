package com.keytech.xuehua.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 工单中心 服务接口（说明书 3.5.1：出入库/盘点作业工单集中管理与统计）
 *
 * <p>只读聚合页（无新表）：入库单 + 出库工单 + 盘点任务三域行拉平，
 * 统一列（工单号/类型/内容/数量/执行人/时间/状态），时间倒序。
 * 状态文案沿用各域口径（登记域 0/1、出库 0/1/2、盘点 0-4）。
 * 纯聚合无自有实体，故不继承 BaseService。
 */
public interface IXuehuaWorkorderService {

    /**
     * 工单聚合列表（已按时间倒序）
     *
     * @param keyword 工单号/内容/执行人/状态 模糊
     * @param woType  入库/出库/盘点，null=全部
     */
    List<Map<String, Object>> workorderRows(String keyword, String woType, Date from, Date to);

    /**
     * 按类型统计工单数量与完成情况：
     * {ib:{total,done}, ob:{total,done}, pv:{total,done}, total, done}
     * （入库完成=已登记；出库完成=已出库；盘点完成=已完成）
     */
    Map<String, Object> stat();
}
