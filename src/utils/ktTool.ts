import { createLocalStorage, createSessionStorage } from '/@/utils/cache';
import { useUserStore } from '/@/store/modules/user';

const departOfStore = createLocalStorage();
const ss = createSessionStorage();

// 修改table列表列头排序的参数字段，把  column=createTime&order=desc 改为  column=createTime&order=desc
export function changeTableSortParams(params) {
  const orderField = params.column;
  const orderType = params.order;

  delete params.column;
  delete params.order;
  //更新默认jeecg的table请求列表接口排序参数createTIME为id
  if (!params.orderField) {
    params = Object.assign(params, { orderField: 'id', orderType: orderType });
  }


}

// 根据当前选中的部门自动获取他的父级工厂值，并且写入缓存中
function setDepartInitVal(orgCode, tree, parentKey) {
  for (let item in tree) {
    // console.log('tree[item].orgCategory:', tree[item].orgCategory)
    // console.log('tree[item].orgCode:', tree[item].orgCode)
    // console.log('tree[item]------:', orgCode)
    if (tree[item].orgCode === orgCode && tree[item].orgCategory === '3') {
      departOfStore.set('KT_CURRENT_FAC', orgCode);
    }
    if (tree[item].orgCode === orgCode && tree[item].orgCategory === '4') {
      departOfStore.set('KT_CURRENT_DEPART', orgCode);
      departOfStore.set('KT_CURRENT_FAC', parentKey);
    }
    setDepartInitVal(orgCode, tree[item].children, tree[item].orgCode);
  }
}

// 设置当前选中的部门到缓存中
export function setStorageDepart(orgCode) {
  departOfStore.remove('KT_CURRENT_DEPART');
  departOfStore.remove('KT_CURRENT_FAC');
  //选择部门时，自动找出他的工厂，
  // if (orgCategory === '4') {
  const departTree = departOfStore.get('KT_ALL_DEPARTS');
  setDepartInitVal(orgCode, departTree, '');
  // } else if (orgCategory === '3') {
  //选择工厂时，直接设置工厂到换成中
  // departOfStore.set('KT_CURRENT_FAC', orgCode);
  // }
}

// 通过orgCode从treeDeparts中查找对应的id
export function getDepartId(orgCode) {
  const departTree = departOfStore.get('KT_ALL_DEPARTS');
  function loopTree(children: Array<object>, code: string) {
    for (const item in children) {
      if (children[item].orgCode === code) {
        return children[item].id
      }
      if (children[item].children && children[item].children.length > 0) {
        return loopTree(children[item].children, code);
      }
    }
    return ''
  }
  // console.log('orgCode:', orgCode);
  // console.log('departTree:', departTree);
  const departId = loopTree(departTree, orgCode);
  return departId;

}

// 根据传入的 orgCode 获取工厂、部门、产线信息
export function getFactoryDepartLineByOrgCode(orgCode, targetObj = null) {
  if (!orgCode || typeof orgCode !== 'string') {
    const result = {
      factoryCode: '',
      deptCode: '',
      lineCode: ''
    };
    
    // 如果传入了目标对象，将结果添加到对象中
    if (targetObj && typeof targetObj === 'object') {
      Object.assign(targetObj, result);
    }
    
    return result;
  }

  const orgCodeLength = orgCode.length;
  let result;
  
  if (orgCodeLength <= 9) {
    // 长度小于等于9位的都是工厂
    result = {
      factoryCode: orgCode,
      deptCode: '',
      lineCode: ''
    };
  } else if (orgCodeLength === 12) {
    // 12位的是部门，截取前九位当工厂
    result = {
      factoryCode: orgCode.substring(0, 9),
      deptCode: orgCode,
      lineCode: ''
    };
  } else if (orgCodeLength > 12) {
    // 15位的是产线，按上面拆分部门和工厂
    result = {
      factoryCode: orgCode.substring(0, 9),
      deptCode: orgCode.substring(0, 12),
      lineCode: orgCode
    };
  } else {
    // 其他长度，返回空
    result = {
      factoryCode: '',
      deptCode: '',
      lineCode: ''
    };
  }
  
  // 如果传入了目标对象，将结果添加到对象中
  if (targetObj && typeof targetObj === 'object') {
    Object.assign(targetObj, result);
  }
  
  return result;
}

export function setFirstAvailableCode(obj, fieldName) {
  if (!obj || typeof obj !== 'object' || !fieldName || typeof fieldName !== 'string') {
    return obj;
  }
  // 从小到大判断：lineCode → deptCode → factoryCode
  if (obj.lineCode && obj.lineCode !== '') {
    obj[fieldName] = obj.lineCode;
  }else if (obj.deptCode && obj.deptCode !== '') {
    obj[fieldName] = obj.deptCode;
  } else if (obj.factoryCode && obj.factoryCode !== '') {
    obj[fieldName] = obj.factoryCode;
  } 

  return obj;
}