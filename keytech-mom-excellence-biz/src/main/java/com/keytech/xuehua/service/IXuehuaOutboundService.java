package com.keytech.xuehua.service;

import com.keytech.base.service.BaseService;
import com.keytech.xuehua.entity.XuehuaOutbound;
import com.keytech.xuehua.entity.XuehuaOutboundDetail;

import java.util.List;
import java.util.Map;

/**
 * 出库工单 服务接口
 *
 * <p>2026-09-13 升级为"自动分配 + 行级拣配"模型（用户口径）：
 * 领料单（EMIS 推送 /xuehua/outbound/receive 或页面申请）只带 备件+数量+人员，不填库位；
 * WCS 按库龄优先 FIFO 自动分配拣配行（mes4_xuehua_outbound_detail，可能多库位）：
 * 库龄 = 该库位备件最早一次确认上架时间（MIN putaway_time），早的先拣；总量不足整单拒绝。
 * 头表状态机不变：0 待拣配 →(首行确认)1 拣配中 →(全部行完成)2 已出库。
 *
 * <p>两条确认入口，最终都收敛到 confirmPickLine（行级）：
 * <ul>
 *   <li>页面/接口测试 → Controller /confirmPick → confirmPick(id)（一按确认最早待拣行）；
 *       业务拒绝以 IllegalStateException 抛出，Controller 转 Result.error；</li>
 *   <li>PTL 按键回调 → confirmPickByWoNo(带 tagAdd 定位行) / confirmPickByTagAdd；
 *       无待拣行返回 null（幂等语义，回调层按"无进行中任务"应答 200，网关重试安全）。</li>
 * </ul>
 */
public interface IXuehuaOutboundService extends BaseService<XuehuaOutbound> {

    /**
     * 各状态数量统计（供流程概览卡，头表口径：一张多库位单仍算一张）
     *
     * @return key: s0 待拣配 / s1 拣配中 / s2 已出库 / total
     */
    Map<String, Object> stat();

    /**
     * 新增领用申请（页面/EMIS 共用入口）：
     * 盘点冻结拦截 → 咨询锁 + WO- 单号生成 → FIFO 分配拣配行（不填库位）→ 落头表+行表
     * → 事务提交后点亮首行库位 PTL 标签（蓝闪+首行数量+该库位现存量）
     *
     * @return 生成的拣配行（seq 升序），receive 端点据此回显分配结果
     * @throws IllegalStateException 盘点冻结 / 备件或数量非法 / 全库总量不足 / 保存失败
     */
    List<XuehuaOutboundDetail> addOutbound(XuehuaOutbound entity);

    /**
     * 行级确认拣配（唯一确认实现）：
     * 占行（WHERE status=0 条件更新，并发双击恰一个胜者）→ 该行库位台账原子扣减
     * （WHERE qty>=n，0 行=库存不足抛异常整体回滚，防负库存）→ 头表推进
     * （全部行完成置 2 已出库，否则置 1 拣配中）→ 熄灭该行库位标签（主动通道，
     * 链式下一行由回调应答再点亮）
     *
     * @return 确认成功的行；null=行不存在或已被确认（幂等重放，不抛异常）
     * @throws IllegalStateException 库位库存不足
     */
    XuehuaOutboundDetail confirmPickLine(Integer lineId, String operator);

    /**
     * 确认该工单最早待拣行（页面按钮语义：一按一行）
     *
     * @throws IllegalStateException 工单不存在 / 无待拣行（已出库，重复确认）
     */
    XuehuaOutboundDetail confirmPick(Integer id);

    /**
     * 确认该工单最早待拣行（带操作人）
     *
     * @param operator 页面确认传登录用户，PTL 按键回调传"PTL按键"
     * @throws IllegalStateException 工单不存在 / 无待拣行
     */
    XuehuaOutboundDetail confirmPick(Integer id, String operator);

    /**
     * PTL OKKey 标准路径：优先按 tagAdd 定位"该单在该库位"的待拣行
     * （一张单拆多库位时，按键的标签决定确认哪一行），兜底取该单最早待拣行
     *
     * @return 确认的行；null=该单无待拣行（已完成或不存在，幂等，不抛异常）
     */
    XuehuaOutboundDetail confirmPickByWoNo(String woNo, Integer tagAdd, String operator);

    /**
     * tagAdd 兜底路径：标签→库位→该库位最早待拣行（跨工单，按行创建顺序）。
     * 适用：回调未回带单号（清屏后盲按）等场景
     *
     * @return 确认的行；null=标签未绑库位 / 该库位无待拣行
     */
    XuehuaOutboundDetail confirmPickByTagAdd(Integer tagAdd, String operator);

    /**
     * 该工单下一张待拣行（seq 升序）——跨库位链式下发：拣完 seq=1 的库位，
     * 回调应答直接点亮 seq=2 的库位标签
     */
    XuehuaOutboundDetail findNextLineByWoNo(String woNo);

    /**
     * 该库位下一张待拣行（跨工单，按行创建顺序）——链式第二优先级：
     * 当前工单拣完后，看刚拣过的库位上是否还压着其他工单的待拣行
     */
    XuehuaOutboundDetail findNextLineByBinCode(String binCode);

    /**
     * 工单的拣配行列表（seq 升序）——前端行展开/拣配指引弹窗
     */
    List<XuehuaOutboundDetail> listLinesByWoNo(String woNo);

    /**
     * 按工单号查工单（任意状态，取最新一条）
     */
    XuehuaOutbound findByWoNo(String woNo);
}
