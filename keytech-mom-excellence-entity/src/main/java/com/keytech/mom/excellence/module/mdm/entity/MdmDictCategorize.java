package com.keytech.mom.excellence.module.mdm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 字典分类
 * @Author: jeecg-boot
 * @Date:   2025-01-14
 * @Version: V1.0
 */
@Data
@TableName("mes4_mdm_dict_categorize")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "字典分类")
public class MdmDictCategorize implements Serializable {
    private static final long serialVersionUID = 1L;

    /**id*/
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "id")
    private String id;

    /**租户ID*/
    @Excel(name = "租户ID", width = 15)
    @Schema(description = "租户ID")
    private Integer tenantId;

    /**工厂*/
    @Excel(name = "工厂", width = 15)
    @Schema(description = "工厂")
    private String factoryCode;

    /**部门*/
    @Excel(name = "部门", width = 15)
    @Schema(description = "部门")
    private String deptCode;

    /**产线*/
    @Excel(name = "产线", width = 15)
    @Schema(description = "产线")
    private String lineCode;

    /**工段*/
    @Excel(name = "工段", width = 15)
    @Schema(description = "工段")
    private String section;

    /**分类名称*/
    @Excel(name = "分类名称", width = 15)
    @Schema(description = "分类名称")
    private String categorizeName;

    /**父节点id*/
    @Excel(name = "父节点id", width = 15)
    @Schema(description = "父节点id")
    private String parentId;

    /**是否有子节点*/
    @Excel(name = "是否有子节点", width = 15)
    @Schema(description = "是否有子节点")
    private String hasChild;

    /**排序*/
    @Excel(name = "排序", width = 15)
    @Schema(description = "排序")
    private Double sort;

    /**备注说明*/
    @Excel(name = "备注说明", width = 15)
    @Schema(description = "备注说明")
    private String remark;

    /**删除标识*/
    @Excel(name = "删除标识", width = 15)
    @Schema(description = "删除标识")
    @TableLogic
    private Integer delFlag;

    /**创建人*/
    @Schema(description = "创建人")
    private String createBy;

    /**创建时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    /**更新人*/
    @Schema(description = "更新人")
    private String updateBy;

    /**更新时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;
}
