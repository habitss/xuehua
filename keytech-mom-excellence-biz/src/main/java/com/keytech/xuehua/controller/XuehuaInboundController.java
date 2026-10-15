package com.keytech.xuehua.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keytech.base.controller.BaseController;
import com.keytech.xuehua.entity.XuehuaInbound;
import com.keytech.xuehua.service.IXuehuaInboundService;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Description:  入库管理（入库单）
 */
@Tag(name = "入库管理")
@RestController
@RequestMapping("/xuehua/inbound")
@Slf4j
public class XuehuaInboundController extends BaseController<IXuehuaInboundService, XuehuaInbound> {

    @Autowired
    private IXuehuaInboundService xuehuaInboundService;

    @Autowired
    private com.keytech.xuehua.service.IXuehuaPersonService xuehuaPersonService;

    /**
     * 分页列表查询
     */
    @AutoLog(value = "入库单-分页列表查询")
    @Operation(summary = "入库单-分页列表查询")
    @GetMapping(value = "/listData")
    public Result<IPage<XuehuaInbound>> queryPageList(XuehuaInbound xuehuaInbound,
                                                      @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                      HttpServletRequest req) {
        QueryWrapper<XuehuaInbound> queryWrapper = QueryGenerator.initQueryWrapper(xuehuaInbound, req.getParameterMap());
        queryWrapper.orderByDesc("create_time");
        Page<XuehuaInbound> page = new Page<>(pageNo, pageSize);
        IPage<XuehuaInbound> pageList = xuehuaInboundService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 各状态数量统计（流程概览卡）
     */
    @Operation(summary = "入库单-状态统计")
    @GetMapping(value = "/stat")
    public Result<Map<String, Object>> stat() {
        return Result.OK(xuehuaInboundService.stat());
    }

    /**
     * 扫码预填：扫备件条码自动带出主数据；扫送货单号提示已登记单号
     */
    @AutoLog(value = "入库单-扫码预填")
    @Operation(summary = "入库单-扫码预填")
    @GetMapping(value = "/scan")
    public Result<Map<String, Object>> scan(@RequestParam(name = "code") String code) {
        try {
            return Result.OK(xuehuaInboundService.scanFill(code));
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增入库登记（自动生成单号；盘点冻结期间拦截）
     */
    @AutoLog(value = "入库单-新增入库登记")
    @Operation(summary = "入库单-新增入库登记")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody @Valid XuehuaInbound xuehuaInbound) {
        try {
            xuehuaInboundService.addInbound(xuehuaInbound);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * EMIS 到货单同步（匿名端点，shiro 白名单 /xuehua/inbound/receive）
     *
     * <p>契约（我方定义，EMIS 适配）：请求 {externalNo(幂等键,必填), partCode(必填), qty(必填),
     * operator(操作人工号或姓名,必填——须已在人员注册), partName, model, unit, deliveryNo}
     * 响应 {code:200/500, msg, inboundNo, status:0待登记}
     *
     * <p>操作人校验：未注册人员拒绝（监控管理口径——不能进仓作业）；
     * 校验通过即推送海康门禁授权（人员注册时已推，此处幂等）。
     * 到货单落库为待登记(status=0)，现场补齐三要素后 /register 完成正式登记。
     */
    @AutoLog(value = "入库单-EMIS到货单同步")
    @Operation(summary = "入库单-EMIS到货单同步(匿名)")
    @PostMapping(value = "/receive")
    public Map<String, Object> receive(@RequestBody Map<String, Object> body) {
        Map<String, Object> resp = new LinkedHashMap<>(8);
        String externalNo = body.get("externalNo") == null ? null : String.valueOf(body.get("externalNo"));
        try {
            if (externalNo == null || externalNo.isBlank()) {
                throw new IllegalStateException("externalNo(EMIS到货单号)不能为空");
            }
            XuehuaInbound exist = xuehuaInboundService.getOne(
                    new QueryWrapper<XuehuaInbound>().eq("external_no", externalNo).last("limit 1"));
            if (exist != null) {
                resp.put("code", 200);
                resp.put("msg", "重复推送，返回原单");
                resp.put("inboundNo", exist.getInboundNo());
                resp.put("status", exist.getStatus());
                return resp;
            }
            // 【进仓授权（监控管理数据流）】人员已注册 → 给海康发授权指令；
            // 失败抛异常拒单（严格模式），hik.enabled=false 降级跳过照常接收。
            // 授权调用在落库事务之前（controller 层），保持事务内不做网络调用的纪律
            com.keytech.xuehua.entity.XuehuaPerson op = xuehuaPersonService.resolveByOperator(str(body.get("operator")));
            if (op == null) {
                throw new IllegalStateException("操作人 " + str(body.get("operator")) + " 未在人员注册中登记，请先注册并推送门禁授权");
            }
            xuehuaPersonService.authorizeEntry(op);
            XuehuaInbound arrival = new XuehuaInbound()
                    .setExternalNo(externalNo)
                    .setPartCode(str(body.get("partCode")))
                    .setPartName(str(body.get("partName")))
                    .setModel(str(body.get("model")))
                    .setUnit(str(body.get("unit")))
                    .setDeliveryNo(str(body.get("deliveryNo")))
                    .setQty(parseInt(body.get("qty")))
                    .setRemark("EMIS到货单");
            arrival = xuehuaInboundService.receiveArrival(arrival, op.getEmpNo());
            resp.put("code", 200);
            resp.put("msg", "到货单接收成功，待现场登记");
            resp.put("inboundNo", arrival.getInboundNo());
            resp.put("status", arrival.getStatus());
        } catch (IllegalStateException e) {
            resp.put("code", 500);
            resp.put("msg", e.getMessage());
        } catch (Exception e) {
            log.warn("[EMIS] 到货单接收失败: {}", e.getMessage());
            resp.put("code", 500);
            resp.put("msg", "处理失败：" + e.getMessage());
        }
        return resp;
    }

    /**
     * 到货单补登记：补齐三要素后 0待登记 → 1已登记（分配库位+上架任务+亮灯）
     */
    @AutoLog(value = "入库单-到货单补登记")
    @Operation(summary = "入库单-到货单补登记")
    @PostMapping(value = "/register")
    public Result<String> register(@RequestBody XuehuaInbound patch) {
        try {
            xuehuaInboundService.register(patch.getId(), patch);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
        return Result.OK("登记成功，已分配库位并点亮 PTL");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "入库单-编辑")
    @Operation(summary = "入库单-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody @Valid XuehuaInbound xuehuaInbound) {
        xuehuaInboundService.updateById(xuehuaInbound);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "入库单-通过id删除")
    @Operation(summary = "入库单-通过id删除")
    @DeleteMapping(value = "/deleteData")
    public Result<String> deleteData(@RequestParam(name = "id", required = true) Integer id) {
        xuehuaInboundService.removeById(id);
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "入库单-批量删除")
    @Operation(summary = "入库单-批量删除")
    @DeleteMapping(value = "/deleteDataBatch")
    public Result<String> deleteDataBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.xuehuaInboundService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "入库单-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<XuehuaInbound> queryById(@RequestParam(name = "id", required = true) Integer id) {
        XuehuaInbound xuehuaInbound = xuehuaInboundService.getById(id);
        if (xuehuaInbound == null) {
            return Result.error(GlobalErrorEnum.NO_CORRESPONDING_DATA_FOUND.getCode());
        }
        return Result.OK(xuehuaInbound);
    }

    /**
     * 导出excel
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, XuehuaInbound xuehuaInbound) {
        return super.exportXls(request, xuehuaInbound, XuehuaInbound.class, "入库单");
    }

    /**
     * 通过excel导入数据
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, XuehuaInbound.class);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Integer parseInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number num) {
            return num.intValue();
        }
        try {
            String s = String.valueOf(o).trim();
            return s.isEmpty() ? null : Integer.valueOf(s);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("qty 必须是整数: " + o);
        }
    }
}