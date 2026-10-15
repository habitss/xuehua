package com.keytech.mom.excellence.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keytech.base.controller.BaseController;
import com.keytech.mom.excellence.mdm.service.IMdmDictGroupService;
import com.keytech.mom.excellence.module.mdm.entity.MdmDictGroup;
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
 * @Description: 字典组表（业务字典-子类）
 */
@Tag(name = "字典组表")
@RestController
@RequestMapping("/mdm/mdmDictGroup")
@Slf4j
public class MdmDictGroupController extends BaseController<IMdmDictGroupService, MdmDictGroup> {

    @Autowired
    private IMdmDictGroupService mdmDictGroupService;

    /**
     * 分页列表查询（按 categorizeId 过滤）
     *
     * @param mdmDictGroup
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @AutoLog(value = "字典组-分页列表查询")
    @Operation(summary = "字典组-分页列表查询")
    @GetMapping(value = "/listData")
    public Result<IPage<MdmDictGroup>> queryPageList(MdmDictGroup mdmDictGroup,
                                                     @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                     @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                     HttpServletRequest req) {
        QueryWrapper<MdmDictGroup> queryWrapper = QueryGenerator.initQueryWrapper(mdmDictGroup, req.getParameterMap());
        queryWrapper.orderByAsc("sort");
        Page<MdmDictGroup> page = new Page<>(pageNo, pageSize);
        IPage<MdmDictGroup> pageList = mdmDictGroupService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     *
     * @param mdmDictGroup
     * @return
     */
    @AutoLog(value = "字典组-添加")
    @Operation(summary = "字典组-添加")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody @Valid MdmDictGroup mdmDictGroup) {
        mdmDictGroupService.save(mdmDictGroup);
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * 编辑
     *
     * @param mdmDictGroup
     * @return
     */
    @AutoLog(value = "字典组-编辑")
    @Operation(summary = "字典组-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody @Valid MdmDictGroup mdmDictGroup) {
        mdmDictGroupService.updateById(mdmDictGroup);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "字典组-通过id删除")
    @Operation(summary = "字典组-通过id删除")
    @DeleteMapping(value = "/deleteData")
    public Result<String> deleteData(@RequestParam(name = "id", required = true) Integer id) {
        mdmDictGroupService.removeById(id);
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "字典组-批量删除")
    @Operation(summary = "字典组-批量删除")
    @DeleteMapping(value = "/deleteDataBatch")
    public Result<String> deleteDataBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.mdmDictGroupService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @Operation(summary = "字典组-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<MdmDictGroup> queryById(@RequestParam(name = "id", required = true) Integer id) {
        MdmDictGroup mdmDictGroup = mdmDictGroupService.getById(id);
        if (mdmDictGroup == null) {
            return Result.error(GlobalErrorEnum.NO_CORRESPONDING_DATA_FOUND.getCode());
        }
        return Result.OK(mdmDictGroup);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param mdmDictGroup
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, MdmDictGroup mdmDictGroup) {
        return super.exportXls(request, mdmDictGroup, MdmDictGroup.class, "字典组表");
    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, MdmDictGroup.class);
    }
}
