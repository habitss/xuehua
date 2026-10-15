import { defHttp } from '/@/utils/http/axios';

/**
 * 工作台（运营驾驶舱）接口
 * 后端一次聚合返回全部区块数据，前端 60s 轮询刷新
 */
enum Api {
  overview = '/xuehua/dashboard/overview',
}

export interface DashboardOverview {
  kpi: { workOrders: number; inbound: number; outbound: number; partKinds: number; overAged: number };
  todos: { toPutaway: number; toPick: number; toInventory: number; diffInventory: number; lowStock: number };
  catDonut: { n: string; v: number }[];
  trend: { ym: string; cin: number; cout: number }[];
  aging: { bucket: string; cnt: number }[];
  zones: { zone: string; total: number; instock: number; pct: number }[];
  liveFeed: { type: string; no: string; partname: string; qty: number; person: string; t: string; status: string }[];
  alarms: { errCount: number; warnCount: number; items: { lv: string; t: string; m: string }[] };
}

export const getOverview = () => defHttp.get<DashboardOverview>({ url: Api.overview });
