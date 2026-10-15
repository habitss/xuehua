/**
 * 雪花备件仓 仓库管理 状态码值与展示映射
 * 与后端 com.keytech.xuehua 实体定义保持一致
 */

/** 入库单状态（登记域） 0待登记 1已登记 */
export const inboundStatus: Record<number, { label: string; color: string }> = {
  0: { label: '待登记', color: 'orange' },
  1: { label: '已登记', color: 'success' },
};

/** 上架任务状态（上架域） 0待上架 1已上架 */
export const putawayStatus: Record<number, { label: string; color: string }> = {
  0: { label: '待上架', color: 'orange' },
  1: { label: '已上架', color: 'success' },
};

/** 出库工单状态 0待拣配 1拣配中 2已出库 */
export const outboundStatus: Record<number, { label: string; color: string }> = {
  0: { label: '待拣配', color: 'orange' },
  1: { label: '拣配中', color: 'processing' },
  2: { label: '已出库', color: 'success' },
};

/** 盘点单状态 0待盘点 1盘点中 2有差异 3已完成 4已终止 */
export const inventoryStatus: Record<number, { label: string; color: string }> = {
  0: { label: '待盘点', color: 'default' },
  1: { label: '盘点中', color: 'processing' },
  2: { label: '有差异', color: 'warning' },
  3: { label: '已完成', color: 'success' },
  4: { label: '已终止', color: 'default' },
};

/** 盘点阶段 0待开始 ~ 6完成 */
export const inventoryStages = ['待开始', '期初账面', '入库核实', '出库核对', '期末账面', '差异处理', '完成'];

/** 库位状态 0空位 1在库 2拣配中 3报警 4超大件存放 */
export const binStatus: Record<number, { label: string; color: string }> = {
  0: { label: '空位', color: 'default' },
  1: { label: '在库', color: 'processing' },
  2: { label: '拣配中', color: 'warning' },
  3: { label: '报警', color: 'error' },
  4: { label: '超大件存放', color: 'purple' },
};

/** 备件单位选项 */
export const unitOptions = ['件', '个', '套', '米', '根', '卷'].map((u) => ({ label: u, value: u }));

/** 备件类别选项（与原型 part.html 一致） */
export const categoryOptions = ['轴承', '传动', '阀门', '电气', '传感', '密封件', '过滤', '仪表', '电机', '超大件', '其他'].map((c) => ({ label: c, value: c }));

/** 库区选项 */
export const zoneOptions = [
  { label: 'A区(标准件)', value: 'A' },
  { label: 'B区(电机类)', value: 'B' },
  { label: 'C区(阀门类)', value: 'C' },
  { label: 'D区(超大件)', value: 'D' },
];

/** 车间选项（演示用，可后续接字典） */
export const workshopOptions = ['包装车间', '酿造车间', '动力车间', '灌装车间', '其他'].map((w) => ({ label: w, value: w }));
