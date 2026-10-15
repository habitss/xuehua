package com.keytech.mom.excellence.mdm.service;

import com.keytech.base.service.BaseService;
import com.keytech.mom.excellence.module.mdm.entity.MdmDict;

import java.util.List;

/**
 * @Description: 字典表 服务接口
 */
public interface IMdmDictService extends BaseService<MdmDict> {

    /**
     * 查询全部或按多个字典组编码查询
     *
     * @param groupCodes 为空时查询全部
     * @return
     */
    List<MdmDict> getDictAllOrByGroupCode(List<String> groupCodes);
}
