package com.keytech.xuehua.service;

import com.keytech.base.service.BaseService;
import com.keytech.xuehua.entity.XuehuaPerson;

import java.util.Map;

/**
 * 人员注册 服务接口
 *
 * <p>数据流向（2026-09-18 用户纠正）：人员档案源头在<b>海康</b>（人脸/卡在他们的平台采集），
 * 海康把注册信息推给 WCS（/xuehua/hik/person，upsert 记录）；WCS 不向海康推送人员档案。
 * EMIS 推单时 WCS 查询操作人是否已注册，已注册则调海康发"进仓授权"指令（失败拒单，
 * hik.enabled=false 降级跳过）。
 */
public interface IXuehuaPersonService extends BaseService<XuehuaPerson> {

    /**
     * 海康推送人员注册信息（upsert 幂等，按工号）：存在则更新海康侧字段，不存在则插入。
     * WCS 只记录，不产生任何对外调用
     *
     * @param body {empNo(必), name(必), dept?, cardNo?, authMethod?, faceStatus?(1/0/2), hikPersonId, zones?}
     * @return 落库后的人员
     * @throws IllegalStateException 工号/姓名缺失
     */
    XuehuaPerson upsertFromHik(Map<String, Object> body);

    /**
     * 发"进仓授权"指令给海康（EMIS 推单校验通过后调用，须在落库事务之前）：
     * 成功写 hik_auth_status=1；失败写 2 并抛异常（调用方拒单）；
     * hik.enabled=false 降级：跳过调用，保持待授权，单据照常接收
     *
     * @throws IllegalStateException 海康授权失败（严格模式拒单）
     */
    void authorizeEntry(XuehuaPerson person);

    /** 门禁远程开门（联调期连通性验证）：转发 HikClient.remoteOpen */
    boolean hikRemoteOpen(Integer doorNo);

    /**
     * 按"工号或姓名"解析注册人员（EMIS 到货单/领料单的操作人校验入口）：
     * 先精确匹配工号，再匹配启用状态的姓名；未匹配返回 null
     */
    XuehuaPerson resolveByOperator(String nameOrEmpNo);

    /**
     * 启用/停用（WCS 侧作业许可开关；海康侧档案仍以海康为准）
     */
    void toggleStatus(Integer id, int status);
}
