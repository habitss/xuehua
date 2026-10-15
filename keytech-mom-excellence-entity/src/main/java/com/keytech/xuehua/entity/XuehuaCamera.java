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
 * 视频点位（监控管理）——库区/货架/库位 ↔ 海康设备通道 绑定
 *
 * <p>对接对象 = 视频AI服务器（16盘位边缘计算主机，自带以文搜图/以图搜图）+ NVR/摄像机；
 * 无 iSecure Center（设备选型 0914 已移除 ISC），协议走 ISAPI（Digest 认证）+ RTSP 取流。
 * 绑定层级：0 库区（zone）→ 1 货架（zone+rack）→ 2 库位（精确 bin），供视频搜索/回放按点位取流。
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(value = {"source"})
@TableName("mes4_xuehua_camera")
@Schema(description = "视频点位")
public class XuehuaCamera implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "主键", width = 15)
    private Integer id;

    @Schema(description = "租户ID")
    @TableField("tenant_id")
    private Integer tenantId;

    @Schema(description = "点位编码（唯一）")
    @TableField("camera_code")
    @Excel(name = "点位编码", width = 18)
    private String cameraCode;

    @Schema(description = "点位名称")
    @TableField("camera_name")
    @Excel(name = "点位名称", width = 20)
    private String cameraName;

    @Schema(description = "设备类型 0视频AI服务器 1NVR 2摄像机直连")
    @TableField("device_type")
    @Excel(name = "设备类型", width = 14)
    private Integer deviceType;

    @Schema(description = "设备IP")
    @TableField("device_ip")
    @Excel(name = "设备IP", width = 16)
    private String deviceIp;

    @Schema(description = "通道号")
    @TableField("channel_no")
    @Excel(name = "通道号", width = 8)
    private Integer channelNo;

    @Schema(description = "设备侧通道编号（ISAPI）")
    @TableField("index_code")
    private String indexCode;

    @Schema(description = "RTSP端口")
    @TableField("rtsp_port")
    private Integer rtspPort;

    @Schema(description = "绑定层级 0库区 1货架 2库位")
    @TableField("location_type")
    @Excel(name = "绑定层级", width = 10)
    private Integer locationType;

    @Schema(description = "库区编码")
    @TableField("zone_code")
    @Excel(name = "库区", width = 8)
    private String zoneCode;

    @Schema(description = "货架编码")
    @TableField("rack_code")
    @Excel(name = "货架", width = 10)
    private String rackCode;

    @Schema(description = "库位编码（层级=库位时）")
    @TableField("bin_code")
    @Excel(name = "库位", width = 12)
    private String binCode;

    @Schema(description = "状态 1启用 0停用")
    @TableField("status")
    @Excel(name = "状态", width = 8)
    private Integer status;

    @Schema(description = "备注")
    @TableField("remark")
    @Excel(name = "备注", width = 24)
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
