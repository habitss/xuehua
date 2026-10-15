package com.keytech.xuehua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.keytech.xuehua.entity.XuehuaOutbound;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 出库工单 Mapper 接口
 *
 * <p>业务 CRUD 走 MyBatis-Plus BaseMapper 通用方法（无 XML）；
 * 本接口只声明两个并发防护专用的原生 SQL：
 * 咨询锁（串行化工单号生成）与全表计数（防"幽灵号"撞唯一索引）。
 */
@Mapper
public interface XuehuaOutboundMapper extends BaseMapper<XuehuaOutbound> {

    /**
     * 事务级咨询锁：串行化出库工单号生成（防撞号第一层）
     *
     * <p>pg_advisory_xact_lock(9702) 语义：
     * <ul>
     *   <li>9702 是出库专属的锁命名空间（bigint key），与入库 9701 / 盘点 9703 互不干扰；
     *       同库不同业务的单号生成各自串行，互不排队；</li>
     *   <li>"事务级"=锁绑定在当前事务上，commit 或 rollback 时 PostgreSQL 自动释放，
     *       不需要（也不能）手工 unlock，绝不会出现忘记释放导致后续请求永久阻塞；</li>
     *   <li>同事务内重复获取同一把锁是幂等的（PG 只记录一次）；</li>
     *   <li>返回值无意义（恒返回 null），签名声明 String 只是因为
     *       MyBatis @Select 不允许 void 返回类型。</li>
     * </ul>
     * 调用位置：XuehuaOutboundServiceImpl.addOutbound 事务内的最前面，
     * 保证"查重→定号→insert"整段临界区同时只有一个事务在跑。
     */
    @Select("SELECT pg_advisory_xact_lock(9702)")
    String advisoryLock();

    /**
     * 按工单号全表计数（含软删除行）——工单号生成查重用（防撞号第二层）
     *
     * <p>为什么不用 this.count(wrapper) 而写原生 SQL：
     * 实体带 @TableLogic 逻辑删除，MP 自动给所有查询追加 del_flag=0 过滤，
     * 软删除的工单对查重"不可见"；但 wo_no 唯一索引管的是物理全表。
     * 于是出现"查重说没用过 → insert 却撞唯一索引"的幽灵号问题
     * （真实案例：WO-2026-0009 软删除后，下一个请求重新生成同号插入失败）。
     * 本 SQL 不过滤 del_flag，把删掉的号也数进来，查重视角与唯一索引对齐。
     */
    @Select("SELECT count(*) FROM mes4_xuehua_outbound WHERE wo_no = #{no}")
    long countNoAll(@Param("no") String no);
}
