package com.keytech.xuehua.service;

import com.keytech.base.service.BaseService;
import com.keytech.xuehua.entity.XuehuaInventory;
import com.keytech.xuehua.entity.XuehuaInventoryDetail;

import java.util.List;
import java.util.Map;

/**
 * 盘点单 服务接口
 *
 * <p>库位级真盘点流程（2026-09-11 升级）：
 * addInventory 建单 → start 开始(可 freeze 冻结库存拦截出入库)
 * → generateDetails 生成库位明细(账面快照) → submitReal 逐行录实盘
 * → applyDifferences 差异处置(差额原子调整 bin.qty、解冻、完成)。
 * 原 dispose(单据级账面/实盘一对数)保留兼容旧页面。
 */
public interface IXuehuaInventoryService extends BaseService<XuehuaInventory> {

    /**
     * 各状态数量统计（供流程概览卡）
     *
     * @return key: s0待盘点/s1盘点中/s2有差异/s3已完成/s4已终止/total
     */
    Map<String, Object> stat();

    /**
     * 新建盘点任务：自动生成盘点单号、默认值
     */
    boolean addInventory(XuehuaInventory entity);

    /**
     * 是否存在冻结中的盘点（冻结期间出入库提交被拦截）
     */
    boolean isFrozen();

    /**
     * 开始盘点：待盘点 -> 盘点中
     */
    boolean start(Integer id);

    /**
     * 推进盘点阶段：stage+1 并刷新进度，到末阶段自动完成；期末阶段实盘数与账面数不一致置为有差异
     */
    boolean advance(Integer id);

    /**
     * 冻结库存（盘点中）
     */
    boolean freeze(Integer id);

    /**
     * 差异处置：完成盘点并解冻
     */
    boolean dispose(Integer id, Integer realQty);

    /**
     * 终止盘点并解冻
     */
    boolean stop(Integer id);

    /**
     * 生成库位级盘点明细：对范围内在库库位快照账面数（重复调用重新生成未应用部分）
     *
     * @throws IllegalStateException 非【盘点中】状态
     */
    List<XuehuaInventoryDetail> generateDetails(Integer id);

    /**
     * 录入某明细行的实盘数（算差异，不动台账）
     *
     * @throws IllegalStateException 实盘数非法 / 明细已处置
     */
    boolean submitReal(Integer detailId, Integer realQty);

    /**
     * 差异处置：所有已录实盘明细的差额原子调整 bin.qty，盘点单转【已完成】并解冻。
     * 任一明细未录实盘或台账调整失败则整体回滚
     */
    boolean applyDifferences(Integer id);

    /**
     * 查询盘点单的库位明细列表
     */
    List<XuehuaInventoryDetail> listDetails(Integer id);

    /**
     * EMIS 盘点任务下发接收（匿名端点）：externalNo 幂等判重，落 0待盘点；
     * executor 盘点人须已注册（与入库/出库同口径——未注册拒绝）
     *
     * @return 落库后的盘点单（含生成的 PV 单号）
     * @throws IllegalStateException 参数缺失 / 盘点人未注册 / 重复下发幂等由调用方返回原单
     */
    XuehuaInventory receiveFromEmis(XuehuaInventory body, String executor);
}
