import { BasicColumn, FormSchema } from '/@/components/Table';
import dayjs from 'dayjs';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { inboundStatus, unitOptions } from '../components/status';

/** 入库单状态 Tag 渲染 */
export function renderInboundStatus(status: number) {
  const item = inboundStatus[status] || { label: String(status), color: 'default' };
  return h(Tag, { color: item.color }, () => item.label);
}

/** 三要素（送货单/全景照片/检验报告）是否已上传标记 */
export function renderUploaded(url: any) {
  return url ? '✓' : '✗';
}

/** 时间列渲染 */
export function renderTime(text) {
  return text ? dayjs(text).format('YYYY-MM-DD HH:mm') : '';
}

export const columns: BasicColumn[] = [
  { title: '入库单号', dataIndex: 'inboundNo', width: 130 },
  { title: '备件编号', dataIndex: 'partCode', width: 100 },
  { title: '备件名称', dataIndex: 'partName', width: 140 },
  { title: '型号/规格', dataIndex: 'model', width: 110 },
  { title: '数量', dataIndex: 'qty', width: 70 },
  { title: '单位', dataIndex: 'unit', width: 60 },
  { title: '分配库位', dataIndex: 'binCode', width: 100 },
  { title: '送货单', dataIndex: 'deliveryNo', width: 130 },
  { title: '全景照片', dataIndex: 'photo', width: 90, customRender: ({ text }) => renderUploaded(text) },
  { title: '检验报告', dataIndex: 'report', width: 90, customRender: ({ text }) => renderUploaded(text) },
  { title: '操作人', dataIndex: 'operator', width: 90 },
  { title: '状态', dataIndex: 'status', width: 90, customRender: ({ text }) => renderInboundStatus(text) },
  { title: '登记时间', dataIndex: 'createTime', width: 140, customRender: ({ text }) => renderTime(text) },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'partCode',
    label: '备件编号',
    component: 'Input',
    colProps: { span: 6 },
  },
  {
    field: 'deliveryNo',
    label: '送货单号',
    component: 'Input',
    colProps: { span: 6 },
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: Object.entries(inboundStatus).map(([value, item]) => ({ label: item.label, value: Number(value) })),
    },
    colProps: { span: 6 },
  },
  {
    field: 'createTime',
    label: '登记时间',
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
    field: 'partCode',
    label: '备件编号',
    component: 'Input',
    required: true,
    componentProps: { placeholder: '扫码枪自动填充，或手动输入，如 BJ-0001' },
  },
  {
    field: 'poNo',
    label: '采购单号',
    component: 'Input',
    componentProps: { placeholder: '扫描采购单条码自动带出' },
  },
  {
    field: 'partName',
    label: '备件名称',
    component: 'Input',
    required: true,
  },
  {
    field: 'model',
    label: '型号/规格',
    component: 'Input',
  },
  {
    field: 'qty',
    label: '入库数量',
    component: 'InputNumber',
    required: true,
    componentProps: { min: 1, precision: 0 },
  },
  {
    field: 'unit',
    label: '单位',
    component: 'Select',
    componentProps: { options: unitOptions },
  },
  {
    field: 'binCode',
    label: '指定库位',
    component: 'Input',
    componentProps: { placeholder: '留空由 WCS 自动分配空位' },
  },
  {
    field: 'operator',
    label: '操作人',
    component: 'Input',
    componentProps: { placeholder: '留空自动识别为当前登录人' },
  },
  {
    field: 'deliveryNo',
    label: '送货单',
    component: 'Input',
    required: true,
    componentProps: { placeholder: '请输入送货单号' },
  },
  {
    field: 'photo',
    label: '全景照片',
    component: 'JUpload',
    required: true,
    helpMessage: '送货备件全景照片（入库登记三要素之一）',
    componentProps: { fileType: 'image', maxCount: 1, bizPath: 'xuehua/inbound', text: '选择照片' },
  },
  {
    field: 'report',
    label: '检验报告',
    component: 'JUpload',
    required: true,
    helpMessage: '检验报告 PDF（入库登记三要素之一）',
    componentProps: { fileType: 'file', maxCount: 1, accept: '.pdf', bizPath: 'xuehua/inbound', text: '选择PDF' },
  },
];
