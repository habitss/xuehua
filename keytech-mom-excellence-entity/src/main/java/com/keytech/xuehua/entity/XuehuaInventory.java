package com.keytech.xuehua.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jeecgframework.poi.excel.annotation.Excel;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 盘点单（状态 0待盘点 1盘点中 2有差异 3已完成 4已终止；stage 0待开始~6完成）
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(value = {"source"})
@TableName("mes4_xuehua_inventory")
@Schema(description = "盘点单")
public class XuehuaInventory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "主键", width = 15)
    private Integer id;

    @Schema(description = "租户ID")
    @TableField("tenant_id")
    private Integer tenantId;

    @Schema(description = "EMIS下发单号（幂等键）")
    @TableField("external_no")
    private String externalNo;

    @Schema(description = "盘点单号")
    @TableField("pv_no")
    @Excel(name = "盘点单号", width = 20)
    private String pvNo;

    @Schema(description = "来源")
    @TableField("source")
    @Excel(name = "来源", width = 15)
    private String source;

    @Schema(description = "盘点区域")
    @TableField("zone_code")
    @Excel(name = "盘点区域", width = 10)
    private String zoneCode;

    @Schema(description = "盘点范围")
    @TableField("scope")
    @Excel(name = "盘点范围", width = 15)
    private String scope;

    @Schema(description = "执行人")
    @TableField("executor")
    @Excel(name = "执行人", width = 15)
    private String executor;

    @Schema(description = "识别方式")
    @TableField("identify_type")
    @Excel(name = "识别方式", width = 15)
    private String identifyType;

    @Schema(description = "账面数")
    @TableField("book_qty")
    @Excel(name = "账面数", width = 10)
    private Integer bookQty;

    @Schema(description = "实盘数")
    @TableField("real_qty")
    @Excel(name = "实盘数", width = 10)
    private Integer realQty;

    @Schema(description = "进度(百分比)")
    @TableField("progress")
    @Excel(name = "进度", width = 10)
    private Integer progress;

    @Schema(description = "阶段 0待开始 1期初账面 2入库核实 3出库核对 4期末账面 5差异处理 6完成")
    @TableField("stage")
    @Excel(name = "阶段", width = 10)
    private Integer stage;

    @Schema(description = "库存冻结 0正常 1冻结")
    @TableField("frozen_flag")
    @Excel(name = "库存冻结", width = 10)
    private Integer frozenFlag;

    @Schema(description = "状态 0待盘点 1盘点中 2有差异 3已完成 4已终止")
    @TableField("status")
    @Excel(name = "状态", width = 10)
    private Integer status;

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
