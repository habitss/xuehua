package com.keytech.xuehua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.keytech.xuehua.entity.XuehuaInbound;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @Description:  入库单 Mapper 接口
 */
@Mapper
public interface XuehuaInboundMapper extends BaseMapper<XuehuaInbound> {

    /**
     * 事务级咨询锁：串行化入库单号生成，事务提交/回滚自动释放（key=9701 专属入库）
     */
    @Select("SELECT pg_advisory_xact_lock(9701)")
    String advisoryLock();

    /**
     * 按单号计数（含软删除行）：单号生成防撞号用——唯一索引管全表，
     * 逻辑删除的"幽灵号"对 MP 自动过滤不可见但同样会撞唯一索引
     */
    @Select("SELECT count(*) FROM mes4_xuehua_inbound WHERE inbound_no = #{no}")
    long countNoAll(@Param("no") String no);
}
