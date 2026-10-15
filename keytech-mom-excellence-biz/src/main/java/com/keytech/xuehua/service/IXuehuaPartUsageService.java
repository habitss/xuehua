package com.keytech.xuehua.service;

import com.keytech.base.service.BaseService;
import com.keytech.xuehua.entity.XuehuaPartUsage;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 备件使用报表 服务接口（说明书 3.5.3：按类别/产线维度跟踪出库备件使用情况）
 *
 * <p>状态机：0待确认 → 1已使用（实际用量+照片+说明）/ 2未使用；
 * 派生态"超1周未使用" = status != 1 且出库超 7 天（不落库，列表装饰时计算）。
 */
public interface IXuehuaPartUsageService extends BaseService<XuehuaPartUsage> {

    /**
     * 使用记录列表（自动幂等回填已拣配明细的缺失记录后查询）。
     * 装饰 days（出库天数）/statusText（待确认/已使用/未使用/超1周未使用）。
     *
     * @param keyword 备件编码/名称/类别/产线/领用人/工单号 模糊
     * @param status  0待确认/1已使用/2未使用/"over"超1周未使用，null=全部
     */
    List<Map<String, Object>> usageRows(String keyword, String status, Date from, Date to);

    /**
     * 确认用途（条件更新 WHERE status=0 防并发双确认）。
     *
     * @param used true=已使用（usedQty 必填 1..qty）；false=登记未使用
     * @throws IllegalStateException 记录不存在或已确认过
     */
    void confirm(Integer id, boolean used, Integer usedQty, String photos, String purpose, String operator);

    /**
     * 提醒确认（remind_count+1）：只对未确认行（0待确认/2未使用）生效
     *
     * @return 实际提醒行数
     */
    int remind(List<Integer> ids);

    /** 一键提醒：全部未确认行（0/2）批量提醒 */
    int remindAll();
}
