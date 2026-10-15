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
 * 备件使用确认（备件使用报表 3.5.3）——出库明细行的用途跟踪
 *
 * <p>一行 = 一条已拣配出库明细。出库拣配确认后由 {@code syncFromOutbound}
 * 幂等生成（status=0 待确认）；领用人确认"已使用"（实际用量+安装照片+说明）
 * 或"未使用"；status != 1 且出库超 7 天为派生态"超1周未使用"（不落库）。
 * 头表/备件主数据字段随行快照（领用人/产线/类别以出库时刻为准，改名不断链）。
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(value = {"source"})
@TableName("mes4_xuehua_part_usage")
@Schema(description = "备件使用确认")
public class XuehuaPartUsage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "主键", width = 15)
    private Integer id;

    @Schema(description = "租户ID")
    @TableField("tenant_id")
    private Integer tenantId;

    @Schema(description = "出库头表id")
    @TableField("out_id")
    private Integer outId;

    @Schema(description = "出库明细行id（同步幂等键）")
    @TableField("detail_id")
    private Integer detailId;

    @Schema(description = "出库工单号")
    @TableField("wo_no")
    @Excel(name = "出库工单号", width = 20)
    private String woNo;

    @Schema(description = "备件编码")
    @TableField("part_code")
    @Excel(name = "备件编码", width = 15)
    private String partCode;

    @Schema(description = "备件名称")
    @TableField("part_name")
    @Excel(name = "备件名称", width = 20)
    private String partName;

    @Schema(description = "类别")
    @TableField("category")
    @Excel(name = "类别", width = 12)
    private String category;

    @Schema(description = "领用产线（车间快照）")
    @TableField("workshop")
    @Excel(name = "领用产线", width = 15)
    private String workshop;

    @Schema(description = "领用人")
    @TableField("receiver")
    @Excel(name = "领用人", width = 12)
    private String receiver;

    @Schema(description = "领用数量")
    @TableField("qty")
    @Excel(name = "领用数量", width = 10)
    private Integer qty;

    @Schema(description = "出库日期（拣配确认时间）")
    @TableField("out_time")
    @Excel(name = "出库日期", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    private Date outTime;

    @Schema(description = "实际使用数量")
    @TableField("used_qty")
    @Excel(name = "实际使用数量", width = 12)
    private Integer usedQty;

    @Schema(description = "用途/使用情况说明")
    @TableField("purpose")
    @Excel(name = "用途", width = 30)
    private String purpose;

    @Schema(description = "安装照片（逗号拼接url）")
    @TableField("photos")
    private String photos;

    @Schema(description = "状态 0待确认 1已使用 2未使用")
    @TableField("status")
    @Excel(name = "状态", width = 10)
    private Integer status;

    @Schema(description = "确认人")
    @TableField("confirm_by")
    @Excel(name = "确认人", width = 12)
    private String confirmBy;

    @Schema(description = "确认时间")
    @TableField("confirm_time")
    @Excel(name = "确认时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    private Date confirmTime;

    @Schema(description = "已提醒次数")
    @TableField("remind_count")
    private Integer remindCount;

    @Schema(description = "最近提醒时间")
    @TableField("last_remind_time")
    private Date lastRemindTime;

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
