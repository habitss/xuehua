package com.keytech.xuehua.service;

import com.keytech.base.service.BaseService;
import com.keytech.xuehua.entity.XuehuaAccessLog;
import com.keytech.xuehua.entity.XuehuaPerson;

import java.util.Date;
import java.util.Map;

/**
 * 监控管理 聚合服务：进出时间线记录 + 海康事件处理 + EMIS 完成回传
 *
 * <p>时间线闭环（用户口径）：进仓 → 放置/拣配确认 → 出仓，全部落
 * mes4_xuehua_access_log（海康 7×24 自录，WCS 只记时间线+点位，回放按需取流）。
 */
public interface IXuehuaMonitorService {

    /**
     * 记录一条时间线事件（不抛异常——监控旁路失败不阻断业务）
     *
     * @param person 注册人员（可空：非授权事件无档案）
     * @param fallbackName person 为空时的展示名（如卡号/抓拍占位）
     */
    void recordAccess(XuehuaPerson person, String fallbackName, int eventType, String zone,
                      String method, String source, String direction, String bizNo,
                      String cameraIndex, Date eventTime);

    /**
     * 处理海康门禁事件（/xuehua/hik/event）：
     * 按 emp_no/card_no 匹配注册人员 → 匹配到写进仓/出仓流水（biz_no 关联该人在途单据），
     * 未匹配写"非授权进入"。事件报文契约我方定义，海康联调期适配解析。
     *
     * @return 处理摘要（code/msg/person）
     */
    Map<String, Object> handleHikEvent(String direction, String empNo, String cardNo,
                                       String personName, String zone, String cameraIndex, Date eventTime);

    /**
     * 入库完成回传 EMIS：单据已登记 + 上架任务全部完成时由确认链路触发；
     * 汇总 = 单据 + 拣配行 + 操作人 + 进出仓时间线。推送失败记 op_log 可重推。
     */
    void notifyInboundDone(String inboundNo);

    /**
     * 出库完成回传 EMIS：全部拣配行完成时触发，结构同入库
     */
    void notifyOutboundDone(String woNo);

    /**
     * 盘点完成回传 EMIS（说明书 5.1 盘点结果实时回传）：差异处置完成时触发，
     * 汇总 = 盘点单 + 差异明细行；推送失败记 op_log 可重推，不阻断盘点完成
     */
    void notifyInventoryDone(String pvNo);

    /**
     * 失败回传手动重推：按 op_log 留痕的 req_body 重发（三向回传统一入口）。
     * 仅允许重推 biz_type=EMIS_PUSH 且 success=0 的记录；成功后回写 success=1。
     *
     * @return 重推结果消息
     * @throws IllegalStateException 留痕不存在 / 已成功（无需重推）/ 历史记录无请求体
     */
    String repush(Integer opLogId);

    /**
     * 事务提交后再执行通知（回传/外呼统一入口）：在事务内调用时注册 afterCommit——
     * ① 锁已释放，不在行锁持有期间做网络调用；② 只在业务真正提交成功后才推送，
     * 消灭"先推 EMIS 后本地回滚"的假完成窗口。无事务上下文（理论不该发生）时直接执行兜底。
     */
    void notifyAfterCommit(Runnable notifyTask);
}
