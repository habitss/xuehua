package com.keytech.xuehua.controller;

import com.keytech.base.controller.BaseController;
import com.keytech.xuehua.entity.XuehuaInventory;
import com.keytech.xuehua.service.IXuehuaDashboardService;
import com.keytech.xuehua.service.IXuehuaInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 工作台（运营驾驶舱）Controller
 *
 * <p>只读聚合：页面一次 overview 拉全 KPI/待办/四图/实时流/预警。
 * 继承 BaseController 仅为获得通用导出等基建能力；本模块不提供增删改端点。
 */
@Tag(name = "工作台")
@RestController
@RequestMapping("/xuehua/dashboard")
@Slf4j
public class XuehuaDashboardController extends BaseController<IXuehuaInventoryService, XuehuaInventory> {

    @Autowired
    private IXuehuaDashboardService xuehuaDashboardService;

    /**
     * 工作台总览（一次返回全部区块数据；前端 60s 轮询刷新）
     */
    @Operation(summary = "工作台-总览聚合")
    @GetMapping(value = "/overview")
    public Result<Map<String, Object>> overview() {
        return Result.OK(xuehuaDashboardService.overview());
    }
}
