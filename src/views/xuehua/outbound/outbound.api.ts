import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

enum Api {
  list = '/xuehua/outbound/listData',
  save = '/xuehua/outbound/add',
  edit = '/xuehua/outbound/edit',
  get = '/xuehua/outbound/queryById',
  delete = '/xuehua/outbound/deleteData',
  deleteBatch = '/xuehua/outbound/deleteDataBatch',
  importExcel = '/xuehua/outbound/importExcel',
  exportXls = '/xuehua/outbound/exportXls',
  stat = '/xuehua/outbound/stat',
  confirmPick = '/xuehua/outbound/confirmPick',
  detailLines = '/xuehua/outbound/detailLines',
  confirmPickLine = '/xuehua/outbound/confirmPickLine',
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
export const getOutboundList = (params) => {
  return defHttp.get({ url: Api.list, params });
};

/**
 * 保存或者更新（新增即领用申请）
 * @param params
 * @param isUpdate
 */
export const saveOrUpdateOutbound = (params, isUpdate) => {
  const url = isUpdate ? Api.edit : Api.save;
  return isUpdate ? defHttp.put({ url, params }) : defHttp.post({ url, params });
};

/**
 * 查询详情
 * @param params
 */
export const getOutboundById = (params) => {
  return defHttp.get({ url: Api.get, params });
};

/**
 * 单条删除
 * @param params
 */
export const deleteOutbound = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.delete, params: { id: params.id } }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};

/**
 * 批量删除
 * @param params
 */
export const batchDeleteOutbound = (params, handleSuccess) => {
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
export const getOutboundStat = () => {
  return defHttp.get({ url: Api.stat });
};

/**
 * 扫码确认出库（确认该工单最早待拣行；多库位单一按一行）
 * @param params { id }
 */
export const confirmPick = (params) => {
  return defHttp.post({ url: Api.confirmPick, params }, { joinParamsToUrl: true });
};

/**
 * 工单拣配明细行（seq 升序）：自动分配的逐库位拣配任务
 * @param params { woNo }
 */
export const getOutboundLines = (params) => {
  return defHttp.get({ url: Api.detailLines, params });
};

/**
 * 行级确认拣配：该行库位台账原子扣减 + 工单状态推进
 * @param params { lineId }
 */
export const confirmPickLine = (params) => {
  return defHttp.post({ url: Api.confirmPickLine, params }, { joinParamsToUrl: true });
};
