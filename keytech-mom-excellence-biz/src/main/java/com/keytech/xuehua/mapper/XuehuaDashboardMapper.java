package com.keytech.xuehua.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 工作台（运营驾驶舱）聚合查询 Mapper
 *
 * <p>只放多表/窗口类原生 SQL——单表计数走各业务 Mapper 的 selectCount(QueryWrapper)。
 * 全部只读，不涉及事务；数据量小（百级单据），无需缓存。
 */
@Mapper
public interface XuehuaDashboardMapper {

    /** 近12个月出入库单量（后端补零序列，前端直接画折线） */
    @Select("SELECT to_char(d.m,'YYYY-MM') AS ym, " +
            " (SELECT count(*) FROM mes4_xuehua_inbound  i WHERE i.del_flag=0 AND to_char(i.create_time,'YYYY-MM')=to_char(d.m,'YYYY-MM')) AS cin, " +
            " (SELECT count(*) FROM mes4_xuehua_outbound o WHERE o.del_flag=0 AND to_char(o.create_time,'YYYY-MM')=to_char(d.m,'YYYY-MM')) AS cout " +
            "FROM generate_series(date_trunc('month', now()) - interval '11 month', date_trunc('month', now()), interval '1 month') AS d(m) " +
            "ORDER BY d.m")
    List<Map<String, Object>> monthlyTrend();

    /** 分区库存占比：各区总库位/在库数/占用率(%) */
    @Select("SELECT zone_code AS zone, count(*) AS total, " +
            " count(*) FILTER (WHERE status = 1) AS inStock, " +
            " ROUND(100.0 * count(*) FILTER (WHERE status = 1) / GREATEST(count(*), 1)) AS pct " +
            "FROM mes4_xuehua_bin WHERE del_flag = 0 GROUP BY zone_code ORDER BY zone_code")
    List<Map<String, Object>> zoneUsage();

    /** 库龄结构（口径：按已确认上架记录的上架时间距今分布） */
    @Select("SELECT CASE WHEN putaway_time >= now() - interval '30 day' THEN 'lt30' " +
            "            WHEN putaway_time >= now() - interval '60 day' THEN 'd30_60' " +
            "            WHEN putaway_time >= now() - interval '90 day' THEN 'd60_90' " +
            "            ELSE 'gt90' END AS bucket, count(*) AS cnt " +
            "FROM mes4_xuehua_putaway WHERE del_flag = 0 AND status = 1 AND putaway_time IS NOT NULL " +
            "GROUP BY bucket")
    List<Map<String, Object>> agingBuckets();

    /** 库龄超期（>90天未动）数量——KPI 卡用 */
    @Select("SELECT count(*) FROM mes4_xuehua_putaway WHERE del_flag = 0 AND status = 1 " +
            "AND putaway_time < now() - interval '90 day'")
    long countOverAged();

    /** 实时出入库信息：入库+出库合并取最新6条，入库单状态联动上架任务显示 待上架/已上架 */
    @Select("SELECT * FROM ( " +
            " SELECT '入库' AS type, i.inbound_no AS no, i.part_name AS partName, i.qty, " +
            "   COALESCE(i.operator, i.create_by) AS person, i.create_time AS t, " +
            "   CASE WHEN EXISTS (SELECT 1 FROM mes4_xuehua_putaway p WHERE p.del_flag=0 AND p.inbound_no=i.inbound_no AND p.status=1) THEN '已上架' " +
            "        WHEN EXISTS (SELECT 1 FROM mes4_xuehua_putaway p WHERE p.del_flag=0 AND p.inbound_no=i.inbound_no AND p.status=0) THEN '待上架' " +
            "        ELSE '已登记' END AS status " +
            " FROM mes4_xuehua_inbound i WHERE i.del_flag = 0 " +
            " UNION ALL " +
            " SELECT '出库', o.wo_no, o.part_name, o.qty, " +
            "   COALESCE(o.receive_user, o.apply_user, o.create_by), o.create_time, " +
            "   CASE o.status WHEN 2 THEN '已出库' WHEN 1 THEN '拣配中' ELSE '待拣配' END " +
            " FROM mes4_xuehua_outbound o WHERE o.del_flag = 0 " +
            ") t ORDER BY t DESC LIMIT 6")
    List<Map<String, Object>> liveFeed();

    /** 低位库存预警明细（口径=备件安全库存：在库+有备件归属+qty>0 且 qty<=COALESCE(safe_stock, 兜底阈值)） */
    @Select("SELECT b.bin_code AS \"binCode\", b.part_code AS \"partCode\", b.qty, " +
            " COALESCE(p.safe_stock, #{fallbackSafe}) AS \"safeStock\" " +
            "FROM mes4_xuehua_bin b " +
            "LEFT JOIN mes4_xuehua_part p ON p.part_code = b.part_code AND p.del_flag = 0 " +
            "WHERE b.del_flag = 0 AND b.status = 1 AND b.qty > 0 AND b.part_code IS NOT NULL " +
            " AND b.qty <= COALESCE(p.safe_stock, #{fallbackSafe}) " +
            "ORDER BY b.qty ASC LIMIT 5")
    List<Map<String, Object>> lowStockBins(@Param("fallbackSafe") int fallbackSafe);

    /** 低位库存预警计数（同上口径，今日待办卡用） */
    @Select("SELECT count(*) FROM mes4_xuehua_bin b " +
            "LEFT JOIN mes4_xuehua_part p ON p.part_code = b.part_code AND p.del_flag = 0 " +
            "WHERE b.del_flag = 0 AND b.status = 1 AND b.qty > 0 AND b.part_code IS NOT NULL " +
            " AND b.qty <= COALESCE(p.safe_stock, #{fallbackSafe})")
    long lowStockCount(@Param("fallbackSafe") int fallbackSafe);
}
