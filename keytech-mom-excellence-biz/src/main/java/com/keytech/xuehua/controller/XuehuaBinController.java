package com.keytech.xuehua.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keytech.base.controller.BaseController;
import com.keytech.xuehua.entity.XuehuaBin;
import com.keytech.xuehua.service.IXuehuaBinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.config.GlobalErrorEnum;
import org.jeecg.common.system.query.QueryGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Description: 库位
 */
@Tag(name = "库位")
@RestController
@RequestMapping("/xuehua/bin")
@Slf4j
public class

XuehuaBinController extends BaseController<IXuehuaBinService, XuehuaBin> {

    @Autowired
    private IXuehuaBinService xuehuaBinService;

    /**
     * 分页列表查询
     */
    @AutoLog(value = "库位-分页列表查询")
    @Operation(summary = "库位-分页列表查询")
    @GetMapping(value = "/listData")
    public Result<IPage<XuehuaBin>> queryPageList(XuehuaBin xuehuaBin,
                                                  @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                  @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                  HttpServletRequest req) {
        QueryWrapper<XuehuaBin> queryWrapper = QueryGenerator.initQueryWrapper(xuehuaBin, req.getParameterMap());
        queryWrapper.orderByAsc("bin_code");
        Page<XuehuaBin> page = new Page<>(pageNo, pageSize);
        IPage<XuehuaBin> pageList = xuehuaBinService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 库位地图数据（全量 + 备件名称/安全库存 + 低库存预警装饰，口径见 Service）
     */
    @AutoLog(value = "库位-地图数据")
    @Operation(summary = "库位-地图数据（含低库存预警）")
    @GetMapping(value = "/mapData")
    public Result<List<Map<String, Object>>> mapData() {
        return Result.OK(xuehuaBinService.mapRows());
    }

    /**
     * 添加
     */
    @AutoLog(value = "库位-添加")
    @Operation(summary = "库位-添加")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody @Valid XuehuaBin xuehuaBin) {
        xuehuaBinService.save(xuehuaBin);
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * 编辑
     */
    @AutoLog(value = "库位-编辑")
    @Operation(summary = "库位-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody @Valid XuehuaBin xuehuaBin) {
        xuehuaBinService.updateById(xuehuaBin);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "库位-通过id删除")
    @Operation(summary = "库位-通过id删除")
    @DeleteMapping(value = "/deleteData")
    public Result<String> deleteData(@RequestParam(name = "id", required = true) Integer id) {
        xuehuaBinService.removeById(id);
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "库位-批量删除")
    @Operation(summary = "库位-批量删除")
    @DeleteMapping(value = "/deleteDataBatch")
    public Result<String> deleteDataBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.xuehuaBinService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "库位-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<XuehuaBin> queryById(@RequestParam(name = "id", required = true) Integer id) {
        XuehuaBin xuehuaBin = xuehuaBinService.getById(id);
        if (xuehuaBin == null) {
            return Result.error(GlobalErrorEnum.NO_CORRESPONDING_DATA_FOUND.getCode());
        }
        return Result.OK(xuehuaBin);
    }

    /**
     * 导出excel
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, XuehuaBin xuehuaBin) {
        return super.exportXls(request, xuehuaBin, XuehuaBin.class, "库位");
    }

    /**
     * 通过excel导入数据
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, XuehuaBin.class);
    }
}
