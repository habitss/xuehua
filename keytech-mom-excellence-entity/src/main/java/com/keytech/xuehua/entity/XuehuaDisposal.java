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
 * 超期处置审批（库龄分析）——原型 agDisposal 的落库版
 *
 * <p>状态机：0待审批 → 1已通过 / 2已驳回；驳回后可重新发起（新行）。
 * 一期"已通过"即终态（处置执行与回传 Emis 由人工/二期流程承接，
 * 工单不联动库存——处置数量落地需盘点差异或专门端点，避免绕过台账原子扣减）。
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(value = {"source"})
@TableName("mes4_xuehua_disposal")
@Schema(description = "超期处置审批")
public class XuehuaDisposal implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "主键", width = 15)
    private Integer id;

    @Schema(description = "租户ID")
    @TableField("tenant_id")
    private Integer tenantId;

    @Schema(description = "超期库位")
    @TableField("bin_code")
    @Excel(name = "库位", width = 15)
    private String binCode;

    @Schema(description = "备件编码")
    @TableField("part_code")
    @Excel(name = "备件编码", width = 15)
    private String partCode;

    @Schema(description = "备件名称")
    @TableField("part_name")
    @Excel(name = "备件名称", width = 15)
    private String partName;

    @Schema(description = "发起时库龄天数")
    @TableField("aging_days")
    @Excel(name = "库龄天数", width = 10)
    private Integer agingDays;

    @Schema(description = "处置方式：报废/调拨他用/退库处置")
    @TableField("method")
    @Excel(name = "处置方式", width = 15)
    private String method;

    @Schema(description = "拟处置数量")
    @TableField("qty")
    @Excel(name = "处置数量", width = 10)
    private Integer qty;

    @Schema(description = "申请理由")
    @TableField("reason")
    @Excel(name = "申请理由", width = 30)
    private String reason;

    @Schema(description = "状态 0待审批 1已通过 2已驳回")
    @TableField("status")
    @Excel(name = "状态", width = 10)
    private Integer status;

    @Schema(description = "申请人")
    @TableField("applicant")
    @Excel(name = "申请人", width = 15)
    private String applicant;

    @Schema(description = "申请时间")
    @TableField("apply_time")
    @Excel(name = "申请时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    private Date applyTime;

    @Schema(description = "审批人")
    @TableField("approver")
    @Excel(name = "审批人", width = 15)
    private String approver;

    @Schema(description = "审批时间")
    @TableField("approve_time")
    @Excel(name = "审批时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;

    @Schema(description = "审批意见")
    @TableField("approve_note")
    @Excel(name = "审批意见", width = 30)
    private String approveNote;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @Schema(description = "是否删除 0-正常 删除-记录id")
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "id")
    private Integer delFlag;

    @Schema(description = "创建人")
    @TableField("create_by")
    private String createBy;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private Date createTime;

    @Schema(description = "更新人")
    @TableField("update_by")
    private String updateBy;

    @Schema(description = "更新时间")
    @TableField("update_time")
    private Date updateTime;
}
