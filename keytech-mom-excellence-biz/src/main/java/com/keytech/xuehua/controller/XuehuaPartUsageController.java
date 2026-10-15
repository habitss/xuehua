package com.keytech.xuehua.controller;

import com.keytech.base.controller.BaseController;
import com.keytech.xuehua.entity.XuehuaPartUsage;
import com.keytech.xuehua.service.IXuehuaPartUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 备件使用报表 Controller（说明书 3.5.3：出库备件使用情况跟踪）
 *
 * <p>标准 CRUD/导出继承 BaseController；/usageList 为装饰列表
 * （自动回填已拣配明细 + days/statusText + 筛选），/confirm /remind /remindAll 为业务动作。
 */
@Tag(name = "备件使用报表")
@RestController
@RequestMapping("/xuehua/usage")
public class XuehuaPartUsageController extends BaseController<IXuehuaPartUsageService, XuehuaPartUsage> {

    @Autowired
    private IXuehuaPartUsageService xuehuaPartUsageService;

    /** 装饰列表（keyword/status/from/to；status 支持 0/1/2/over超1周未使用；包装 {records,total}） */
    @Operation(summary = "备件使用报表-装饰列表")
    @GetMapping(value = "/usageList")
    public Result<Map<String, Object>> usageList(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date to) {
        Date toEnd = to == null ? null : new Date(to.getTime() + 86400000L - 1);
        List<Map<String, Object>> rows = xuehuaPartUsageService.usageRows(keyword, status, from, toEnd);
        Map<String, Object> page = new HashMap<>(4);
        page.put("records", rows);
        page.put("total", rows.size());
        return Result.OK(page);
    }

    /**
     * 确认用途：used=true 已使用（usedQty 必填）；false=登记未使用
     */
    @AutoLog(value = "备件使用报表-确认用途")
    @Operation(summary = "备件使用报表-确认用途(已使用/未使用)")
    @PostMapping(value = "/confirm")
    public Result<String> confirm(@RequestParam(name = "id") Integer id,
                                  @RequestParam(name = "used") boolean used,
                                  @RequestParam(name = "usedQty", required = false) Integer usedQty,
                                  @RequestParam(name = "photos", required = false) String photos,
                                  @RequestParam(name = "purpose", required = false) String purpose) {
        try {
            xuehuaPartUsageService.confirm(id, used, usedQty, photos, purpose, "管理员");
            return Result.OK(used ? "已确认使用 · 同步 Emis" : "已登记未使用");
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 提醒确认：body {"ids":[..]} 单条/批量；空 body / {} = 一键提醒全部未确认。
     * （不直接收 List：前端空参时 defHttp 发 {} 对象，List 反序列化会炸——用 Map 包一层兼容两态）
     */
    @AutoLog(value = "备件使用报表-提醒确认")
    @Operation(summary = "备件使用报表-提醒确认")
    @PostMapping(value = "/remind")
    public Result<String> remind(@RequestBody(required = false) Map<String, List<Integer>> body) {
        List<Integer> ids = body == null ? null : body.get("ids");
        int n = (ids == null || ids.isEmpty())
                ? xuehuaPartUsageService.remindAll()
                : xuehuaPartUsageService.remind(ids);
        return Result.OK("已提醒 " + n + " 条待确认记录");
    }

    /** Excel 导出（@Excel 注解字段，QueryGenerator 筛选） */
    @Operation(summary = "备件使用报表-导出Excel")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, XuehuaPartUsage usage) {
        return super.exportXls(request, usage, XuehuaPartUsage.class, "备件使用报表");
    }
}
