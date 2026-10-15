package com.keytech.xuehua.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keytech.base.controller.BaseController;
import com.keytech.xuehua.entity.XuehuaOutbound;
import com.keytech.xuehua.entity.XuehuaOutboundDetail;
import com.keytech.xuehua.service.IXuehuaOutboundService;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 出库管理（出库工单） Controller
 *
 * <p>页面四个动作对应关系：
 * 列表/概览 ← /listData + /stat；新增领用申请弹窗 → /add（落库后自动亮 PTL 蓝灯）；
 * 行内"确认出库" → /confirmPick（状态转已出库 + 释放库位 + 熄灯）。
 *
 * <p>注意：PTL 按键确认不走本 Controller——平台 Webhook 直达
 * /xuehua/ptl/tagEvent（匿名端点），由 XuehuaPtlEventServiceImpl 按单号前缀路由到 Service 层。
 */
@Tag(name = "出库管理")
@RestController
@RequestMapping("/xuehua/outbound")
@Slf4j
public class XuehuaOutboundController extends BaseController<IXuehuaOutboundService, XuehuaOutbound> {

    @Autowired
    private IXuehuaOutboundService xuehuaOutboundService;

    @Autowired
    private com.keytech.xuehua.service.IXuehuaPersonService xuehuaPersonService;

