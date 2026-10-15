package com.keytech.mom.excellence.mdm.service.impl;

import com.keytech.base.service.impl.BaseServiceImpl;
import com.keytech.mom.excellence.mdm.mapper.MdmDictCategorizeMapper;
import com.keytech.mom.excellence.mdm.service.IMdmDictCategorizeService;
import com.keytech.mom.excellence.module.mdm.entity.MdmDictCategorize;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description: 字典分类
 */
@Service
public class MdmDictCategorizeServiceImpl extends BaseServiceImpl<MdmDictCategorizeMapper, MdmDictCategorize> implements IMdmDictCategorizeService {

    public static final String ROOT_PID_VALUE = "0";

    @Override
    public List<SelectTreeModel> queryListByPid(String pid) {
        if (oConvertUtils.isEmpty(pid)) {
            pid = ROOT_PID_VALUE;
        }
        return baseMapper.queryListByPid(pid, null);
    }
}
