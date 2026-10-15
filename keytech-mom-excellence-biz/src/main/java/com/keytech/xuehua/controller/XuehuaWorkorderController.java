package com.keytech.xuehua.controller;

import com.keytech.xuehua.service.IXuehuaWorkorderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单中心 Controller（说明书 3.5.1：出入库/盘点作业工单集中管理与统计）
 *
 * <p>只读聚合端点：/listData 三域拉平（统一 工单号/类型/内容/数量/执行人/时间/状态），
 * /stat 按类型统计数量与完成情况。业务逻辑在 {@link IXuehuaWorkorderService}。
 */
@Tag(name = "工单中心")
@RestController
@RequestMapping("/xuehua/workorder")
@Slf4j
public class XuehuaWorkorderController {

    @Resource
    private IXuehuaWorkorderService workorderService;

    /** 工单聚合列表（keyword/woType/from/to，时间倒序，包装 {records,total} 供 useListPage） */
    @Operation(summary = "工单中心-聚合列表")
    @GetMapping(value = "/listData")
    public Result<Map<String, Object>> listData(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "woType", required = false) String woType,
            @RequestParam(name = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date to) {
        // to 闭区间到当天末尾（与 createTime 同一 JVM 时区口径，不做手工时区偏移）
        Date toEnd = to == null ? null : new Date(to.getTime() + 86400000L - 1);
        List<Map<String, Object>> rows = workorderService.workorderRows(keyword, woType, from, toEnd);
        Map<String, Object> page = new HashMap<>(4);
        page.put("records", rows);
        page.put("total", rows.size());
        return Result.OK(page);
    }

    /** 按类型统计工单数量与完成情况（入库/出库/盘点 三卡） */
    @Operation(summary = "工单中心-类型统计")
    @GetMapping(value = "/stat")
    public Result<Map<String, Object>> stat() {
        return Result.OK(workorderService.stat());
    }
}
