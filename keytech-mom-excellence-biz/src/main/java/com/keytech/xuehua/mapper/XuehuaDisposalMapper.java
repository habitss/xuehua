package com.keytech.xuehua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.keytech.xuehua.entity.XuehuaDisposal;
import org.apache.ibatis.annotations.Mapper;

/**
 * 超期处置审批 Mapper：纯 BaseMapper（查询走 QueryGenerator/QueryWrapper）
 */
@Mapper
public interface XuehuaDisposalMapper extends BaseMapper<XuehuaDisposal> {
}
