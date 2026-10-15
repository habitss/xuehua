package com.keytech.xuehua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.keytech.xuehua.entity.XuehuaOutboundDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出库拣配明细 Mapper：纯 BaseMapper 通用 CRUD，无自定义 SQL。
 * 行确认的台账原子扣减写在 XuehuaOutboundServiceImpl（UpdateWrapper.setSql 算术），
 * 与行状态/头状态更新同事务。
 */
@Mapper
public interface XuehuaOutboundDetailMapper extends BaseMapper<XuehuaOutboundDetail> {
}
