import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

enum Api {
  list = '/xuehua/inventory/listData',
  save = '/xuehua/inventory/add',
  edit = '/xuehua/inventory/edit',
  get = '/xuehua/inventory/queryById',
  delete = '/xuehua/inventory/deleteData',
  deleteBatch = '/xuehua/inventory/deleteDataBatch',
  importExcel = '/xuehua/inventory/importExcel',
  exportXls = '/xuehua/inventory/exportXls',
  stat = '/xuehua/inventory/stat',
  start = '/xuehua/inventory/start',
  advance = '/xuehua/inventory/advance',
  freeze = '/xuehua/inventory/freeze',
  dispose = '/xuehua/inventory/dispose',
  stop = '/xuehua/inventory/stop',
  generateDetails = '/xuehua/inventory/generateDetails',
  details = '/xuehua/inventory/details',
  submitReal = '/xuehua/inventory/submitReal',
  applyDifferences = '/xuehua/inventory/applyDifferences',
}
/**
 * 导出api
 */
export const getExportUrl = Api.exportXls;

export const getImportUrl = Api.importExcel;
/**
 * 查询列表
 * @param params
 */
export const getInventoryList = (params) => {
  return defHttp.get({ url: Api.list, params });
};

/**
 * 保存或者更新
 * @param params
 * @param isUpdate
 */
export const saveOrUpdateInventory = (params, isUpdate) => {
  const url = isUpdate ? Api.edit : Api.save;
  return isUpdate ? defHttp.put({ url, params }) : defHttp.post({ url, params });
};

/**
 * 查询详情
 * @param params
 */
export const getInventoryById = (params) => {
  return defHttp.get({ url: Api.get, params });
};

/**
 * 单条删除
 * @param params
 */
export const deleteInventory = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.delete, params: { id: params.id } }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};

/**
 * 批量删除
 * @param params
 */
export const batchDeleteInventory = (params, handleSuccess) => {
  Modal.confirm({
    title: '确认删除',
    content: '是否删除选中数据',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      const ids = Array.isArray(params.ids) ? params.ids.join(',') : params.ids;
      return defHttp.delete({ url: Api.deleteBatch, params: { ids } }, { joinParamsToUrl: true }).then(() => {
        handleSuccess();
      });
    },
  });
};

/**
 * 各状态数量统计（流程概览卡）
 */
export const getInventoryStat = () => {
  return defHttp.get({ url: Api.stat });
};

/**
 * 开始盘点
 */
export const startInventory = (id) => {
  return defHttp.post({ url: Api.start, params: { id } }, { joinParamsToUrl: true });
};

/**
 * 推进盘点阶段
 */
export const advanceInventory = (id) => {
  return defHttp.post({ url: Api.advance, params: { id } }, { joinParamsToUrl: true });
};

/**
 * 冻结库存
 */
export const freezeInventory = (id) => {
  return defHttp.post({ url: Api.freeze, params: { id } }, { joinParamsToUrl: true });
};

/**
 * 差异处置（可录入实盘数），完成并解冻
 */
export const disposeInventory = (id, realQty?) => {
  const params: Record<string, any> = { id };
  if (realQty !== undefined && realQty !== null) {
    params.realQty = realQty;
  }
  return defHttp.post({ url: Api.dispose, params }, { joinParamsToUrl: true });
};

/**
 * 终止盘点
 */
export const stopInventory = (id) => {
  return defHttp.post({ url: Api.stop, params: { id } }, { joinParamsToUrl: true });
};

// ===== 库位级盘点明细（2026-10-15 补 UI）：生成账面快照 → 逐行录实盘 → 差异处置 =====

/** 生成库位明细（按在库库位逐行快照账面数；幂等可重入，已生成返回现有明细） */
export const generateDetails = (id: number) =>
  defHttp.post({ url: Api.generateDetails, params: { id } }, { joinParamsToUrl: true });

/** 库位明细列表（库位/备件/账面/实盘/差额/处置状态） */
export const getDetails = (id: number) => defHttp.get({ url: Api.details, params: { id } });

/** 录入某库位实盘数（只记数不动台账；重复录=覆盖） */
export const submitReal = (detailId: number, realQty: number) =>
  defHttp.post({ url: Api.submitReal, params: { detailId, realQty } }, { joinParamsToUrl: true });

/** 差异处置：全部已录行的差额原子入账 bin.qty + 完成盘点 + 解冻 + 回传 EMIS */
export const applyDifferences = (id: number) =>
  defHttp.post({ url: Api.applyDifferences, params: { id } }, { joinParamsToUrl: true });
