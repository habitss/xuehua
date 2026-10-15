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
 * 出库拣配明细（库位行）——一张领料工单按"库龄优先 FIFO"拆分出的逐库位拣配行
 *
 * <p>背景（2026-09-13 用户口径）：领料单只带 备件+数量+人员，不填库位；
 * WCS 按库位级库龄（该库位备件最早一次确认上架时间 MIN putaway_time）升序分配，
 * 库龄大的先拣；单库位不够时拆多行。行级确认 = 行状态翻 1 + 该行库位台账原子扣减。
 * seq 即跨库位链式拣选顺序：拣完 seq=1 的库位，PTL 应答直接点亮 seq=2 的库位标签。
 *
 * <p>并发口径：申请分配时不预留库存（已知简化），最终由行确认的
 * 原子扣减 WHERE qty >= n 兜底，扣不到整体回滚。
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(value = {"source"})
@TableName("mes4_xuehua_outbound_detail")
@Schema(description = "出库拣配明细")
public class XuehuaOutboundDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "主键", width = 15)
    private Integer id;

    @Schema(description = "租户ID")
    @TableField("tenant_id")
    private Integer tenantId;

    @Schema(description = "所属出库工单id")
    @TableField("outbound_id")
    private Integer outboundId;

    @Schema(description = "工单号(冗余)")
    @TableField("wo_no")
    @Excel(name = "工单号", width = 20)
    private String woNo;

    @Schema(description = "拣配顺序 1..n")
    @TableField("seq")
    @Excel(name = "顺序", width = 8)
    private Integer seq;

    @Schema(description = "拣配库位")
    @TableField("bin_code")
    @Excel(name = "拣配库位", width = 15)
    private String binCode;

    @Schema(description = "备件编码")
    @TableField("part_code")
    @Excel(name = "备件编码", width = 15)
    private String partCode;

    @Schema(description = "该行拣配数量")
    @TableField("qty")
    @Excel(name = "拣配数量", width = 10)
    private Integer qty;

    @Schema(description = "状态 0待拣配 1已拣配")
    @TableField("status")
    @Excel(name = "行状态", width = 10)
    private Integer status;

    @Schema(description = "拣配人（页面确认=登录人，PTL按键=PTL按键）")
    @TableField("pick_by")
    private String pickBy;

    @Schema(description = "拣配时间")
    @TableField("pick_time")
    private Date pickTime;

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
