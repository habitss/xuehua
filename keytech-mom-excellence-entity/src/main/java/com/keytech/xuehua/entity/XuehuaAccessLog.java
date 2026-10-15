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
 * 进出/作业时间线流水（监控管理）——海康门禁事件、PTL 确认、模拟注入统一落本表
 *
 * <p业务价值：一份流水串起"进仓 → 放置/拣配确认 → 出仓"完整作业时间线，
 * 每个事件带 camera_index（海康点位），页面按人/时间/事件检索并提供
 * 回放入口（海康 7×24 自录，WCS 只记时间线不控制录像——用户确认的口径）。
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(value = {"source"})
@TableName("mes4_xuehua_access_log")
@Schema(description = "进出记录")
public class XuehuaAccessLog implements Serializable {

    /** 事件类型：进仓 */
    public static final int EVT_IN = 1;
    /** 事件类型：出仓 */
    public static final int EVT_OUT = 2;
    /** 事件类型：放置确认（入库 PTL OKKey） */
    public static final int EVT_PUTAWAY = 3;
    /** 事件类型：拣配确认（出库 PTL OKKey） */
    public static final int EVT_PICK = 4;
    /** 事件类型：非授权进入 */
    public static final int EVT_UNAUTHORIZED = 5;
    /** 事件类型：尾随进入 */
    public static final int EVT_TAILGATE = 6;

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "主键", width = 15)
    private Integer id;

    @Schema(description = "租户ID")
    @TableField("tenant_id")
    private Integer tenantId;

    @Schema(description = "注册人员id（未匹配为空）")
    @TableField("person_id")
    private Integer personId;

    @Schema(description = "人员姓名快照")
    @TableField("person_name")
    @Excel(name = "人员", width = 15)
    private String personName;

    @Schema(description = "工号")
    @TableField("emp_no")
    @Excel(name = "工号", width = 15)
    private String empNo;

    @Schema(description = "部门")
    @TableField("dept")
    @Excel(name = "部门", width = 15)
    private String dept;

    @Schema(description = "事件类型 1进仓/2出仓/3放置确认/4拣配确认/5非授权/6尾随")
    @TableField("event_type")
    @Excel(name = "事件类型", width = 10)
    private Integer eventType;

    @Schema(description = "区域/门点位")
    @TableField("zone")
    @Excel(name = "区域", width = 10)
    private String zone;

    @Schema(description = "识别方式：人脸/IC卡/AI抓拍/模拟")
    @TableField("method")
    @Excel(name = "识别方式", width = 10)
    private String method;

    @Schema(description = "方向 in/out")
    @TableField("direction")
    private String direction;

    @Schema(description = "来源：hik/ptl/mock")
    @TableField("source")
    private String source;

    @Schema(description = "关联单据号")
    @TableField("biz_no")
    @Excel(name = "关联单据", width = 15)
    private String bizNo;

    @Schema(description = "海康点位编码")
    @TableField("camera_index")
    private String cameraIndex;

    @Schema(description = "回放地址")
    @TableField("video_url")
    private String videoUrl;

    @Schema(description = "事件时间")
    @TableField("event_time")
    @Excel(name = "事件时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    private Date eventTime;

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
