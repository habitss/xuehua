package com.keytech.mom.excellence.mdm.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.keytech.base.controller.BaseController;
import com.keytech.mom.excellence.mdm.service.IMdmDictCategorizeService;
import com.keytech.mom.excellence.module.mdm.entity.MdmDictCategorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.config.GlobalErrorEnum;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.List;

/**
 * @Description: 字典分类
 * @Author: jeecg-boot
 * @Date:   2025-01-14
 * @Version: V1.0
 */
@Tag(name = "字典分类")
@RestController
@RequestMapping("/mdm/mdmDictCategorize")
@Slf4j
public class MdmDictCategorizeController extends BaseController<IMdmDictCategorizeService, MdmDictCategorize> {

    @Autowired
    private IMdmDictCategorizeService mdmDictCategorizeService;

    /**
     * 列表查询
     *
     * @param mdmDictCategorize
     * @param req
     * @return
     */
    @Operation(summary = "字典分类-列表查询")
    @GetMapping(value = "/queryList")
    public Result<List<MdmDictCategorize>> queryList(MdmDictCategorize mdmDictCategorize, HttpServletRequest req) {
        QueryWrapper<MdmDictCategorize> queryWrapper = QueryGenerator.initQueryWrapper(mdmDictCategorize, req.getParameterMap());
        String parentIds = req.getParameter("parentIds");
        if (StrUtil.isNotBlank(parentIds)) {
            List<String> parentIdList = Arrays.asList(parentIds.split(","));
            queryWrapper.in("parent_id", parentIdList);
        }
        return Result.OK(mdmDictCategorizeService.list(queryWrapper));
    }

    /**
     * 【vue3专用】加载一级节点/如果是同步 则所有数据
     *
     * @param async
     * @param pcode
     * @return
     */
    @RequestMapping(value = "/loadTreeRoot", method = RequestMethod.GET)
    public Result<List<SelectTreeModel>> loadTreeRoot(@RequestParam(name = "async") Boolean async, @RequestParam(name = "pcode") String pcode) {
        Result<List<SelectTreeModel>> result = new Result<>();
        try {
            List<SelectTreeModel> ls = mdmDictCategorizeService.queryListByPid(pcode);
            if (!async) {
                loadAllChildren(ls);
            }
            result.setResult(ls);
            result.setSuccess(true);
        } catch (Exception e) {
            e.printStackTrace();
            result.setMessage(e.getMessage());
            result.setSuccess(false);
        }
        return result;
    }

    /**
     * 【vue3专用】递归求子节点 同步加载用到
     *
     * @param ls
     */
    private void loadAllChildren(List<SelectTreeModel> ls) {
        for (SelectTreeModel tsm : ls) {
            List<SelectTreeModel> temp = mdmDictCategorizeService.queryListByPid(tsm.getKey());
            if (temp != null && temp.size() > 0) {
                tsm.setChildren(temp);
                loadAllChildren(temp);
            }
        }
    }

    /**
     * 添加
     *
     * @param mdmDictCategorize
     * @return
     */
    @AutoLog(value = "字典分类-添加")
    @Operation(summary = "字典分类-添加")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody MdmDictCategorize mdmDictCategorize) {
        mdmDictCategorizeService.save(mdmDictCategorize);
        if (StrUtil.isNotBlank(mdmDictCategorize.getParentId())) {
            mdmDictCategorizeService.lambdaUpdate().eq(MdmDictCategorize::getId, mdmDictCategorize.getParentId()).set(MdmDictCategorize::getHasChild, "1").update();
        }
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * 编辑
     *
     * @param mdmDictCategorize
     * @return
     */
    @AutoLog(value = "字典分类-编辑")
    @Operation(summary = "字典分类-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody MdmDictCategorize mdmDictCategorize) {
        mdmDictCategorizeService.updateById(mdmDictCategorize);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "字典分类-通过id删除")
    @Operation(summary = "字典分类-通过id删除")
    @RequiresPermissions("mdm:mes4_mdm_dict_categorize:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        mdmDictCategorizeService.removeById(id);
        return Result.ok(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "字典分类-批量删除")
    @Operation(summary = "字典分类-批量删除")
    @RequiresPermissions("mdm:mes4_mdm_dict_categorize:deletionBatch")
    @DeleteMapping(value = "/deletionBatch")
    public Result<String> deletionBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.mdmDictCategorizeService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @Operation(summary = "字典分类-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<MdmDictCategorize> queryById(@RequestParam(name = "id", required = true) String id) {
        MdmDictCategorize mdmDictCategorize = mdmDictCategorizeService.getById(id);
        if (mdmDictCategorize == null) {
            return Result.error(GlobalErrorEnum.NO_CORRESPONDING_DATA_FOUND.getCode());
        }
        return Result.OK(mdmDictCategorize);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param mdmDictCategorize
     */
    @RequiresPermissions("mdm:mes4_mdm_dict_categorize:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, MdmDictCategorize mdmDictCategorize) {
        return super.exportXls(request, mdmDictCategorize, MdmDictCategorize.class, "字典分类");
    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequiresPermissions("mdm:mes4_mdm_dict_categorize:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, MdmDictCategorize.class);
    }
}
