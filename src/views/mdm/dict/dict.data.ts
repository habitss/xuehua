import { BasicColumn, FormSchema } from '/@/components/Table';
import { rules } from '/@/utils/helper/validator';
import { categorizeList } from './dict.api';

// 字典组（子类）列表列
export const columns: BasicColumn[] = [
  {
    title: '字典组名称',
    dataIndex: 'dictGroupName',
    width: 240,
  },
  {
    title: '字典组编码',
    dataIndex: 'dictGroupCode',
    width: 240,
  },
  {
    title: '描述',
    dataIndex: 'remark',
  },
];

// 字典组搜索表单
export const searchFormSchema: FormSchema[] = [
  {
    label: '字典组名称',
    field: 'dictGroupName',
    component: 'Input',
    colProps: { span: 6 },
  },
  {
    label: '字典组编码',
    field: 'dictGroupCode',
    component: 'Input',
    colProps: { span: 6 },
  },
];

// 字典组新增/编辑表单
export const formSchema: FormSchema[] = [
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
  {
    label: '',
    field: 'categorizeId',
    component: 'Input',
    show: false,
  },
  {
    label: '字典组名称',
    field: 'dictGroupName',
    required: true,
    component: 'Input',
  },
  {
    label: '字典组编码',
    field: 'dictGroupCode',
    component: 'Input',
    dynamicDisabled: ({ values }) => {
      return !!values.id;
    },
    dynamicRules: ({ model, schema }) => {
      const arr = rules.duplicateCheckRule('mes4_mdm_dict_group', 'dict_group_code', model, schema, true);
      arr[0].required = true;
      return arr;
    },
  },
  {
    label: '描述',
    field: 'remark',
    component: 'Input',
  },
];

// 字典配置项列表列
export const dictItemColumns: BasicColumn[] = [
  {
    title: '名称',
    dataIndex: 'itemKey',
    width: 80,
  },
  {
    title: '数据值',
    dataIndex: 'itemVal',
    width: 80,
  },
  {
    title: '排序',
    dataIndex: 'sort',
    width: 80,
  },
];

// 字典配置项搜索表单
export const dictItemSearchFormSchema: FormSchema[] = [
  {
    label: '名称',
    field: 'itemKey',
    component: 'Input',
  },
];

// 字典配置项新增/编辑表单
export const itemFormSchema: FormSchema[] = [
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
  {
    label: '名称',
    field: 'itemKey',
    required: true,
    component: 'Input',
  },
  {
    label: '数据值',
    field: 'itemVal',
    component: 'Input',
    dynamicDisabled: ({ values }) => {
      return !!values.id;
    },
    dynamicRules: () => {
      return [
        {
          required: true,
          validator: (_, value) => {
            if (!value) {
              return Promise.reject('请输入数据值');
            }
            return Promise.resolve();
          },
        },
      ];
    },
  },
  {
    label: '描述',
    field: 'remark',
    component: 'Input',
  },
  {
    field: 'sort',
    label: '排序',
    component: 'InputNumber',
    defaultValue: 1,
  },
];

// 字典分类（大类）表单
export const categorizeFormSchema: FormSchema[] = [
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
  {
    label: '分类名称',
    field: 'categorizeName',
    required: true,
    component: 'Input',
  },
  {
    label: '父类',
    field: 'parentId',
    component: 'ApiSelect',
    componentProps: {
      api: categorizeList,
      params: {
        parentId: 0,
      },
      labelField: 'categorizeName',
      valueField: 'id',
      getPopupContainer: () => document.body,
    },
  },
];
