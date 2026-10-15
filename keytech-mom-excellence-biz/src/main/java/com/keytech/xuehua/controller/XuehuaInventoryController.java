package com.keytech.xuehua.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keytech.base.controller.BaseController;
import com.keytech.xuehua.entity.XuehuaInventory;
import com.keytech.xuehua.entity.XuehuaInventoryDetail;
import com.keytech.xuehua.service.IXuehuaInventoryService;
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
 * @Description: 盘点管理（盘点单）
 */
@Tag(name = "盘点管理")
@RestController
@RequestMapping("/xuehua/inventory")
@Slf4j
public class XuehuaInventoryController extends BaseController<IXuehuaInventoryService, XuehuaInventory> {

    @Autowired
    private IXuehuaInventoryService xuehuaInventoryService;

    @Autowired
    private com.keytech.xuehua.service.IXuehuaPersonService xuehuaPersonService;

    /**
     * EMIS 盘点任务下发（匿名端点，白名单放行）。
     *
     * <p>契约（我方定义，EMIS 适配——原型 pvData source=EMIS下发 / 说明书 5.1）：
     * 请求 {externalNo(幂等键,必填), zoneCode, scope, executor(盘点人,必填——须已注册),
     *       identifyType, bookQty, remark}
     * 响应 {code:200/500, msg, pvNo, status:0待盘点}。
     * 语义与入库/出库 receive 同款：成功=200、真实失败=500，重推安全；
     * 重复推送幂等返回原盘点单号。盘点人未注册拒绝（监控管理口径，不能进仓作业）。
     */
    @AutoLog(value = "盘点单-EMIS盘点任务下发")
    @Operation(summary = "盘点单-EMIS盘点任务下发(匿名)")
    @PostMapping(value = "/receive")
    public Map<String, Object> receive(@RequestBody Map<String, Object> body) {
        Map<String, Object> resp = new java.util.LinkedHashMap<>(8);
        String externalNo = body.get("externalNo") == null ? null : String.valueOf(body.get("externalNo")).trim();
        try {
            if (externalNo == null || externalNo.isBlank()) {
                throw new IllegalStateException("externalNo(EMIS盘点任务单号)不能为空");
            }
            String executor = body.get("executor") == null ? "" : String.valueOf(body.get("executor")).trim();
            if (executor.isBlank()) {
                throw new IllegalStateException("executor(盘点人)不能为空");
            }
            // 幂等判重：同一盘点任务号重复推送 → 返回原单
            XuehuaInventory exist = xuehuaInventoryService.getOne(
                    new QueryWrapper<XuehuaInventory>().eq("external_no", externalNo).last("limit 1"));
            if (exist != null) {
                resp.put("code", 200);
                resp.put("msg", "重复推送，返回原盘点任务");
                resp.put("pvNo", exist.getPvNo());
                resp.put("status", exist.getStatus());
                return resp;
            }
            // 盘点人校验+进仓授权（与入库/出库 receive 同构：落库事务前完成网络调用）
            com.keytech.xuehua.entity.XuehuaPerson executor0 = xuehuaPersonService.resolveByOperator(executor);
            if (executor0 == null) {
                throw new IllegalStateException("盘点人 " + executor + " 未在人员注册中登记，请先注册并推送门禁授权");
            }
            xuehuaPersonService.authorizeEntry(executor0);
            XuehuaInventory task = new XuehuaInventory()
                    .setExternalNo(externalNo)
                    .setZoneCode(str(body.get("zoneCode")))
                    .setScope(str(body.get("scope")))
                    .setIdentifyType(str(body.get("identifyType")))
                    .setBookQty(parseInt(body.get("bookQty")))
                    .setRemark(str(body.get("remark")));
            task = xuehuaInventoryService.receiveFromEmis(task, executor0.getEmpNo());
            resp.put("code", 200);
            resp.put("msg", "盘点任务接收成功");
            resp.put("pvNo", task.getPvNo());
            resp.put("status", task.getStatus());
        } catch (IllegalStateException e) {
            resp.put("code", 500);
            resp.put("msg", e.getMessage());
        } catch (Exception e) {
            log.error("EMIS盘点任务接收异常", e);
            resp.put("code", 500);
            resp.put("msg", "接收失败：" + e.getMessage());
        }
        return resp;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static Integer parseInt(Object o) {
        if (o == null || String.valueOf(o).isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 分页列表查询
     */
    @AutoLog(value = "盘点单-分页列表查询")
    @Operation(summary = "盘点单-分页列表查询")
    @GetMapping(value = "/listData")
    public Result<IPage<XuehuaInventory>> queryPageList(XuehuaInventory xuehuaInventory,
                                                        @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                        @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                        HttpServletRequest req) {
        QueryWrapper<XuehuaInventory> queryWrapper = QueryGenerator.initQueryWrapper(xuehuaInventory, req.getParameterMap());
        queryWrapper.orderByDesc("create_time");
        Page<XuehuaInventory> page = new Page<>(pageNo, pageSize);
        IPage<XuehuaInventory> pageList = xuehuaInventoryService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 各状态数量统计（流程概览卡）
     */
    @Operation(summary = "盘点单-状态统计")
    @GetMapping(value = "/stat")
    public Result<Map<String, Object>> stat() {
        return Result.OK(xuehuaInventoryService.stat());
    }

    /**
     * 新建盘点任务（自动生成单号、默认值）
     */
    @AutoLog(value = "盘点单-新建盘点任务")
    @Operation(summary = "盘点单-新建盘点任务")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody @Valid XuehuaInventory xuehuaInventory) {
        xuehuaInventoryService.addInventory(xuehuaInventory);
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * 生成库位级盘点明细（账面快照）
     */
    @AutoLog(value = "盘点单-生成盘点明细")
    @Operation(summary = "盘点单-生成盘点明细")
    @PostMapping(value = "/generateDetails")
    public Result<List<XuehuaInventoryDetail>> generateDetails(@RequestParam(name = "id") Integer id) {
        try {
            return Result.OK(xuehuaInventoryService.generateDetails(id));
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 盘点单的库位明细列表
     */
    @Operation(summary = "盘点单-盘点明细列表")
    @GetMapping(value = "/details")
    public Result<List<XuehuaInventoryDetail>> details(@RequestParam(name = "id") Integer id) {
        return Result.OK(xuehuaInventoryService.listDetails(id));
    }

    /**
     * 录入某明细行实盘数（算差异，不动台账）
     */
    @AutoLog(value = "盘点单-录入实盘数")
    @Operation(summary = "盘点单-录入实盘数")
    @PostMapping(value = "/submitReal")
    public Result<String> submitReal(@RequestParam(name = "detailId") Integer detailId,
                                     @RequestParam(name = "realQty") Integer realQty) {
        try {
            xuehuaInventoryService.submitReal(detailId, realQty);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
        return Result.OK("实盘数已录入");
    }

    /**
     * 差异处置：所有明细差额原子调整库位台账，盘点单转【已完成】并解冻
     */
    @AutoLog(value = "盘点单-差异处置")
    @Operation(summary = "盘点单-差异处置(联动库存)")
    @PostMapping(value = "/applyDifferences")
    public Result<String> applyDifferences(@RequestParam(name = "id") Integer id) {
        try {
            xuehuaInventoryService.applyDifferences(id);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
        return Result.OK("差异处置完成，台账已校正");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "盘点单-编辑")
    @Operation(summary = "盘点单-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody @Valid XuehuaInventory xuehuaInventory) {
        xuehuaInventoryService.updateById(xuehuaInventory);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "盘点单-通过id删除")
    @Operation(summary = "盘点单-通过id删除")
    @DeleteMapping(value = "/deleteData")
    public Result<String> deleteData(@RequestParam(name = "id", required = true) Integer id) {
        xuehuaInventoryService.removeById(id);
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "盘点单-批量删除")
    @Operation(summary = "盘点单-批量删除")
    @DeleteMapping(value = "/deleteDataBatch")
    public Result<String> deleteDataBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.xuehuaInventoryService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "盘点单-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<XuehuaInventory> queryById(@RequestParam(name = "id", required = true) Integer id) {
        XuehuaInventory xuehuaInventory = xuehuaInventoryService.getById(id);
        if (xuehuaInventory == null) {
            return Result.error(GlobalErrorEnum.NO_CORRESPONDING_DATA_FOUND.getCode());
        }
        return Result.OK(xuehuaInventory);
    }

    /**
     * 开始盘点：待盘点 -> 盘点中
     */
    @AutoLog(value = "盘点单-开始盘点")
    @Operation(summary = "盘点单-开始盘点")
    @PostMapping(value = "/start")
    public Result<String> start(@RequestParam(name = "id", required = true) Integer id) {
        try {
            xuehuaInventoryService.start(id);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
        return Result.OK("盘点已开始");
    }

    /**
     * 推进盘点阶段（期初账面 -> ... -> 完成）
     */
    @AutoLog(value = "盘点单-推进盘点")
    @Operation(summary = "盘点单-推进盘点")
    @PostMapping(value = "/advance")
    public Result<String> advance(@RequestParam(name = "id", required = true) Integer id) {
        try {
            xuehuaInventoryService.advance(id);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
        return Result.OK("盘点已推进到下一阶段");
    }

    /**
     * 冻结库存（冻结期间入库登记被拦截）
     */
    @AutoLog(value = "盘点单-冻结库存")
    @Operation(summary = "盘点单-冻结库存")
    @PostMapping(value = "/freeze")
    public Result<String> freeze(@RequestParam(name = "id", required = true) Integer id) {
        try {
            xuehuaInventoryService.freeze(id);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
        return Result.OK("库存已冻结");
    }

    /**
     * 差异处置：录入实盘数、完成盘点并解冻
     */
    @AutoLog(value = "盘点单-差异处置")
    @Operation(summary = "盘点单-差异处置")
    @PostMapping(value = "/dispose")
    public Result<String> dispose(@RequestParam(name = "id", required = true) Integer id,
                                  @RequestParam(name = "realQty", required = false) Integer realQty) {
        try {
            xuehuaInventoryService.dispose(id, realQty);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
        return Result.OK("差异处置完成，已解冻并回传 Emis");
    }

    /**
     * 终止盘点并解冻
     */
    @AutoLog(value = "盘点单-终止盘点")
    @Operation(summary = "盘点单-终止盘点")
    @PostMapping(value = "/stop")
    public Result<String> stop(@RequestParam(name = "id", required = true) Integer id) {
        try {
            xuehuaInventoryService.stop(id);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
        return Result.OK("盘点已终止");
    }

    /**
     * 导出excel
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, XuehuaInventory xuehuaInventory) {
        return super.exportXls(request, xuehuaInventory, XuehuaInventory.class, "盘点单");
    }

    /**
     * 通过excel导入数据
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, XuehuaInventory.class);
    }
}
