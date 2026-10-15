import { FormSchema } from '/@/components/Form';
import { getPositionByDepartId } from "./depart.api";
import { useMessage } from "@/hooks/web/useMessage";
import { BasicColumn } from "@/components/Table";
import {
  getDepartName,
  getDepartPathName,
  getDepartPathNameByOrgCode,
  getMultiDepartPathName
} from '@/utils/common/compUtils';
import { h, ref } from 'vue';

const { createMessage: $message } = useMessage();
//部门名称
const departNamePath = ref<Record<string, string>>({});

// 部门基础表单
export function useBasicFormSchema(treeData) {
  const basicFormSchema: FormSchema[] = [
    {
      field: 'departName',
      label: '机构名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入机构/部门名称',
      },
      rules: [{ required: true, message: '机构名称不能为空' }],
    },
    {
      field: 'departNameAbbr',
      label: '机构简称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入机构/部门简称',
      }
    },
    {
      field: 'parentId',
      label: '上级部门',
      component: 'TreeSelect',
      componentProps: {
        treeData: [],
        placeholder: '无',
        treeCheckAble: true,
        multiple: true,
        dropdownStyle: { maxHeight: '200px', overflow: 'auto' },
        tagRender: (options) => {
          const { value, label, option } = options;
          if (departNamePath.value[value]) {
            return h(
                'span', {  style: { marginLeft: '10px' } },
                departNamePath.value[value]
            );
          }
          getDepartPathNameByOrgCode('', label, option.id).then((data) => {
            departNamePath.value[value] = data;
          });
        },
      },
    },
    {
      field: 'orgCode',
      label: '机构编码',
      component: 'Input',
      componentProps: {
        placeholder: '请输入机构编码',
      },
    },
    {
      field: 'orgCategory',
      label: '机构类型',
      component: 'RadioButtonGroup',
      componentProps: { options: [] },
    },
    //update-begin author:ems date:2026-07-26 for:ems 岗位在 sys_position 表, sys_depart 无岗位, 去掉 jeecg 的"职务级别(positionId)"和"上级岗位(depPostParentId)"字段(原仅 orgCategory=='3' 即 jeecg 岗位时显示, ems 的 org_category=3 是工厂不应出现岗位字段) ---
    //update-end author:ems date:2026-07-26
    {
      field: 'departOrder',
      label: '排序',
      component: 'InputNumber',
      componentProps: {},
    },
    {
      field: 'mobile',
      label: '电话',
      component: 'Input',
      componentProps: {
        placeholder: '请输入电话',
      },
      //update-begin author:ems date:2026-07-26 for:ems org_category=3 是工厂(非岗位), 电话/传真/地址/备注 在所有机构类型都应显示 ---
      ifShow: () => {
        return true;
      },
      //update-end author:ems date:2026-07-26
    },
    {
      field: 'fax',
      label: '传真',
      component: 'Input',
      componentProps: {
        placeholder: '请输入传真',
      },
      //update-begin author:ems date:2026-07-26 for:ems org_category=3 是工厂(非岗位), 电话/传真/地址/备注 在所有机构类型都应显示 ---
      ifShow: () => {
        return true;
      },
      //update-end author:ems date:2026-07-26
    },
    {
      field: 'address',
      label: '地址',
      component: 'Input',
      componentProps: {
        placeholder: '请输入地址',
      },
      //update-begin author:ems date:2026-07-26 for:ems org_category=3 是工厂(非岗位), 电话/传真/地址/备注 在所有机构类型都应显示 ---
      ifShow: () => {
        return true;
      },
      //update-end author:ems date:2026-07-26
    },
    {
      field: 'memo',
      label: '备注',
      component: 'InputTextArea',
      componentProps: {
        placeholder: '请输入备注',
      },
      //update-begin author:ems date:2026-07-26 for:ems org_category=3 是工厂(非岗位), 电话/传真/地址/备注 在所有机构类型都应显示 ---
      ifShow: () => {
        return true;
      },
      //update-end author:ems date:2026-07-26
    },
    {
      field: 'id',
      label: 'ID',
      component: 'Input',
      show: false,
    },
  ];
  return { basicFormSchema };
}

// 机构类型选项
//update-begin author:ems date:2026-07-26 for:ems 机构类型采用 6 级体系(1集团/2大区/3工厂/4部门/5区域/6产线), 对齐 mes3; ems 岗位在 sys_position 表, sys_depart 无岗位, 去掉 jeecg 标准的子公司/岗位/childDepartPost/childPost ---
export const orgCategoryOptions = {
  // 一级部门(集团)
  root: [{ value: '1', label: '集团' }],
  // 子级部门(大区/工厂/部门/区域/产线)
  child: [
    { value: '2', label: '大区' },
    { value: '3', label: '工厂' },
    { value: '4', label: '部门' },
    { value: '5', label: '区域' },
    { value: '6', label: '产线' },
  ],
};
//update-end author:ems date:2026-07-26

/**
 * 用户列表
 */
export const userColumns: BasicColumn[] = [
  {
    title: '姓名',
    dataIndex: 'realname',
    width: 150,
  },
  {
    title: '手机',
    width: 150,
    dataIndex: 'phone',
    customRender:( { record, text })=>{
      if(record.izHideContact && record.izHideContact === '1'){
        return '/';
      }
      return text;
    }
  },
  {
    title: '主岗位',
    dataIndex: 'mainDepPostId',
    customRender: ({ record, text })=>{
      if(!text){
        return '';
      }
      return getDepartName(getDepartPathName(record.mainDepPostId_dictText,text,false));
    },
    width: 200,
  },
  {
    title: '兼职岗位',
    dataIndex: 'otherDepPostId',
    customRender: ({ record, text })=>{
      if(!text){
        return '';
      }
      return getDepartName(getMultiDepartPathName(record.otherDepPostId_dictText,text));
    },
    width: 200,
  },
];

/**
 * 职位改变事件
 * @param value
 * @param model
 * @param treeData
 */
export function positionChange(value, model, treeData) {
  if(value && model.parentId){
    getPositionByDepartId({ parentId: model.parentId, departId: model.id ? model.id:'', positionId: value }).then((res) =>{
      if(res.success){
        treeData.value = res.result;
      }else{
        treeData.value = [];
        $message.warning(res.message);
      }
    });
  } else {
    treeData.value = [];
  }
}
