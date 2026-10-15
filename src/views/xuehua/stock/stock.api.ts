import { defHttp } from '/@/utils/http/axios';

/** 库位地图接口 */
enum Api {
  map = '/xuehua/bin/mapData',
}

export interface BinRow {
  id: number;
  binCode: string;
  zoneCode?: string;
  zoneName?: string;
  rackCode?: string;
  layer?: number;
  colNo?: number;
  status: number;
  partCode?: string;
  qty?: number;
  tagAdd?: number;
  partName?: string;
  safeStock?: number;
  lowStock?: boolean;
}

/** 库位地图全量数据（含备件名称/安全库存/低库存预警装饰，现场量级几十位，客户端分组渲染） */
export const getBinList = () => defHttp.get({ url: Api.map });
