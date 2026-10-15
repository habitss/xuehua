package com.keytech.xuehua.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jeecgframework.poi.excel.annotation.Excel;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 库位（编码=货架-层-位，如 A-01-03）
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(value = {"source"})
@TableName("mes4_xuehua_bin")
@Schema(description = "库位")
public class XuehuaBin implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "主键", width = 15)
    private Integer id;

    @Schema(description = "租户ID")
    @TableField("tenant_id")
    private Integer tenantId;

    @Schema(description = "库位编码")
    @TableField("bin_code")
    @Excel(name = "库位编码", width = 15)
    @NotBlank(message = "库位编码为空")
    private String binCode;

    @Schema(description = "所属区域编码")
    @TableField("zone_code")
    @Excel(name = "所属区域编码", width = 10)
    private String zoneCode;

    @Schema(description = "所属区域名称")
    @TableField("zone_name")
    @Excel(name = "所属区域名称", width = 15)
    private String zoneName;

    @Schema(description = "所属货架")
    @TableField("rack_code")
    @Excel(name = "所属货架", width = 10)
    private String rackCode;

    @Schema(description = "层")
    @TableField("layer")
    @Excel(name = "层", width = 10)
    private Integer layer;

    @Schema(description = "位")
    @TableField("col_no")
    @Excel(name = "位", width = 10)
    private Integer colNo;

    @Schema(description = "PTL电子标签地址(接口文档 tagAdd)，NULL=未绑定")
    @TableField("tag_add")
    @Excel(name = "PTL标签地址", width = 10)
    private Integer tagAdd;

    @Schema(description = "状态 0空位 1在库 2拣配中 3报警 4超大件存放")
    @TableField("status")
    @Excel(name = "状态", width = 15)
    private Integer status;

    /**
     * 当前存放的备件编码。口径：一个库位只存一种备件（PTL 标签按库位点亮，屏显单一物料）。
     * 上架确认时写入；出库清零时置 NULL。混装在 XuehuaPutawayServiceImpl 上架确认处拦截。
     */
    @Schema(description = "当前存放备件编码(空=无货)")
    @TableField("part_code")
    @Excel(name = "备件编码", width = 15)
    private String partCode;

    /**
     * 当前库存数量（实物台账）。联动规则——
     * 上架确认：qty += 上架数量（XuehuaPutawayServiceImpl，原子 SQL 累加）；
     * 出库确认：qty -= 领用数量，扣到 0 库位状态才回"空位"（XuehuaOutboundServiceImpl，
     * WHERE qty >= n 原子扣减防负库存）。盘点差异处理是未来的第三个联动入口（暂未接）。
     * 注意：qty 与 status 必须一起改（同一条 UPDATE），不能分开两次写，否则并发下状态与数量会错位。
     */
    @Schema(description = "当前库存数量")
    @TableField("qty")
    @Excel(name = "库存数量", width = 10)
    private Integer qty;

    @Schema(description = "备注")
    @TableField("remark")
    @Excel(name = "备注", width = 15)
    private String remark;

    @Schema(description = "是否删除 0-正常 删除-记录id")
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "id")
    private Integer delFlag;

    @Schema(description = "创建人")
    @TableField("create_by")
    @Excel(name = "创建人", width = 15)
    private String createBy;

    @Schema(description = "创建时间")
    @TableField("create_time")
    @Excel(name = "创建时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "更新人")
    @TableField("update_by")
    @Excel(name = "更新人", width = 15)
    private String updateBy;

    @Schema(description = "更新时间")
    @TableField("update_time")
    @Excel(name = "更新时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
