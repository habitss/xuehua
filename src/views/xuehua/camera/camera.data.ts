import { FormSchema, BasicColumn } from '/@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';

export const DEVICE_TYPE: Record<number, string> = { 0: '视频AI服务器', 1: 'NVR', 2: '摄像机直连' };
export const LOCATION_TYPE: Record<number, string> = { 0: '库区', 1: '货架', 2: '库位' };

export const cameraColumns: BasicColumn[] = [
  { title: '点位编码', dataIndex: 'cameraCode', width: 110 },
  { title: '点位名称', dataIndex: 'cameraName', width: 150 },
  {
    title: '设备类型',
    dataIndex: 'deviceType',
    width: 120,
    customRender: ({ text }) => h(Tag, { color: ['processing', 'success', 'warning'][(text as number) || 0] }, () => DEVICE_TYPE[text as number] ?? '未知'),
  },
  { title: '设备IP', dataIndex: 'deviceIp', width: 130 },
  { title: '通道', dataIndex: 'channelNo', width: 60, align: 'center' },
  {
    title: '绑定层级',
    dataIndex: 'locationType',
    width: 90,
    customRender: ({ text }) => h(Tag, { color: 'default' }, () => LOCATION_TYPE[text as number] ?? '未绑定'),
  },
  {
    title: '绑定位置',
    dataIndex: 'location',
    width: 170,
    customRender: ({ record }) => {
      const r = record as any;
      const parts = [r.zoneCode && r.zoneCode + '区', r.rackCode, r.binCode].filter(Boolean);
      return parts.length ? parts.join(' · ') : '—';
    },
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 80,
    customRender: ({ text }) =>
      h(Tag, { color: text === 1 ? 'success' : 'default' }, () => (text === 1 ? '启用' : '停用')),
  },
  { title: '备注', dataIndex: 'remark', width: 180 },
];

export const cameraSearchSchema: FormSchema[] = [
  { label: '关键字', field: 'keyword', component: 'Input', colProps: { span: 6 }, componentProps: { placeholder: '点位编码/名称/IP' } },
  {
    label: '设备类型',
    field: 'deviceType',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: {
      options: [
        { label: '视频AI服务器', value: 0 },
        { label: 'NVR', value: 1 },
        { label: '摄像机直连', value: 2 },
      ],
      allowClear: true,
    },
  },
];

export const cameraFormSchema: FormSchema[] = [
  { label: '点位编码', field: 'cameraCode', component: 'Input', required: true, colProps: { span: 12 }, componentProps: { placeholder: '如 CAM-A-01' } },
  { label: '点位名称', field: 'cameraName', component: 'Input', required: true, colProps: { span: 12 } },
  {
    label: '设备类型',
    field: 'deviceType',
    component: 'Select',
    required: true,
    defaultValue: 0,
    colProps: { span: 12 },
    componentProps: {
      options: [
        { label: '视频AI服务器', value: 0 },
        { label: 'NVR', value: 1 },
        { label: '摄像机直连', value: 2 },
      ],
    },
  },
  { label: '设备IP', field: 'deviceIp', component: 'Input', required: true, colProps: { span: 12 }, componentProps: { placeholder: '视频AI服务器/相机 IP' } },
  { label: '通道号', field: 'channelNo', component: 'InputNumber', colProps: { span: 12 }, componentProps: { min: 1, style: { width: '100%' } } },
  { label: 'RTSP端口', field: 'rtspPort', component: 'InputNumber', defaultValue: 554, colProps: { span: 12 }, componentProps: { min: 1, style: { width: '100%' } } },
  {
    label: '绑定层级',
    field: 'locationType',
    component: 'Select',
    defaultValue: 0,
    colProps: { span: 12 },
    componentProps: {
      options: [
        { label: '库区', value: 0 },
        { label: '货架', value: 1 },
        { label: '库位', value: 2 },
      ],
    },
  },
  { label: '库区', field: 'zoneCode', component: 'Input', colProps: { span: 12 }, componentProps: { placeholder: '如 A' } },
  { label: '货架', field: 'rackCode', component: 'Input', colProps: { span: 12 }, componentProps: { placeholder: '绑定层级=货架/库位时填，如 A-01' } },
  { label: '库位', field: 'binCode', component: 'Input', colProps: { span: 12 }, componentProps: { placeholder: '绑定层级=库位时填，如 A-01-02' } },
  { label: '是否启用', field: 'status', component: 'RadioGroup', defaultValue: 1, colProps: { span: 12 }, componentProps: { options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }] } },
  { label: '备注', field: 'remark', component: 'InputTextArea', colProps: { span: 24 }, componentProps: { rows: 2 } },
];
