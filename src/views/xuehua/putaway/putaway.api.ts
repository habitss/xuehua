import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/xuehua/putaway/listData',
  edit = '/xuehua/putaway/edit',
  get = '/xuehua/putaway/queryById',
  delete = '/xuehua/putaway/deleteData',
  exportXls = '/xuehua/putaway/exportXls',
  stat = '/xuehua/putaway/stat',
  confirmPutaway = '/xuehua/putaway/confirmPutaway',
}

/**
 *
 * 导出api
 */
export const getExportUrl = Api.exportXls;

/**
 * 上架任务列表
 * @param params
 */
export const getPutawayList = (params) => {
  return defHttp.get({ url: Api.list, params });
};

/**
 * 各状态数量统计（流程概览卡）
 */
export const getPutawayStat = () => {
  return defHttp.get({ url: Api.stat });
};

/**
 * 确认上架（待上架 -> 已上架）
 * @param params { id }
 */
export const confirmPutaway = (params) => {
  return defHttp.post({ url: Api.confirmPutaway, params }, { joinParamsToUrl: true });
};

/**
 * 通过id查询
 */
export const getPutawayById = (params) => {
  return defHttp.get({ url: Api.get, params });
};

/**
 * 单条删除
 */
export const deletePutaway = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.delete, params: { id: params.id } }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};
