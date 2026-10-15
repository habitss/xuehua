import { defHttp } from '/@/utils/http/axios';

/** 人员注册（监控管理）接口 */
enum Api {
  list = '/xuehua/person/listData',
  toggle = '/xuehua/person/toggleStatus',
}

export const getPersonList = (params) => defHttp.get({ url: Api.list, params });
/** 启用/停用（WCS 侧作业许可开关；档案变更请到海康侧操作后重新推送） */
export const togglePerson = (params) => defHttp.post({ url: Api.toggle, params }, { joinParamsToUrl: true });

/** Excel 导入/导出（人员注册 + 进出记录，jeecg 标准 exportXls/importExcel） */
export const exportXlsUrl = {
  person: '/xuehua/person/exportXls',
  access: '/xuehua/access/exportXls',
};
export const importXlsUrl = {
  person: '/xuehua/person/importExcel',
  access: '/xuehua/access/importExcel',
};

/** 下载导出文件（blob 保存）。isTransformResponse 属于 jeecg defHttp 的第二参 requestOptions，
 *  作用是跳过统一响应转换、拿原始 blob（放进第一参 axios config 会报 Volar 类型错） */
export function downloadXls(url: string, params: any, filename: string) {
  return defHttp
    .get({ url, params, responseType: 'blob' }, { isTransformResponse: false })
    .then((data) => {
      const blob = new Blob([data], { type: 'application/vnd.ms-excel' });
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = filename;
      a.click();
      URL.revokeObjectURL(a.href);
    });
}

/** 上传导入文件 */
export function importXls(url: string, file: File) {
  return defHttp.uploadFile({ url }, { file, name: 'files', filename: file.name });
}

/** 进出记录（监控管理）接口 */
enum AccessApi {
  list = '/xuehua/access/listData',
  playback = '/xuehua/access/playback',
}
export const getAccessList = (params) => defHttp.get({ url: AccessApi.list, params });
export const getPlayback = (id: number) => defHttp.get({ url: AccessApi.playback, params: { id } });
