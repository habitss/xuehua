package com.keytech.mom.excellence.mdm.service.impl;

import com.keytech.base.service.impl.BaseServiceImpl;
import com.keytech.mom.excellence.mdm.mapper.MdmDictMapper;
import com.keytech.mom.excellence.mdm.service.IMdmDictService;
import com.keytech.mom.excellence.module.mdm.entity.MdmDict;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description: 字典表 服务实现类
 */
@Service
public class MdmDictServiceImpl extends BaseServiceImpl<MdmDictMapper, MdmDict> implements IMdmDictService {

    @Resource
    private MdmDictMapper mdmDictMapper;

    @Override
    public List<MdmDict> getDictAllOrByGroupCode(List<String> groupCodes) {
        return mdmDictMapper.getDictAllOrByGroupCode(groupCodes);
    }
}
