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
 * @Description: 备件主数据
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(value = {"source"})
@TableName("mes4_xuehua_part")
@Schema(description = "备件主数据")
public class XuehuaPart implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "主键", width = 15)
    private Integer id;

    @Schema(description = "租户ID")
    @TableField("tenant_id")
    private Integer tenantId;

    @Schema(description = "EMIS同步单号（来源标记）")
    @TableField("external_no")
    private String externalNo;

    @Schema(description = "数据来源 SELF/EMIS")
    @TableField("source")
    private String source;

    @Schema(description = "备件编号")
    @TableField("part_code")
    @Excel(name = "备件编号", width = 15)
    @NotBlank(message = "备件编号为空")
    private String partCode;

    @Schema(description = "备件名称")
    @TableField("part_name")
    @Excel(name = "备件名称", width = 20)
    @NotBlank(message = "备件名称为空")
    private String partName;

    @Schema(description = "型号/规格")
    @TableField("model")
    @Excel(name = "型号/规格", width = 15)
    private String model;

    @Schema(description = "类别")
    @TableField("category")
    @Excel(name = "类别", width = 15)
    private String category;

    @Schema(description = "单位")
    @TableField("unit")
    @Excel(name = "单位", width = 10)
    private String unit;

    @Schema(description = "安全库存")
    @TableField("safe_stock")
    @Excel(name = "安全库存", width = 10)
    private Integer safeStock;

    @Schema(description = "默认库区")
    @TableField("default_zone")
    @Excel(name = "默认库区", width = 10)
    private String defaultZone;

    @Schema(description = "状态 1启用 0停用")
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
