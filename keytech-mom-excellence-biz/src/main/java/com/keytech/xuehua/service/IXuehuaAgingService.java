package com.keytech.xuehua.service;

import com.keytech.base.service.BaseService;
import com.keytech.xuehua.entity.XuehuaDisposal;

import java.util.List;
import java.util.Map;

/**
 * 库龄分析 服务接口（说明书 3.5.2：库龄分布/超期预警/分类分析 + 处置审批流）
 *
 * <p>库龄口径（用户拍板）：库位级最早一次确认上架距今（MIN putaway_time，
 * 与出库 FIFO 完全同源）；>90 超期 / 60-90 偏高 / <60 正常；分布五档
 * <30/30-60/60-90/90-180/>180（件数=qty 求和）。
 */
public interface IXuehuaAgingService extends BaseService<XuehuaDisposal> {

    /**
     * 在库库龄明细（已装饰 agingDays/status；keyword/status 筛选）
     *
     * @param keyword 关键字（编码/名称/库位/类别模糊）
     * @param status  库龄状态 0正常/1偏高/2超期，null=全部
     */
    List<Map<String, Object>> agingRows(String keyword, Integer status);

    /**
     * 统计聚合：KPI（在库件数/超期件数/超期库位/最高库龄）
     * + 五档分布（件数/库位数，<30/30-60/60-90/90-180/>180 补零）
     * + 分类分析（类别/在库件数/超期件数）
     */
    Map<String, Object> stat();

    /**
     * 处置申请列表（binCode 可选，pendingOnly 只看待审批）
     */
    List<XuehuaDisposal> disposalList(String binCode, boolean pendingOnly);

    /**
     * 发起处置申请（超期库位）：落 0待审批。
     * 同库位已有待审批时拒绝（防重复发起）；申请人取 body.applicant，空则记"管理员"
     *
     * @throws IllegalStateException 库位/方式/理由缺失或已有待审批申请
     */
    void applyDisposal(XuehuaDisposal body);

    /**
     * 处置审批（通过/驳回）：条件更新 WHERE status=0 防并发双审；审批人记"库房主管"
     * （一期无审批岗账号，二期接入后改为登录人）
     *
     * @throws IllegalStateException 申请不存在或已审批过
     */
    void approveDisposal(Integer id, boolean pass, String note);
}
