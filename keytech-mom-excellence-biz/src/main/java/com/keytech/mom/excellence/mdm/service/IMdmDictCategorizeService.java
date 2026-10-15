package com.keytech.mom.excellence.mdm.service;

import com.keytech.base.service.BaseService;
import com.keytech.mom.excellence.module.mdm.entity.MdmDictCategorize;
import org.jeecg.common.system.vo.SelectTreeModel;

import java.util.List;

/**
 * @Description: 字典分类
 */
public interface IMdmDictCategorizeService extends BaseService<MdmDictCategorize> {

    /**
     * 【vue3专用】根据pid查询子节点集合
     *
     * @param pid
     * @return
     */
    List<SelectTreeModel> queryListByPid(String pid);
}
