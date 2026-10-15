import { BasicColumn, FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

/** 事件类型字典（人员注册/进出记录/视频搜索共用） */
export const eventTypeOptions = [
  { label: '进仓', value: 1 },
  { label: '出仓', value: 2 },
  { label: '放置确认', value: 3 },
  { label: '拣配确认', value: 4 },
  { label: '非授权进入', value: 5 },
  { label: '尾随进入', value: 6 },
];

function evTag(eventType: number) {
  const map: Record<number, [string, string]> = {
    1: ['进仓', 'processing'],
    2: ['出仓', 'purple'],
    3: ['放置确认', 'success'],
    4: ['拣配确认', 'success'],
    5: ['非授权进入', 'error'],
    6: ['尾随进入', 'error'],
  };
  const [label, color] = map[eventType] || ['未知', 'default'];
  return h(Tag, { color }, () => label);
}

/** 人员注册列表列 */
export const personColumns: BasicColumn[] = [
  { title: '姓名', dataIndex: 'name', width: 90 },
  { title: '工号', dataIndex: 'empNo', width: 100 },
  { title: '部门', dataIndex: 'dept', width: 110 },
  { title: '识别方式', dataIndex: 'authMethod', width: 100 },
  {
    title: '人脸库',
    dataIndex: 'faceStatus',
    width: 90,
    customRender: ({ text }) => {
      const map = { 1: ['已采集', 'success'], 0: ['未采集', 'warning'], 2: ['采集失败', 'error'] } as any;
      const [label, color] = map[text] || ['未知', 'default'];
      return h(Tag, { color }, () => label);
    },
  },
  { title: '卡号', dataIndex: 'cardNo', width: 110 },
  { title: '授权区域', dataIndex: 'zones', width: 100 },
  {
    title: '状态',
    dataIndex: 'status',
    width: 80,
    customRender: ({ text }) => h(Tag, { color: text === 1 ? 'success' : 'default' }, () => (text === 1 ? '启用' : '停用')),
  },
  {
    title: '海康门禁授权',
    dataIndex: 'hikAuthStatus',
    width: 110,
    customRender: ({ text }) => {
      const map = { 1: ['已授权', 'success'], 2: ['授权失败', 'error'], 0: ['待授权', 'warning'] } as any;
      const [label, color] = map[text] || ['待授权', 'warning'];
      return h(Tag, { color }, () => label);
    },
  },
  { title: '注册时间', dataIndex: 'createTime', width: 150, customRender: ({ text }) => (text ? dayjs(text).format('YYYY-MM-DD HH:mm') : '') },
];

export const personSearchSchema: FormSchema[] = [
  { label: '姓名', field: 'name', component: 'Input', colProps: { span: 6 } },
  { label: '工号', field: 'empNo', component: 'Input', colProps: { span: 6 } },
  {
    label: '状态',
    field: 'status',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: { options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }] },
  },
];

/** 进出记录 / 视频搜索 列表列 */
export const accessColumns: BasicColumn[] = [
  { title: '时间', dataIndex: 'eventTime', width: 150, customRender: ({ text }) => (text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '') },
  { title: '人员', dataIndex: 'personName', width: 100 },
  { title: '部门', dataIndex: 'dept', width: 110 },
  { title: '事件类型', dataIndex: 'eventType', width: 100, customRender: ({ text }) => evTag(text) },
  { title: '区域', dataIndex: 'zone', width: 90 },
  { title: '识别方式', dataIndex: 'method', width: 90 },
  { title: '关联单据', dataIndex: 'bizNo', width: 130 },
];

export const accessSearchSchema: FormSchema[] = [
  { label: '人员', field: 'personName', component: 'Input', colProps: { span: 6 } },
  { label: '事件类型', field: 'eventType', component: 'Select', colProps: { span: 6 }, componentProps: { options: eventTypeOptions } },
];
