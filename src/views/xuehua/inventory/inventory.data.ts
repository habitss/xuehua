import { BasicColumn, FormSchema } from '/@/components/Table';
import dayjs from 'dayjs';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { inventoryStatus, zoneOptions } from '../components/status';

/** 盘点单状态 Tag 渲染 */
export function renderInventoryStatus(status: number) {
  const item = inventoryStatus[status] || { label: String(status), color: 'default' };
  return h(Tag, { color: item.color }, () => item.label);
}

/** 时间列渲染 */
export function renderTime(text) {
  return text ? dayjs(text).format('YYYY-MM-DD HH:mm') : '';
}

export const columns: BasicColumn[] = [
  { title: '盘点单号', dataIndex: 'pvNo', width: 110 },
  { title: '来源', dataIndex: 'source', width: 90 },
  { title: '区域', dataIndex: 'zoneCode', width: 80 },
  { title: '范围', dataIndex: 'scope', width: 100 },
  { title: '执行人', dataIndex: 'executor', width: 90 },
  { title: '识别方式', dataIndex: 'identifyType', width: 110 },
  { title: '账面数', dataIndex: 'bookQty', width: 80 },
  { title: '实盘数', dataIndex: 'realQty', width: 80 },
  {
    title: '进度',
    dataIndex: 'progress',
    width: 130,
    customRender: ({ text }) => `${text ?? 0}%`,
  },
  { title: '状态', dataIndex: 'status', width: 90, customRender: ({ text }) => renderInventoryStatus(text) },
  { title: '创建时间', dataIndex: 'createTime', width: 140, customRender: ({ text }) => renderTime(text) },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'pvNo',
    label: '盘点单号',
    component: 'Input',
    colProps: { span: 6 },
  },
  {
    field: 'executor',
    label: '执行人',
    component: 'Input',
    colProps: { span: 6 },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: Object.entries(inventoryStatus).map(([value, item]) => ({ label: item.label, value: Number(value) })),
    },
    colProps: { span: 6 },
  },
  {
    field: 'createTime',
    label: '创建时间',
    component: 'RangePicker',
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
    field: 'zoneCode',
    label: '盘点区域',
    component: 'Select',
    componentProps: { options: zoneOptions, placeholder: '不选则全仓' },
  },
  {
    field: 'scope',
    label: '盘点范围',
    component: 'Select',
    componentProps: {
      options: [
        { label: '全部备件', value: '全部备件' },
        { label: '超期件', value: '超期件' },
        { label: '高值件', value: '高值件' },
      ],
    },
  },
  {
    field: 'executor',
    label: '执行人',
    component: 'Input',
    required: true,
  },
  {
    field: 'bookQty',
    label: '账面数',
    component: 'InputNumber',
    componentProps: { min: 0, precision: 0 },
  },
];
