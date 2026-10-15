package com.keytech.xuehua.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keytech.base.controller.BaseController;
import com.keytech.xuehua.entity.XuehuaPutaway;
import com.keytech.xuehua.service.IXuehuaPutawayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.config.GlobalErrorEnum;
import org.jeecg.common.system.query.QueryGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * @Description: 上架管理（上架任务：待上架/已上架）
 */
@Tag(name = "上架管理")
@RestController
@RequestMapping("/xuehua/putaway")
@Slf4j
public class XuehuaPutawayController extends BaseController<IXuehuaPutawayService, XuehuaPutaway> {

    @Autowired
    private IXuehuaPutawayService xuehuaPutawayService;

    /**
     * 分页列表查询
     */
    @AutoLog(value = "上架任务-分页列表查询")
    @Operation(summary = "上架任务-分页列表查询")
    @GetMapping(value = "/listData")
    public Result<IPage<XuehuaPutaway>> queryPageList(XuehuaPutaway xuehuaPutaway,
                                                      @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                      HttpServletRequest req) {
        QueryWrapper<XuehuaPutaway> queryWrapper = QueryGenerator.initQueryWrapper(xuehuaPutaway, req.getParameterMap());
        queryWrapper.orderByDesc("create_time");
        Page<XuehuaPutaway> page = new Page<>(pageNo, pageSize);
        IPage<XuehuaPutaway> pageList = xuehuaPutawayService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 各状态数量统计（流程概览卡）
     */
    @Operation(summary = "上架任务-状态统计")
    @GetMapping(value = "/stat")
    public Result<Map<String, Object>> stat() {
        return Result.OK(xuehuaPutawayService.stat());
    }

    /**
     * 确认上架（PTL 放置完成后回调）：待上架 -> 已上架，并更新库位状态
     */
    @AutoLog(value = "上架任务-确认上架")
    @Operation(summary = "上架任务-确认上架")
    @PostMapping(value = "/confirmPutaway")
    public Result<String> confirmPutaway(@RequestParam(name = "id", required = true) Integer id) {
        try {
            xuehuaPutawayService.confirmPutaway(id);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
        return Result.OK("上架成功，已反馈 WCS/Emis");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "上架任务-编辑")
    @Operation(summary = "上架任务-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody @Valid XuehuaPutaway xuehuaPutaway) {
        xuehuaPutawayService.updateById(xuehuaPutaway);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "上架任务-通过id删除")
    @Operation(summary = "上架任务-通过id删除")
    @DeleteMapping(value = "/deleteData")
    public Result<String> deleteData(@RequestParam(name = "id", required = true) Integer id) {
        xuehuaPutawayService.removeById(id);
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "上架任务-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<XuehuaPutaway> queryById(@RequestParam(name = "id", required = true) Integer id) {
        XuehuaPutaway xuehuaPutaway = xuehuaPutawayService.getById(id);
        if (xuehuaPutaway == null) {
            return Result.error(GlobalErrorEnum.NO_CORRESPONDING_DATA_FOUND.getCode());
        }
        return Result.OK(xuehuaPutaway);
    }

    /**
     * 导出excel
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, XuehuaPutaway xuehuaPutaway) {
        return super.exportXls(request, xuehuaPutaway, XuehuaPutaway.class, "上架任务");
    }
}
