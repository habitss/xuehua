import { defHttp } from '/@/utils/http/axios';

/** 库龄分析接口 */
enum Api {
  list = '/xuehua/aging/listData',
  stat = '/xuehua/aging/stat',
  disposal = '/xuehua/aging/disposal',
  apply = '/xuehua/aging/disposal/apply',
  approve = '/xuehua/aging/disposal/approve',
}

export interface AgingRow {
  binCode: string;
  zoneCode?: string;
  rackCode?: string;
  partCode?: string;
  partName?: string;
  category?: string;
  qty?: number;
  firstPutaway?: string;
  agingDays: number;
  status: number; // 0正常 1偏高 2超期
}

export interface DisposalRow {
  id: number;
  binCode: string;
  partCode?: string;
  partName?: string;
  agingDays?: number;
  method: string;
  qty: number;
  reason: string;
  status: number; // 0待审批 1已通过 2已驳回
  applicant?: string;
  applyTime?: string;
  approver?: string;
  approveTime?: string;
  approveNote?: string;
}

export const getAgingList = (params?) => defHttp.get({ url: Api.list, params });
export const getAgingStat = () => defHttp.get({ url: Api.stat });
export const getDisposalList = (params?) => defHttp.get({ url: Api.disposal, params });
export const applyDisposal = (params) => defHttp.post({ url: Api.apply, params });
export const approveDisposal = (params) => defHttp.post({ url: Api.approve, params }, { joinParamsToUrl: true });
