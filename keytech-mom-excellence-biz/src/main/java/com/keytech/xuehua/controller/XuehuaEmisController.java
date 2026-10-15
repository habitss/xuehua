package com.keytech.xuehua.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.keytech.xuehua.entity.XuehuaOpLog;
import com.keytech.xuehua.mapper.XuehuaOpLogMapper;
import com.keytech.xuehua.service.IXuehuaMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * EMIS 回传管理 Controller（登录态，内部管理操作）
 *
 * <p>三向回传（入库/出库/盘点完成推 EMIS）失败时留痕 mes4_xuehua_op_log(EMIS_PUSH, success=0)，
 * 这里提供失败清单查询与手动重推（按留痕 req_body 原样重发，成功回写 success=1）。
 */
@Tag(name = "EMIS回传管理")
@RestController
@RequestMapping("/xuehua/emis")
@Slf4j
public class XuehuaEmisController {

    @Resource
    private IXuehuaMonitorService monitorService;
    @Resource
    private XuehuaOpLogMapper opLogMapper;

    /** 失败回传清单（EMIS_PUSH + success=0，时间倒序） */
    @Operation(summary = "EMIS回传-失败清单")
    @GetMapping(value = "/failedList")
    public Result<List<XuehuaOpLog>> failedList() {
        return Result.OK(opLogMapper.selectList(new QueryWrapper<XuehuaOpLog>()
                .eq("biz_type", "EMIS_PUSH").eq("success", 0)
                .orderByDesc("id").last("limit 100")));
    }

    /** 手动重推一条失败回传（按 op_log id；EMIS 侧按 externalNo 幂等，重复投递安全） */
    @AutoLog(value = "EMIS回传-手动重推")
    @Operation(summary = "EMIS回传-手动重推")
    @PostMapping(value = "/repush")
    public Result<String> repush(@RequestParam(name = "opLogId") Integer opLogId) {
        try {
            return Result.OK(monitorService.repush(opLogId));
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }
}
