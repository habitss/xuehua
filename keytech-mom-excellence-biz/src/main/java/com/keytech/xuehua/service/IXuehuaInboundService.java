package com.keytech.xuehua.service;

import com.keytech.base.service.BaseService;
import com.keytech.xuehua.entity.XuehuaInbound;

import java.util.Map;

/**
 * @Description:  入库单 服务接口（登记域：0待登记 1已登记）
 */
public interface IXuehuaInboundService extends BaseService<XuehuaInbound> {

    /**
     * 各状态数量统计（供流程概览卡）
     *
     * @return key: s0待登记/s1已登记/total
     */
    Map<String, Object> stat();

    /**
     * 新增入库登记：三要素（送货单/全景照片/检验报告）必填，成功即【已登记】并自动生成上架任务、分配库位；
     * 盘点冻结期间抛异常拦截
     */
    boolean addInbound(XuehuaInbound entity);

    /**
     * EMIS 到货单同步（匿名端点用，externalNo 幂等由唯一索引兜底）：
     * 操作人必须已注册（人员注册模块），落库为 status=0 待登记（无三要素）；
     * 现场补齐三要素后走 register 完成正式登记（分配库位+亮灯）
     *
     * @param operatorName EMIS 推送的操作人（工号或姓名）
     * @return 落库后的到货单（含生成的入库单号）
     * @throws IllegalStateException 操作人未注册 / 冻结中 / 字段非法
     */
    XuehuaInbound receiveArrival(XuehuaInbound entity, String operatorName);

    /**
     * 到货单补登记：补齐三要素（送货单/全景照片/检验报告）后 0待登记 → 1已登记，
     * 并执行分配库位 + 生成上架任务 + afterCommit 亮灯
     *
     * @throws IllegalStateException 不存在 / 已登记 / 冻结中 / 三要素缺失
     */
    boolean register(Integer id, XuehuaInbound patch);

    /**
     * 按入库单号查单据（任意状态，取最新一条）——PTL 回调时间线/回传汇总用
     */
    XuehuaInbound findByInboundNo(String inboundNo);

    /**
     * 扫码预填：扫备件条码返回主数据供表单回填；扫送货单号返回已登记单号（查重提醒）；未识别抛异常
     */
    Map<String, Object> scanFill(String code);
}
