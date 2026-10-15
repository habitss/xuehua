import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

enum Api {
  list = '/xuehua/inbound/listData',
  save = '/xuehua/inbound/add',
  edit = '/xuehua/inbound/edit',
  get = '/xuehua/inbound/queryById',
  delete = '/xuehua/inbound/deleteData',
  deleteBatch = '/xuehua/inbound/deleteDataBatch',
  importExcel = '/xuehua/inbound/importExcel',
  exportXls = '/xuehua/inbound/exportXls',
  stat = '/xuehua/inbound/stat',
  scan = '/xuehua/inbound/scan',
  confirmPutaway = '/xuehua/inbound/confirmPutaway',
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
export const getInboundList = (params) => {
  return defHttp.get({ url: Api.list, params });
};

/**
 * 保存或者更新
 * @param params
 * @param isUpdate
 */
export const saveOrUpdateInbound = (params, isUpdate) => {
  const url = isUpdate ? Api.edit : Api.save;
  return isUpdate ? defHttp.put({ url, params }) : defHttp.post({ url, params });
};

/**
 * 查询详情
 * @param params
 */
export const getInboundById = (params) => {
  return defHttp.get({ url: Api.get, params });
};

/**
 * 单条删除
 * @param params
 */
export const deleteInbound = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.delete, params: { id: params.id } }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};

/**
 * 批量删除
 * @param params
 */
export const batchDeleteInbound = (params, handleSuccess) => {
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
export const getInboundStat = () => {
  return defHttp.get({ url: Api.stat });
};

/**
 * 扫码预填：扫备件条码自动带出主数据；扫送货单号查重提醒
 * @param params { code }
 */
export const scanCode = (params) => {
  return defHttp.get({ url: Api.scan, params });
};

/**
 * 确认上架（待上架 -> 已上架）
 * @param params { id }
 */
export const confirmPutaway = (params) => {
  return defHttp.post({ url: Api.confirmPutaway, params }, { joinParamsToUrl: true });
};
