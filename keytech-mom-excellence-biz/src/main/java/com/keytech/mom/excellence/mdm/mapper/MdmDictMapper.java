package com.keytech.mom.excellence.mdm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.keytech.mom.excellence.module.mdm.entity.MdmDict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Description: 字典表 Mapper 接口
 */
@Mapper
public interface MdmDictMapper extends BaseMapper<MdmDict> {

    /**
     * 查询全部或按多个字典组编码查询
     *
     * @param groupCodes 为空时查询全部
     * @return
     */
    List<MdmDict> getDictAllOrByGroupCode(@Param("groupCodes") List<String> groupCodes);
}
