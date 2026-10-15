package com.keytech.xuehua.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keytech.base.controller.BaseController;
import com.keytech.xuehua.entity.XuehuaPart;
import com.keytech.xuehua.service.IXuehuaPartService;
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

/**
 * @Description: 备件主数据
 */
@Tag(name = "备件主数据")
@RestController
@RequestMapping("/xuehua/part")
@Slf4j
public class XuehuaPartController extends BaseController<IXuehuaPartService, XuehuaPart> {

    /**
     * EMIS 备件主数据同步（匿名端点，白名单放行；说明书 5.1：备件主数据从 Emis 同步）。
     *
     * <p>契约（我方定义，EMIS 适配）：
     * 请求 {partCode(必填), partName, model, category, unit, safeStock(可选——EMIS 侧若有标准可带), externalNo}
     * 响应 {code:200/500, msg, id, isUpdate}。
     * partCode 幂等：同码重复推送=更新主数据（WCS 本地安全库存/默认库区不被冲掉），新码插入。
     */
    @AutoLog(value = "备件主数据-EMIS同步")
    @Operation(summary = "备件主数据-EMIS同步(匿名)")
    @PostMapping(value = "/receive")
    public java.util.Map<String, Object> receive(@RequestBody java.util.Map<String, Object> body) {
        java.util.Map<String, Object> resp = new java.util.LinkedHashMap<>(8);
        try {
            String partCode = body.get("partCode") == null ? "" : String.valueOf(body.get("partCode")).trim();
            if (partCode.isBlank()) {
                throw new IllegalStateException("partCode(备件编码)不能为空");
            }
            String externalNo = body.get("externalNo") == null ? null : String.valueOf(body.get("externalNo")).trim();
            boolean existed = xuehuaPartService.getOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.keytech.xuehua.entity.XuehuaPart>()
                            .eq("part_code", partCode).last("limit 1")) != null;
            com.keytech.xuehua.entity.XuehuaPart part = new com.keytech.xuehua.entity.XuehuaPart()
                    .setPartCode(partCode)
                    .setPartName(str(body.get("partName")))
                    .setModel(str(body.get("model")))
                    .setCategory(str(body.get("category")))
                    .setUnit(str(body.get("unit")));
            Object safe = body.get("safeStock");
            if (safe != null && !String.valueOf(safe).isBlank()) {
                try {
                    part.setSafeStock(Integer.valueOf(String.valueOf(safe)));
                } catch (NumberFormatException ignore) {
                    // EMIS 侧格式异常不阻断——安全库存留给仓库本地维护
                }
            }
            part = xuehuaPartService.receiveFromEmis(part, externalNo);
            resp.put("code", 200);
            resp.put("msg", existed ? "主数据已更新" : "主数据已新增");
            resp.put("id", part.getId());
            resp.put("isUpdate", existed);
        } catch (IllegalStateException e) {
            resp.put("code", 500);
            resp.put("msg", e.getMessage());
        } catch (Exception e) {
            resp.put("code", 500);
            resp.put("msg", "同步失败：" + e.getMessage());
        }
        return resp;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    @Autowired
    private IXuehuaPartService xuehuaPartService;

    /**
     * 分页列表查询
     */
    @AutoLog(value = "备件-分页列表查询")
    @Operation(summary = "备件-分页列表查询")
    @GetMapping(value = "/listData")
    public Result<IPage<XuehuaPart>> queryPageList(XuehuaPart xuehuaPart,
                                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                   HttpServletRequest req) {
        QueryWrapper<XuehuaPart> queryWrapper = QueryGenerator.initQueryWrapper(xuehuaPart, req.getParameterMap());
        queryWrapper.orderByDesc("create_time");
        Page<XuehuaPart> page = new Page<>(pageNo, pageSize);
        IPage<XuehuaPart> pageList = xuehuaPartService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     */
    @AutoLog(value = "备件-添加")
    @Operation(summary = "备件-添加")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody @Valid XuehuaPart xuehuaPart) {
        xuehuaPartService.save(xuehuaPart);
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * 编辑
     */
    @AutoLog(value = "备件-编辑")
    @Operation(summary = "备件-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody @Valid XuehuaPart xuehuaPart) {
        xuehuaPartService.updateById(xuehuaPart);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "备件-通过id删除")
    @Operation(summary = "备件-通过id删除")
    @DeleteMapping(value = "/deleteData")
    public Result<String> deleteData(@RequestParam(name = "id", required = true) Integer id) {
        xuehuaPartService.removeById(id);
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "备件-批量删除")
    @Operation(summary = "备件-批量删除")
    @DeleteMapping(value = "/deleteDataBatch")
    public Result<String> deleteDataBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.xuehuaPartService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "备件-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<XuehuaPart> queryById(@RequestParam(name = "id", required = true) Integer id) {
        XuehuaPart xuehuaPart = xuehuaPartService.getById(id);
        if (xuehuaPart == null) {
            return Result.error(GlobalErrorEnum.NO_CORRESPONDING_DATA_FOUND.getCode());
        }
        return Result.OK(xuehuaPart);
    }

    /**
     * 导出excel
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, XuehuaPart xuehuaPart) {
        return super.exportXls(request, xuehuaPart, XuehuaPart.class, "备件主数据");
    }

    /**
     * 通过excel导入数据
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, XuehuaPart.class);
    }
}
