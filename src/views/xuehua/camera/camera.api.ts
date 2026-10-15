import { defHttp } from '/@/utils/http/axios';

/** 视频点位接口（监控管理） */
enum Api {
  list = '/xuehua/camera/listData',
  save = '/xuehua/camera/add',
  edit = '/xuehua/camera/edit',
  delete = '/xuehua/camera/deleteData',
  exportXls = '/xuehua/camera/exportXls',
  discover = '/xuehua/camera/discoverChannels',
  preview = '/xuehua/camera/previewUrl',
}

export interface CameraRow {
  id: number;
  cameraCode: string;
  cameraName?: string;
  deviceType?: number; // 0视频AI服务器 1NVR 2摄像机直连
  deviceIp?: string;
  channelNo?: number;
  indexCode?: string;
  rtspPort?: number;
  locationType?: number; // 0库区 1货架 2库位
  zoneCode?: string;
  rackCode?: string;
  binCode?: string;
  status?: number;
  remark?: string;
}

export const getCameraList = (params?) => defHttp.get({ url: Api.list, params });
export const saveCamera = (params, isUpdate: boolean) =>
  isUpdate ? defHttp.put({ url: Api.edit, params }) : defHttp.post({ url: Api.save, params });
export const deleteCamera = (params, handleSuccess) =>
  defHttp.delete({ url: Api.delete, params: { id: params.id } }, { joinParamsToUrl: true }).then(() => handleSuccess());

/** 从 AI 服务器/NVR 拉通道（未接入时后端返回错误信息） */
export const discoverChannels = () => defHttp.get({ url: Api.discover });

/** RTSP 地址生成（实时主/子码流；带时间段附回放） */
export const previewUrl = (params: { id: number; startTime?: string; endTime?: string }) => defHttp.get({ url: Api.preview, params });

/** 导出（blob 双参写法） */
export async function downloadCameraXls(params: any, filename: string) {
  const res = await defHttp.get({ url: Api.exportXls, params, responseType: 'blob' }, { isTransformResponse: false });
  const blob = new Blob([res], { type: 'application/vnd.ms-excel' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
}
