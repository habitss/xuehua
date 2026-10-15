package com.keytech.mom.excellence.mdm.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keytech.base.controller.BaseController;
import com.keytech.mom.excellence.mdm.service.IMdmDictService;
import com.keytech.mom.excellence.module.mdm.entity.MdmDict;
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
import java.util.stream.Collectors;

/**
 * @Description: 字典表（业务字典-配置项）
 */
@Tag(name = "字典表")
@RestController
@RequestMapping("/mdm/mdmDict")
@Slf4j
public class MdmDictController extends BaseController<IMdmDictService, MdmDict> {

    @Autowired
    private IMdmDictService mdmDictService;

    /**
     * 分页列表查询（按 groupCode 过滤）
     *
     * @param mdmDict
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @AutoLog(value = "字典-分页列表查询")
    @Operation(summary = "字典-分页列表查询")
    @GetMapping(value = "/listData")
    public Result<IPage<MdmDict>> queryPageList(MdmDict mdmDict,
                                                @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                HttpServletRequest req) {
        QueryWrapper<MdmDict> queryWrapper = QueryGenerator.initQueryWrapper(mdmDict, req.getParameterMap());
        queryWrapper.orderByAsc("sort");
        Page<MdmDict> page = new Page<>(pageNo, pageSize);
        IPage<MdmDict> pageList = mdmDictService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 字典查询（按 groupCode 集合批量查询，供其他模块使用）
     *
     * @param code 多个字典组编码用逗号分隔
     * @return
     */
    @AutoLog(value = "字典-按字典组查询")
    @Operation(summary = "字典-按字典组查询")
    @GetMapping(value = "/dicList")
    public Result<?> getByGroupCode(@RequestParam(required = false) String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        List<String> list = Arrays.stream(code.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        List<MdmDict> lists = mdmDictService.getDictAllOrByGroupCode(list);
        return Result.OK(lists);
    }

    /**
     * 添加
     *
     * @param mdmDict
     * @return
     */
    @AutoLog(value = "字典-添加")
    @Operation(summary = "字典-添加")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody @Valid MdmDict mdmDict) {
        mdmDictService.save(mdmDict);
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * 编辑
     *
     * @param mdmDict
     * @return
     */
    @AutoLog(value = "字典-编辑")
    @Operation(summary = "字典-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody @Valid MdmDict mdmDict) {
        mdmDictService.updateById(mdmDict);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "字典-通过id删除")
    @Operation(summary = "字典-通过id删除")
    @DeleteMapping(value = "/deleteData")
    public Result<String> deleteData(@RequestParam(name = "id", required = true) Integer id) {
        mdmDictService.removeById(id);
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "字典-批量删除")
    @Operation(summary = "字典-批量删除")
    @DeleteMapping(value = "/deleteDataBatch")
    public Result<String> deleteDataBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.mdmDictService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @Operation(summary = "字典-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<MdmDict> queryById(@RequestParam(name = "id", required = true) Integer id) {
        MdmDict mdmDict = mdmDictService.getById(id);
        if (mdmDict == null) {
            return Result.error(GlobalErrorEnum.NO_CORRESPONDING_DATA_FOUND.getCode());
        }
        return Result.OK(mdmDict);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param mdmDict
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, MdmDict mdmDict) {
        return super.exportXls(request, mdmDict, MdmDict.class, "字典表");
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
        return super.importExcel(request, response, MdmDict.class);
    }
}
