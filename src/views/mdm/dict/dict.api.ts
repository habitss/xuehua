import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

enum Api {
  // 字典组（业务字典-子类）
  list = '/mdm/mdmDictGroup/listData',
  save = '/mdm/mdmDictGroup/add',
  edit = '/mdm/mdmDictGroup/edit',
  deleteDict = '/mdm/mdmDictGroup/deleteData',
  deleteBatch = '/mdm/mdmDictGroup/deleteDataBatch',
  importExcel = '/mdm/mdmDictGroup/importExcel',
  exportXls = '/mdm/mdmDictGroup/exportXls',
  // 字典配置项
  itemList = '/mdm/mdmDict/listData',
  deleteItem = '/mdm/mdmDict/deleteData',
  itemSave = '/mdm/mdmDict/add',
  itemEdit = '/mdm/mdmDict/edit',
  dicList = '/mdm/mdmDict/dicList',
  // 字典分类（业务字典-大类）
  categorizeList = '/mdm/mdmDictCategorize/queryList',
  categorizeSave = '/mdm/mdmDictCategorize/add',
  categorizeEdit = '/mdm/mdmDictCategorize/edit',
  categorizeDeleteOne = '/mdm/mdmDictCategorize/delete',
  loadTreeData = '/mdm/mdmDictCategorize/loadTreeRoot',
  deletionBatchDictCategorize = '/mdm/mdmDictCategorize/deletionBatch',
}

/**
 * 导出api
 */
export const getExportUrl = Api.exportXls;
/**
 * 导入api
 */
export const getImportUrl = Api.importExcel;

/**
 * 字典组列表接口
 * @param params
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 删除字典组
 */
export const deleteDict = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.deleteDict, params: { id: params.id } }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};

/**
 * 批量删除字典组
 * @param params
 */
export const batchDeleteDict = (params, handleSuccess) => {
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
 * 保存或者更新字典组
 * @param params
 * @param isUpdate
 */
export const saveOrUpdateDict = (params, isUpdate) => {
  const url = isUpdate ? Api.edit : Api.save;
  if (isUpdate) {
    return defHttp.put({ url, params });
  }
  return defHttp.post({ url, params });
};

/**
 * 字典配置项列表
 * @param params
 */
export const itemList = (params) => defHttp.get({ url: Api.itemList, params });

/**
 * 字典配置项删除
 */
export const deleteItem = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.deleteItem, params: { id: params.id } }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};

/**
 * 保存或者更新字典配置项
 */
export const saveOrUpdateDictItem = (params, isUpdate) => {
  const url = isUpdate ? Api.itemEdit : Api.itemSave;
  if (isUpdate) {
    return defHttp.put({ url, params });
  }
  return defHttp.post({ url, params });
};

/**
 * 按字典组编码查询字典项（供其他模块使用）
 * @param params
 */
export const dicList = (params) => defHttp.get({ url: Api.dicList, params });

/**
 * 字典分类列表接口
 * @param params
 */
export const categorizeList = (params) => defHttp.get({ url: Api.categorizeList, params });

/**
 * 字典分类保存或者更新
 */
export const categorizeSaveOrUpdate = (params, isUpdate) => {
  let url = isUpdate ? Api.categorizeEdit : Api.categorizeSave;
  return defHttp.post({ url, params });
};

/**
 * 查询全部树形节点数据
 * @param params
 */
export const loadTreeData = (params) => defHttp.get({ url: Api.loadTreeData, params });

/**
 * 批量删除字典分类
 */
export const deletionBatchDictCategorize = (params, handleSuccess) => {
  Modal.confirm({
    title: '确认删除',
    content: '是否删除选中数据',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      const ids = Array.isArray(params.ids) ? params.ids.join(',') : params.ids;
      return defHttp.delete({ url: Api.deletionBatchDictCategorize, params: { ids } }, { joinParamsToUrl: true }).then(() => {
        handleSuccess();
      });
    },
  });
};
