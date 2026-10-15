package com.keytech.mom.excellence.module.mdm.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jeecgframework.poi.excel.annotation.Excel;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 字典组表（业务字典-子类）
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(value = {"tenantId", "delFlag", "source"})
@TableName("mes4_mdm_dict_group")
@Schema(description = "字典组表")
public class MdmDictGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "主键", width = 15)
    private Integer id;

    @Schema(description = "租户ID")
    @TableField("tenant_id")
    private Integer tenantId;

    @Schema(description = "字典组名称")
    @TableField("dict_group_name")
    @Excel(name = "字典组名称", width = 15)
    @NotBlank(message = "字典组名称为空")
    private String dictGroupName;

    @Schema(description = "字典组编码")
    @TableField("dict_group_code")
    @Excel(name = "字典组编码", width = 15)
    @NotBlank(message = "字典组编码为空")
    @Pattern(regexp = "[A-Z]+((_[A-Z]+)+)?", message = "字典组编码格式错误,示例:AA或者AA_BB(大写)")
    private String dictGroupCode;

    @Schema(description = "字典分类")
    @TableField("categorize_id")
    @Excel(name = "字典分类", width = 15)
    @NotBlank(message = "字典分类为空")
    private String categorizeId;

    @Schema(description = "排序")
    @TableField("sort")
    @Excel(name = "排序", width = 15)
    private Integer sort;

    @Schema(description = "备注")
    @TableField("remark")
    @Excel(name = "备注", width = 15)
    private String remark;

    @Schema(description = "是否删除 0-正常 1-删除")
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
