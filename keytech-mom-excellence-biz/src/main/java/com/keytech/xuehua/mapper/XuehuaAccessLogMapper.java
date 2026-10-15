package com.keytech.xuehua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.keytech.xuehua.entity.XuehuaAccessLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 进出流水 Mapper：纯 BaseMapper 通用 CRUD（列表检索用 QueryWrapper 组合即可）。
 */
@Mapper
public interface XuehuaAccessLogMapper extends BaseMapper<XuehuaAccessLog> {
}
