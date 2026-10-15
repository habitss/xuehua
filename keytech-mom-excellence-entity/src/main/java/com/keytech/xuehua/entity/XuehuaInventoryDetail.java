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
 * 盘点明细（库位级）——一个盘点单按范围生成的逐库位盘点行
 *
 * <p>生命周期：盘点开始后 generateDetails 生成（book_qty=当时 bin.qty 快照）
 * → 现场清点后 submitReal 录实盘数（diff_qty=real-book）
 * → applyDifferences 差异处置：把 diff 原子加到 bin.qty 并标记 applied=1。
 * applied 标记防重复应用——同一条差异只能进一次台账。
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(value = {"source"})
@TableName("mes4_xuehua_inventory_detail")
@Schema(description = "盘点明细")
public class XuehuaInventoryDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "主键", width = 15)
    private Integer id;

    @Schema(description = "租户ID")
    @TableField("tenant_id")
    private Integer tenantId;

    @Schema(description = "所属盘点单id")
    @TableField("inventory_id")
    private Integer inventoryId;

    @Schema(description = "盘点单号(冗余)")
    @TableField("pv_no")
    @Excel(name = "盘点单号", width = 20)
    private String pvNo;

    @Schema(description = "盘点库位")
    @TableField("bin_code")
    @Excel(name = "库位", width = 15)
    private String binCode;

    @Schema(description = "库位备件编码")
    @TableField("part_code")
    @Excel(name = "备件编码", width = 15)
    private String partCode;

    @Schema(description = "账面数(生成时快照)")
    @TableField("book_qty")
    @Excel(name = "账面数", width = 10)
    private Integer bookQty;

    @Schema(description = "实盘数")
    @TableField("real_qty")
    @Excel(name = "实盘数", width = 10)
    private Integer realQty;

    @Schema(description = "差异 = 实盘 - 账面")
    @TableField("diff_qty")
    @Excel(name = "差异", width = 10)
    private Integer diffQty;

    @Schema(description = "差异已应用台账 0否/1是")
    @TableField("applied")
    private Integer applied;

    @Schema(description = "是否删除")
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
