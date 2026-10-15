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
 * 人员注册（监控管理）——出入库作业人员的身份台账
 *
 * <p>业务位置：EMIS 到货单/领料单携带操作人（工号），WCS 按 emp_no 匹配本表：
 * 未注册 → 拒绝单据；已注册 → 推送海康门禁授权（该人才能刷脸/刷卡进仓作业）。
 * hik_person_id / hik_auth_status 是海康 OpenAPI 推送后的回写
 * （hik.enabled=false 降级模式下保持 0 待推送，联调时全量补推）。
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(value = {"source"})
@TableName("mes4_xuehua_person")
@Schema(description = "人员注册")
public class XuehuaPerson implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "主键", width = 15)
    private Integer id;

    @Schema(description = "租户ID")
    @TableField("tenant_id")
    private Integer tenantId;

    @Schema(description = "姓名")
    @TableField("name")
    @Excel(name = "姓名", width = 15)
    private String name;

    @Schema(description = "工号（EMIS 操作人匹配键，唯一）")
    @TableField("emp_no")
    @Excel(name = "工号", width = 15)
    private String empNo;

    @Schema(description = "部门")
    @TableField("dept")
    @Excel(name = "部门", width = 15)
    private String dept;

    @Schema(description = "识别方式：人脸/人脸+IC卡/IC卡")
    @TableField("auth_method")
    @Excel(name = "识别方式", width = 15)
    private String authMethod;

    @Schema(description = "人脸库 1已采集/0未采集/2采集失败")
    @TableField("face_status")
    @Excel(name = "人脸库", width = 10)
    private Integer faceStatus;

    @Schema(description = "绑定卡号")
    @TableField("card_no")
    @Excel(name = "卡号", width = 15)
    private String cardNo;

    @Schema(description = "授权区域")
    @TableField("zones")
    @Excel(name = "授权区域", width = 15)
    private String zones;

    @Schema(description = "状态 1启用/0停用")
    @TableField("status")
    @Excel(name = "状态", width = 10)
    private Integer status;

    @Schema(description = "海康侧人员ID")
    @TableField("hik_person_id")
    private String hikPersonId;

    @Schema(description = "海康门禁授权 0未推送/1已授权/2失败")
    @TableField("hik_auth_status")
    @Excel(name = "海康授权", width = 10)
    private Integer hikAuthStatus;

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
