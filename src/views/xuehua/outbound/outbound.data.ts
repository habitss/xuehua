import { BasicColumn, FormSchema } from '/@/components/Table';
import dayjs from 'dayjs';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { outboundStatus, categoryOptions, workshopOptions } from '../components/status';

/** 出库工单状态 Tag 渲染 */
export function renderOutboundStatus(status: number) {
  const item = outboundStatus[status] || { label: String(status), color: 'default' };
  return h(Tag, { color: item.color }, () => item.label);
}

/** 时间列渲染 */
export function renderTime(text) {
  return text ? dayjs(text).format('YYYY-MM-DD HH:mm') : '';
}

export const columns: BasicColumn[] = [
  { title: '工单号', dataIndex: 'woNo', width: 130 },
  { title: '备件编号', dataIndex: 'partCode', width: 100 },
  { title: '备件名称', dataIndex: 'partName', width: 140 },
  { title: '类别', dataIndex: 'category', width: 90 },
  { title: '数量', dataIndex: 'qty', width: 70 },
  { title: '申请人', dataIndex: 'applyUser', width: 90 },
  { title: '领用人', dataIndex: 'receiveUser', width: 90 },
  { title: '车间', dataIndex: 'workshop', width: 100 },
  { title: '首拣库位', dataIndex: 'binCode', width: 110 },
  { title: '状态', dataIndex: 'status', width: 90, customRender: ({ text }) => renderOutboundStatus(text) },
  { title: '申请时间', dataIndex: 'createTime', width: 140, customRender: ({ text }) => renderTime(text) },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'woNo',
    label: '工单号',
    component: 'Input',
    colProps: { span: 6 },
  },
  {
    field: 'partName',
    label: '备件名称',
    component: 'Input',
    colProps: { span: 6 },
  },
  {
    field: 'applyUser',
    label: '申请人',
    component: 'Input',
    colProps: { span: 6 },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: Object.entries(outboundStatus).map(([value, item]) => ({ label: item.label, value: Number(value) })),
    },
    colProps: { span: 6 },
  },
];

export const formSchema: FormSchema[] = [
  {
    label: '主键',
    field: 'id',
    component: 'Input',
    show: false,
  },
  {
    field: 'partCode',
    label: '备件编号',
    component: 'Input',
    required: true,
    componentProps: { placeholder: '扫码自动填充，或手动输入' },
  },
  {
    field: 'partName',
    label: '备件名称',
    component: 'Input',
    required: true,
  },
  {
    field: 'category',
    label: '类别',
    component: 'Select',
    componentProps: { options: categoryOptions },
  },
  {
    field: 'qty',
    label: '数量',
    component: 'InputNumber',
    required: true,
    componentProps: { min: 1, precision: 0 },
  },
  {
    field: 'applyUser',
    label: '申请人',
    component: 'Input',
    required: true,
  },
  {
    field: 'receiveUser',
    label: '领用人',
    component: 'Input',
  },
  {
    field: 'workshop',
    label: '车间',
    component: 'Select',
    componentProps: { options: workshopOptions },
  },
  // 2026-09-13 起移除"取货库位"输入：领料单不填库位，提交后由 WCS 按
  // 库龄优先 FIFO 自动分配拣配库位（可能多个），分配结果见列表展开行/拣配指引
];
