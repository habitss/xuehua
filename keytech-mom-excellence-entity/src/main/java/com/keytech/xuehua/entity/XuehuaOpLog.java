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
 * @Description: 操作日志（审计）：外部回调/关键业务留痕
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(value = {"source"})
@TableName("mes4_xuehua_op_log")
@Schema(description = "操作日志")
public class XuehuaOpLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "主键", width = 15)
    private Integer id;

    @Schema(description = "租户ID")
    @TableField("tenant_id")
    private Integer tenantId;

    @Schema(description = "业务类型：PTL_CALLBACK 等")
    @TableField("biz_type")
    @Excel(name = "业务类型", width = 15)
    private String bizType;

    @Schema(description = "业务单号")
    @TableField("biz_no")
    @Excel(name = "业务单号", width = 15)
    private String bizNo;

    @Schema(description = "动作：TAG_OKKey 等")
    @TableField("op_action")
    @Excel(name = "动作", width = 15)
    private String opAction;

    @Schema(description = "操作来源：PTL网关/登录人")
    @TableField("op_user")
    @Excel(name = "操作来源", width = 15)
    private String opUser;

    @Schema(description = "来源IP")
    @TableField("op_ip")
    @Excel(name = "来源IP", width = 15)
    private String opIp;

    @Schema(description = "请求报文")
    @TableField("req_body")
    private String reqBody;

    @Schema(description = "响应报文")
    @TableField("resp_body")
    private String respBody;

    @Schema(description = "1正常 0异常")
    @TableField("success")
    @Excel(name = "结果", width = 10)
    private Integer success;

    @Schema(description = "失败/忽略说明")
    @TableField("error_msg")
    @Excel(name = "说明", width = 30)
    private String errorMsg;

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
    @Excel(name = "时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "更新人")
    @TableField("update_by")
    private String updateBy;

    @Schema(description = "更新时间")
    @TableField("update_time")
    private Date updateTime;
}
