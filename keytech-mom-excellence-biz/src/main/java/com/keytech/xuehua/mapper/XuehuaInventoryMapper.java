package com.keytech.xuehua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.keytech.xuehua.entity.XuehuaInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @Description: 盘点单 Mapper 接口
 */
@Mapper
public interface XuehuaInventoryMapper extends BaseMapper<XuehuaInventory> {

    /**
     * 事务级咨询锁：串行化盘点单号生成，事务提交/回滚自动释放（key=9703 专属盘点）
     */
    @Select("SELECT pg_advisory_xact_lock(9703)")
    String advisoryLock();

    /**
     * 按盘点单号计数（含软删除行）：单号生成防撞号用——唯一索引管全表，
     * 逻辑删除的"幽灵号"对 MP 自动过滤不可见但同样会撞唯一索引
     */
    @Select("SELECT count(*) FROM mes4_xuehua_inventory WHERE pv_no = #{no}")
    long countNoAll(@Param("no") String no);
}
