import { defHttp } from '/@/utils/http/axios';

/** 备件使用报表接口（出库明细用途跟踪） */
enum Api {
  list = '/xuehua/usage/usageList',
  confirm = '/xuehua/usage/confirm',
  remind = '/xuehua/usage/remind',
  exportXls = '/xuehua/usage/exportXls',
}

export interface UsageRow {
  id: number;
  woNo?: string;
  partCode?: string;
  partName?: string;
  category?: string;
  workshop?: string; // 领用产线
  receiver?: string; // 领用人
  qty?: number; // 领用数量
  outTime?: string;
  days?: number; // 出库天数
  usedQty?: number; // 实际使用数量
  purpose?: string; // 用途说明
  photos?: string; // 逗号拼接 url
  status: number; // 0待确认 1已使用 2未使用（超1周未使用为派生，见 statusText）
  statusText?: string;
  remindCount?: number;
}

/** 装饰列表（返回 {records,total}） */
export const getUsageList = (params?) => defHttp.get({ url: Api.list, params });

/** 确认用途：used=true 已使用 / false 登记未使用（joinParamsToUrl 对齐后端 @RequestParam） */
export const confirmUsage = (params: { id: number; used: boolean; usedQty?: number; photos?: string; purpose?: string }) =>
  defHttp.post({ url: Api.confirm, params }, { joinParamsToUrl: true, isTransformResponse: false });

/** 提醒确认：ids 空 = 一键提醒全部未确认（body 统一 {"ids":[..]} 包裹，空对象后端按一键处理） */
export const remindUsage = (ids: number[] | null) => defHttp.post({ url: Api.remind, params: ids ? { ids } : {} });

/** 导出 Excel（blob 双参写法） */
export async function downloadUsageXls(params: any, filename: string) {
  const res = await defHttp.get({ url: Api.exportXls, params, responseType: 'blob' }, { isTransformResponse: false });
  const blob = new Blob([res], { type: 'application/vnd.ms-excel' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
}
