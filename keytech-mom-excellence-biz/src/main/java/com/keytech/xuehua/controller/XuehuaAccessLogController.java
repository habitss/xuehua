package com.keytech.xuehua.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keytech.base.controller.BaseController;
import com.keytech.xuehua.entity.XuehuaAccessLog;
import com.keytech.xuehua.service.IXuehuaAccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 进出记录 Controller（监控管理）：时间线检索 + 回放取流
 */
@Tag(name = "进出记录")
@RestController
@RequestMapping("/xuehua/access")
public class XuehuaAccessLogController extends BaseController<IXuehuaAccessLogService, XuehuaAccessLog> {

    @Autowired
    private IXuehuaAccessLogService xuehuaAccessLogService;

    /** 分页列表（QueryGenerator 支持 personName/eventType/bizNo/zone 等筛选 + 时间范围） */
    @Operation(summary = "进出记录-分页列表")
    @GetMapping(value = "/listData")
    public Result<IPage<XuehuaAccessLog>> listData(XuehuaAccessLog log,
                                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                   HttpServletRequest req) {
        QueryWrapper<XuehuaAccessLog> qw = QueryGenerator.initQueryWrapper(log, req.getParameterMap());
        qw.orderByDesc("event_time");
        return Result.OK(xuehuaAccessLogService.page(new Page<>(pageNo, pageSize), qw));
    }

    /** Excel 导出（@Excel 注解字段） */
    @Operation(summary = "进出记录-导出Excel")
    @RequestMapping(value = "/exportXls")
    public org.springframework.web.servlet.ModelAndView exportXls(HttpServletRequest request, XuehuaAccessLog log) {
        return super.exportXls(request, log, XuehuaAccessLog.class, "进出记录");
    }

    /** Excel 导入（历史事件补录用） */
    @AutoLog(value = "进出记录-导入Excel")
    @Operation(summary = "进出记录-导入Excel")
    @PostMapping(value = "/importExcel")
    public Result<?> importExcel(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        return super.importExcel(request, response, XuehuaAccessLog.class);
    }

    /** 回放取流：按流水调海康 playbackURLs（hik 未启用返回提示） */
    @Operation(summary = "进出记录-回放取流")
    @GetMapping(value = "/playback")
    public Result<String> playback(@RequestParam(name = "id") Integer id) {
        String url = xuehuaAccessLogService.playbackUrl(id);
        if (url == null) {
            return Result.OK("海康平台未接入（xuehua.hik.enabled=false），当前仅提供时间线；联调后此处返回回放地址");
        }
        return Result.OK(url);
    }
}