    /**
     * 分页列表查询
     *
     * <p>QueryGenerator 按 GET 参数自动拼查询条件（表单筛选项），
     * 固定按创建时间倒序——最新工单排最前
     */
    @AutoLog(value = "出库工单-分页列表查询")
    @Operation(summary = "出库工单-分页列表查询")
    @GetMapping(value = "/listData")
    public Result<IPage<XuehuaOutbound>> queryPageList(XuehuaOutbound xuehuaOutbound,
                                                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                       HttpServletRequest req) {
        QueryWrapper<XuehuaOutbound> queryWrapper = QueryGenerator.initQueryWrapper(xuehuaOutbound, req.getParameterMap());
        queryWrapper.orderByDesc("create_time");
        Page<XuehuaOutbound> page = new Page<>(pageNo, pageSize);
        IPage<XuehuaOutbound> pageList = xuehuaOutboundService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 各状态数量统计（流程概览卡）：返回 {s0待拣配, s1拣配中, s2已出库, total}
     */
    @Operation(summary = "出库工单-状态统计")
    @GetMapping(value = "/stat")
    public Result<Map<String, Object>> stat() {
        return Result.OK(xuehuaOutboundService.stat());
    }

    /**
     * 工单拣配明细行（seq 升序）——前端列表行展开/拣配指引弹窗用
     */
    @Operation(summary = "出库工单-拣配明细行")
    @GetMapping(value = "/detailLines")
    public Result<List<XuehuaOutboundDetail>> detailLines(@RequestParam(name = "woNo") String woNo) {
        return Result.OK(xuehuaOutboundService.listLinesByWoNo(woNo));
    }

    /**
     * 新增领用申请
     *
     * <p>Service 内完成：冻结拦截 → 咨询锁 → WO- 工单号生成 → 库龄优先 FIFO 自动分配
     * 拣配行（前端不填库位）→ 落头表+行表 → 事务提交后点亮首行库位 PTL 标签（蓝闪）。
     */
    @AutoLog(value = "出库工单-新增领用申请")
    @Operation(summary = "出库工单-新增领用申请")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody @Valid XuehuaOutbound xuehuaOutbound) {
        try {
            xuehuaOutboundService.addOutbound(xuehuaOutbound);
        } catch (IllegalStateException e) {
            // 业务拒绝（如全库总量不足）：转 Result.error 让前端弹 message，
            // 不走全局异常的 500"系统异常"
            return Result.error(e.getMessage());
        }
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * EMIS 领料单接收（匿名端点，shiro 白名单 /xuehua/outbound/receive 放行）
     *
     * <p>契约（2026-09-13 自定，待 EMIS 对接时可调）：
     * 请求 JSON：{externalNo(EMIS领料单号,幂等键,必填), partCode(必填), qty(必填,>0),
     * receiveUser(领用人), applyUser(申请人), workshop(车间), partName(可选)}
     * 响应 JSON：{code:200成功/500失败, msg, woNo, allocations:[{seq,binCode,qty,status}]}
     *
     * <p>语义如实（与 PTL 回调同款约定）：成功=200、真实失败=500，EMIS 重推安全——
     * external_no 可空唯一索引判重，重复推送幂等返回原工单号与分配结果。
     * 业务拒绝（冻结/总量不足等）code=500 + 中文原因。
     */
    @AutoLog(value = "出库工单-EMIS领料单接收")
    @Operation(summary = "出库工单-EMIS领料单接收(匿名)")
    @PostMapping(value = "/receive")
    public Map<String, Object> receive(@RequestBody Map<String, Object> body) {
        Map<String, Object> resp = new LinkedHashMap<>(8);
        String externalNo = str(body.get("externalNo"));
        String partCode = str(body.get("partCode"));
        String msg;
        String woNo = null;
        List<Map<String, Object>> allocations = new ArrayList<>();
        int code;
        try {
            if (StrUtil.isBlank(externalNo)) {
                throw new IllegalStateException("externalNo(EMIS领料单号)不能为空");
            }
            if (StrUtil.isBlank(partCode)) {
                throw new IllegalStateException("partCode(备件编码)不能为空");
            }
            // 【人员校验（监控管理口径）】领用人必须已在人员注册中登记——
            // 未注册人员无法获得海康门禁授权，不能进仓拣配
            String receiveUser = str(body.get("receiveUser"));
            if (StrUtil.isNotBlank(receiveUser)) {
                com.keytech.xuehua.entity.XuehuaPerson receiver = xuehuaPersonService.resolveByOperator(receiveUser);
                if (receiver == null) {
                    throw new IllegalStateException("领用人 " + receiveUser + " 未在人员注册中登记，请先注册并推送门禁授权");
                }
                // 【进仓授权】已注册 → 给海康发授权指令，失败拒单（严格模式，降级跳过）
                xuehuaPersonService.authorizeEntry(receiver);
            }
            // 幂等判重：同一领料单号重复推送 → 返回原工单与分配结果（不重复建单）
            XuehuaOutbound exist = xuehuaOutboundService.getOne(
                    new QueryWrapper<XuehuaOutbound>().eq("external_no", externalNo).last("limit 1"));
            if (exist != null) {
                resp.put("code", 200);
                resp.put("msg", "重复推送，返回原工单");
                resp.put("woNo", exist.getWoNo());
                resp.put("allocations", allocationLines(xuehuaOutboundService.listLinesByWoNo(exist.getWoNo())));
                return resp;
            }
            XuehuaOutbound order = new XuehuaOutbound()
                    .setPartCode(partCode)
                    .setPartName(str(body.get("partName")))
                    .setQty(parseInt(body.get("qty")))
                    .setReceiveUser(str(body.get("receiveUser")))
                    .setApplyUser(StrUtil.blankToDefault(str(body.get("applyUser")), "EMIS"))
                    .setWorkshop(str(body.get("workshop")))
                    .setExternalNo(externalNo)
                    .setRemark("EMIS推送");
            List<XuehuaOutboundDetail> lines = xuehuaOutboundService.addOutbound(order);
            woNo = order.getWoNo();
            code = 200;
            msg = "领料单接收成功，已自动分配拣配库位";
            allocations.addAll(allocationLines(lines));
        } catch (IllegalStateException e) {
            code = 500;
            msg = e.getMessage();
        } catch (Exception e) {
            log.warn("[EMIS] 领料单接收失败: externalNo={} err={}", externalNo, e.getMessage());
            code = 500;
            msg = "处理失败：" + e.getMessage();
        }
        resp.put("code", code);
        resp.put("msg", msg);
        resp.put("woNo", woNo);
        resp.put("allocations", allocations);
        return resp;
    }

    private List<Map<String, Object>> allocationLines(List<XuehuaOutboundDetail> lines) {
        List<Map<String, Object>> out = new ArrayList<>(lines.size());
        for (XuehuaOutboundDetail l : lines) {
            Map<String, Object> a = new LinkedHashMap<>(4);
            a.put("seq", l.getSeq());
            a.put("binCode", l.getBinCode());
            a.put("qty", l.getQty());
            a.put("status", l.getStatus());
            out.add(a);
        }
        return out;
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

    /**
     * 编辑
     */
    @AutoLog(value = "出库工单-编辑")
    @Operation(summary = "出库工单-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody @Valid XuehuaOutbound xuehuaOutbound) {
        xuehuaOutboundService.updateById(xuehuaOutbound);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除：走 @TableLogic 逻辑删除（del_flag=1），单号仍占用唯一索引
     * ——所以工单号查重必须用 countNoAll 含软删除计数，见 Mapper 说明
     */
    @AutoLog(value = "出库工单-通过id删除")
    @Operation(summary = "出库工单-通过id删除")
    @DeleteMapping(value = "/deleteData")
    public Result<String> deleteData(@RequestParam(name = "id", required = true) Integer id) {
        xuehuaOutboundService.removeById(id);
        return Result.OK(GlobalErrorEnum.SUCCESS.getCode());
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "出库工单-批量删除")
    @Operation(summary = "出库工单-批量删除")
    @DeleteMapping(value = "/deleteDataBatch")
    public Result<String> deleteDataBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.xuehuaOutboundService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "出库工单-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<XuehuaOutbound> queryById(@RequestParam(name = "id", required = true) Integer id) {
        XuehuaOutbound xuehuaOutbound = xuehuaOutboundService.getById(id);
        if (xuehuaOutbound == null) {
            return Result.error(GlobalErrorEnum.NO_CORRESPONDING_DATA_FOUND.getCode());
        }
        return Result.OK(xuehuaOutbound);
    }

    /**
     * 扫码/按钮确认出库：待拣配/拣配中 → 已出库，释放库位回空位，熄灭 PTL 标签
     *
     * <p>异常到 HTTP 的映射约定：Service 层用 IllegalStateException 表达"业务拒绝"
     * （工单不存在 / 已出库重复确认 / 并发双击的输家），这里捕获转 Result.error
     * （HTTP 200 + success=false），前端弹出 message 提示——业务拒绝不是服务器错误，
     * 不该走 5xx 让前端弹"系统异常"。
     * PTL 按键路径的同类拒绝在 XuehuaPtlEventServiceImpl 里处理，语义不同（返回 200 幂等应答）。
     */
    @AutoLog(value = "出库工单-扫码确认出库")
    @Operation(summary = "出库工单-扫码确认出库")
    @PostMapping(value = "/confirmPick")
    public Result<String> confirmPick(@RequestParam(name = "id", required = true) Integer id) {
        try {
            xuehuaOutboundService.confirmPick(id);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
        return Result.OK("出库成功，已反馈 WCS/Emis");
    }

    /**
     * 导出excel
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, XuehuaOutbound xuehuaOutbound) {
        return super.exportXls(request, xuehuaOutbound, XuehuaOutbound.class, "出库工单");
    }

    /**
     * 通过excel导入数据
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, XuehuaOutbound.class);
    }
}
