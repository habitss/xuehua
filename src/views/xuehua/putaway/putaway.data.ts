import { BasicColumn, FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { putawayStatus } from '../components/status';
import { renderTime } from '../inbound/inbound.data';

/** 上架任务状态 Tag 渲染 **/
export function renderPutawayStatus(status: number) {
  const item = putawayStatus[status] || { label: String(status), color: 'default' };
  return h(Tag, { color: item.color }, () => item.label);
}

export const columns: BasicColumn[] = [
  { title: '入库单号', dataIndex: 'inboundNo', width: 130 },
  { title: '备件编号', dataIndex: 'partCode', width: 100 },
  { title: '备件名称', dataIndex: 'partName', width: 140 },
  { title: '型号/规格', dataIndex: 'model', width: 110 },
  { title: '数量', dataIndex: 'qty', width: 70 },
  { title: '单位', dataIndex: 'unit', width: 60 },
  { title: '放置库位', dataIndex: 'binCode', width: 100 },
  { title: '登记人', dataIndex: 'registerBy', width: 90 },
  { title: '登记时间', dataIndex: 'createTime', width: 140, customRender: ({ text }) => renderTime(text) },
  { title: '状态', dataIndex: 'status', width: 90, customRender: ({ text }) => renderPutawayStatus(text) },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'inboundNo',
    label: '入库单号',
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
    field: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: Object.entries(putawayStatus).map(([value, item]) => ({ label: item.label, value: Number(value) })),
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
