package com.keytech.mom.excellence.mdm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.keytech.mom.excellence.module.mdm.entity.MdmDictCategorize;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.common.system.vo.SelectTreeModel;

import java.util.List;
import java.util.Map;

/**
 * @Description: 字典分类
 */
@Mapper
public interface MdmDictCategorizeMapper extends BaseMapper<MdmDictCategorize> {

    /**
     * 【vue3专用】根据父级ID查询树节点数据
     *
     * @param pid
     * @param query
     * @return
     */
    List<SelectTreeModel> queryListByPid(@Param("pid") String pid, @Param("query") Map<String, String> query);
}
