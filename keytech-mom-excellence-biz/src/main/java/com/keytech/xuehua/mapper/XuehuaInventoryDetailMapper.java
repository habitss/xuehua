package com.keytech.xuehua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.keytech.xuehua.entity.XuehuaInventoryDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 盘点明细 Mapper：纯 BaseMapper 通用 CRUD，无自定义 SQL。
 * 差异应用到台账的原子 UPDATE 写在 XuehuaInventoryServiceImpl（UpdateWrapper.setSql 算术），
 * 不放这里——它属于业务编排，且要和盘点单状态更新同事务。
 */
@Mapper
public interface XuehuaInventoryDetailMapper extends BaseMapper<XuehuaInventoryDetail> {
}
