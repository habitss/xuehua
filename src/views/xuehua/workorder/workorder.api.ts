import { defHttp } from '/@/utils/http/axios';

/** 工单中心接口（三域工单聚合，只读） */
enum Api {
  list = '/xuehua/workorder/listData',
  stat = '/xuehua/workorder/stat',
}

export const getWorkorderList = (params?) => defHttp.get({ url: Api.list, params });
export const getWorkorderStat = () => defHttp.get({ url: Api.stat });
