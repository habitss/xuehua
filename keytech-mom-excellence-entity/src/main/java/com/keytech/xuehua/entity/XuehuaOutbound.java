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
 * @Description: 出库工单（状态 0待拣配 1拣配中 2已出库）
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(value = {"source"})
@TableName("mes4_xuehua_outbound")
@Schema(description = "出库工单")
public class XuehuaOutbound implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "主键", width = 15)
    private Integer id;

    @Schema(description = "租户ID")
    @TableField("tenant_id")
    private Integer tenantId;

    @Schema(description = "工单号")
    @TableField("wo_no")
    @Excel(name = "工单号", width = 20)
    private String woNo;

    @Schema(description = "备件编号")
    @TableField("part_code")
    @Excel(name = "备件编号", width = 15)
    private String partCode;

    @Schema(description = "备件名称")
    @TableField("part_name")
    @Excel(name = "备件名称", width = 20)
    private String partName;

    @Schema(description = "类别")
    @TableField("category")
    @Excel(name = "类别", width = 15)
    private String category;

    @Schema(description = "数量")
    @TableField("qty")
    @Excel(name = "数量", width = 10)
    private Integer qty;

    @Schema(description = "申请人")
    @TableField("apply_user")
    @Excel(name = "申请人", width = 15)
    private String applyUser;

    @Schema(description = "领用人")
    @TableField("receive_user")
    @Excel(name = "领用人", width = 15)
    private String receiveUser;

    @Schema(description = "车间")
    @TableField("workshop")
    @Excel(name = "车间", width = 15)
    private String workshop;

    /**
     * 首拣库位（冗余展示）。2026-09-13 起领料单不填库位，由 WCS 按库龄优先 FIFO
     * 自动分配，实际拣配按明细行表 mes4_xuehua_outbound_detail（可能多库位）；
     * 本列存第一行的库位，供列表/导出/工作台展示兼容。
     */
    @Schema(description = "首拣库位(自动分配,冗余展示)")
    @TableField("bin_code")
    @Excel(name = "首拣库位", width = 15)
    private String binCode;

    /**
     * EMIS 领料单号（外部单号）。可空唯一索引：NULL 不冲突；EMIS 推送单必填，
     * 重复推送按此判重幂等返回原工单号。页面手工申请不填。
     */
    @Schema(description = "EMIS领料单号(外部单号,幂等判重)")
    @TableField("external_no")
    private String externalNo;

    @Schema(description = "状态 0待拣配 1拣配中 2已出库")
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
