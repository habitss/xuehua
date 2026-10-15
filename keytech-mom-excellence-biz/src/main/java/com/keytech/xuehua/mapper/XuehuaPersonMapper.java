package com.keytech.xuehua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.keytech.xuehua.entity.XuehuaPerson;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人员注册 Mapper：纯 BaseMapper 通用 CRUD。
 * emp_no 唯一性由部分唯一索引（del_flag=0）兜底，重复注册抛 DuplicateKeyException。
 */
@Mapper
public interface XuehuaPersonMapper extends BaseMapper<XuehuaPerson> {
}
