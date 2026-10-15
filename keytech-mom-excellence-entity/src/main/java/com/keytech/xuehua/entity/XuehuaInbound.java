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
 * @Description: 入库单（登记域：0待登记 1已登记；三要素=送货单/全景照片/检验报告，登记提交必填）
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(value = {"source"})
@TableName("mes4_xuehua_inbound")
@Schema(description = "入库单")
public class XuehuaInbound implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "主键", width = 15)
    private Integer id;

    @Schema(description = "租户ID")
    @TableField("tenant_id")
    private Integer tenantId;

    @Schema(description = "入库单号")
    @TableField("inbound_no")
    @Excel(name = "入库单号", width = 20)
    private String inboundNo;

    @Schema(description = "采购单号")
    @TableField("po_no")
    @Excel(name = "采购单号", width = 20)
    private String poNo;

    @Schema(description = "备件编号")
    @TableField("part_code")
    @Excel(name = "备件编号", width = 15)

    private String partCode;

    @Schema(description = "备件名称")
    @TableField("part_name")
    @Excel(name = "备件名称", width = 20)
    private String partName;

    @Schema(description = "型号/规格")
    @TableField("model")
    @Excel(name = "型号/规格", width = 15)
    private String model;

    @Schema(description = "数量")
    @TableField("qty")
    @Excel(name = "数量", width = 10)
    private Integer qty;

    @Schema(description = "单位")
    @TableField("unit")
    @Excel(name = "单位", width = 10)
    private String unit;

    @Schema(description = "库位编码")
    @TableField("bin_code")
    @Excel(name = "库位", width = 15)
    private String binCode;

    @Schema(description = "送货单号")
    @TableField("delivery_no")
    @Excel(name = "送货单号", width = 20)
    private String deliveryNo;

    @Schema(description = "全景照片")
    @TableField("photo")
    @Excel(name = "全景照片", width = 20)
    private String photo;

    @Schema(description = "检验报告(PDF)")
    @TableField("report")
    @Excel(name = "检验报告", width = 20)
    private String report;

    @Schema(description = "操作人")
    @TableField("operator")
    @Excel(name = "操作人", width = 15)
    private String operator;

    @Schema(description = "状态 0待登记 1已登记")
    @TableField("status")
    @Excel(name = "状态", width = 10)
    private Integer status;

    @Schema(description = "备注")
    @TableField("remark")
    @Excel(name = "备注", width = 15)
    private String remark;

    /**
     * EMIS 到货单号（外部单号）。可空唯一索引：EMIS 推送必填、幂等判重防重推；
     * 页面手工登记不填。
     */
    @Schema(description = "EMIS到货单号(幂等判重)")
    @TableField("external_no")
    private String externalNo;

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
