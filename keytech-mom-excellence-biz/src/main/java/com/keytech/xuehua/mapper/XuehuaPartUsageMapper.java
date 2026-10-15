package com.keytech.xuehua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.keytech.xuehua.entity.XuehuaPartUsage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

/**
 * 备件使用确认 Mapper
 */
public interface XuehuaPartUsageMapper extends BaseMapper<XuehuaPartUsage> {

    /**
     * 幂等回填：已拣配明细行未生成使用记录的补一条（status=0 待确认）。
     * 唯一索引 uk_xuehua_usage_detail(detail_id) 兜底防并发双插。
     *
     * @return 本次新补行数
     */
    @Insert("INSERT INTO mes4_xuehua_part_usage (out_id, detail_id, wo_no, part_code, part_name, category, workshop, receiver, qty, out_time, status, create_time) "
            + "SELECT o.id, d.id, d.wo_no, d.part_code, COALESCE(o.part_name, d.part_code), o.category, o.workshop, o.receive_user, d.qty, d.pick_time, 0, now() "
            + "FROM mes4_xuehua_outbound_detail d "
            + "JOIN mes4_xuehua_outbound o ON o.id = d.outbound_id "
            + "WHERE d.del_flag = 0 AND d.status = 1 AND d.pick_time IS NOT NULL "
            + "AND NOT EXISTS (SELECT 1 FROM mes4_xuehua_part_usage u WHERE u.detail_id = d.id AND u.del_flag = 0)")
    int insertFromOutbound();

    /** 查重用含软删除计数（防"幽灵 detail_id"，与单号防撞同款口径） */
    @Select("SELECT COUNT(*) FROM mes4_xuehua_part_usage WHERE detail_id = #{detailId}")
    int countNoAllByDetailId(Integer detailId);
}
