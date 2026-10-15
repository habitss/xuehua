package com.keytech.xuehua.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 库龄分析 Mapper（说明书 3.5.2：库龄分布/超期预警/分类分析）
 *
 * <p>库龄口径（用户拍板）：库位级最早一次确认上架距今（MIN putaway_time），
 * 与出库 FIFO 的库龄排序完全同源——在库明细一行=一个库位。
 *
 * <p>注意：MyBatis Map 结果不启用下划线转驼峰，且 PostgreSQL 会把未加引号的
 * 别名折叠成小写——所有驼峰别名必须写成 "binCode" 形式（双引号保形），
 * 否则 Controller 端 get("binCode") 拿到 null（首版 days 全 0 的真实踩坑）。
 */
@Mapper
public interface XuehuaAgingMapper {

    /**
     * 在库库龄明细行：bin 在库 + 该库位最早确认上架时间 + 备件主数据联查。
     * agingDays/status 由 Java 计算（天数依赖"现在"，落 SQL 不利测试与缓存）
     */
    @Select("SELECT b.bin_code AS \"binCode\", b.zone_code AS \"zoneCode\", b.rack_code AS \"rackCode\", " +
            " b.part_code AS \"partCode\", COALESCE(p2.part_name, b.part_code) AS \"partName\", " +
            " COALESCE(p2.category, '未分类') AS \"category\", b.qty AS \"qty\", " +
            " (SELECT MIN(p.putaway_time) FROM mes4_xuehua_putaway p " +
            "   WHERE p.del_flag = 0 AND p.status = 1 AND p.bin_code = b.bin_code) AS \"firstPutaway\" " +
            "FROM mes4_xuehua_bin b " +
            "LEFT JOIN mes4_xuehua_part p2 ON p2.del_flag = 0 AND p2.part_code = b.part_code " +
            "WHERE b.del_flag = 0 AND b.status <> 0 AND b.qty > 0 " +
            "ORDER BY \"firstPutaway\" ASC NULLS LAST, b.bin_code")
    List<Map<String, Object>> agingRows();

    /**
     * 分类分析：各类别在库件数/库位数/超期件数/超期库位数/最高库龄（天数在 SQL 内计算）
     */
    @Select("SELECT COALESCE(p2.category, '未分类') AS \"category\", " +
            " SUM(b.qty) AS \"pieces\", COUNT(*) AS \"bins\", " +
            " SUM(CASE WHEN ag.age_days > 90 THEN b.qty ELSE 0 END) AS \"overPieces\", " +
            " SUM(CASE WHEN ag.age_days > 90 THEN 1 ELSE 0 END) AS \"overBins\", " +
            " MAX(CASE WHEN ag.age_days IS NOT NULL THEN ag.age_days ELSE 0 END) AS \"maxDays\" " +
            "FROM mes4_xuehua_bin b " +
            "LEFT JOIN mes4_xuehua_part p2 ON p2.del_flag = 0 AND p2.part_code = b.part_code, " +
            "LATERAL (SELECT EXTRACT(DAY FROM now() - (SELECT MIN(p.putaway_time) FROM mes4_xuehua_putaway p" +
            "  WHERE p.del_flag = 0 AND p.status = 1 AND p.bin_code = b.bin_code)) AS age_days) ag " +
            "WHERE b.del_flag = 0 AND b.status <> 0 AND b.qty > 0 " +
            "GROUP BY COALESCE(p2.category, '未分类') ORDER BY \"pieces\" DESC")
    List<Map<String, Object>> byCategory();
}
