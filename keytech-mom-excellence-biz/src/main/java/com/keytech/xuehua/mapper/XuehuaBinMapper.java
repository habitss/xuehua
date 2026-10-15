package com.keytech.xuehua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.keytech.xuehua.entity.XuehuaBin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * @Description: 库位 Mapper 接口
 */
@Mapper
public interface XuehuaBinMapper extends BaseMapper<XuehuaBin> {

    /**
     * 库位地图行：联备件主数据带出名称与安全库存（PG 别名双引号保驼峰——MyBatis Map 不转驼峰的坑）。
     * lowStock 在 Service 侧装饰（qty>0 且 qty<=安全库存，仅对有备件归属的在库库位判定）。
     */
    @Select("SELECT b.id, b.bin_code AS \"binCode\", b.zone_code AS \"zoneCode\", b.zone_name AS \"zoneName\", " +
            " b.rack_code AS \"rackCode\", b.layer, b.col_no AS \"colNo\", b.status, b.part_code AS \"partCode\", " +
            " b.qty, b.tag_add AS \"tagAdd\", p.part_name AS \"partName\", p.safe_stock AS \"safeStock\" " +
            "FROM mes4_xuehua_bin b " +
            "LEFT JOIN mes4_xuehua_part p ON p.part_code = b.part_code AND p.del_flag = 0 " +
            "WHERE b.del_flag = 0 ORDER BY b.bin_code")
    List<Map<String, Object>> mapRows();

    /** 字典项取值：安全库存预警配置（item_text 精确匹配），无配置返回 null 由调用方兜底 */
    @Select("SELECT di.item_value FROM sys_dict d JOIN sys_dict_item di ON di.dict_id = d.id " +
            "WHERE d.dict_code = #{dictCode} AND d.del_flag = 0 AND di.status = 1 AND di.item_text = #{itemText} " +
            "ORDER BY di.sort_order LIMIT 1")
    String dictItemValue(@Param("dictCode") String dictCode, @Param("itemText") String itemText);
}
