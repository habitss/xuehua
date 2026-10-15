package com.keytech.xuehua.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keytech.base.controller.BaseController;
import com.keytech.xuehua.entity.XuehuaPerson;
import com.keytech.xuehua.service.IXuehuaPersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.query.QueryGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 人员注册 Controller（监控管理）
 *
 * <p>【只读+启停】人员档案源头在海康（注册信息由海康推送到 /xuehua/hik/person，
 * WCS 只记录）；本 Controller 不提供新增/编辑/删除——防止绕过海康产生无档案人员。
 * EMIS 到货单/领料单的操作人校验即查询本模块（按工号/姓名）。
 */
@Tag(name = "人员注册")
@RestController
@RequestMapping("/xuehua/person")
@Slf4j
public class XuehuaPersonController extends BaseController<IXuehuaPersonService, XuehuaPerson> {

    /**
     * 门禁远程开门（联调期连通性/应急放行验证，登录态）：
     * 门禁机 ISAPI 直连 PUT RemoteControl/door/{doorNo}。
     * 未启用时明确提示配置项，不误报成功。
     */
    @AutoLog(value = "人员注册-门禁远程开门")
    @Operation(summary = "人员注册-门禁远程开门")
    @PostMapping(value = "/remoteOpen")
    public Result<String> remoteOpen(@RequestParam(name = "doorNo", defaultValue = "1") Integer doorNo) {
        if (!xuehuaPersonService.hikRemoteOpen(doorNo)) {
            return Result.error("远程开门失败：门禁机未接入（xuehua.hik.door.enabled/base-url/password 未配置）或设备不可达");
        }
        return Result.OK("门禁 " + doorNo + " 已远程开启");
    }

    @Autowired
    private IXuehuaPersonService xuehuaPersonService;

    /** 分页列表（QueryGenerator 支持 name/empNo/dept/status 等筛选） */
    @AutoLog(value = "人员注册-分页列表")
    @Operation(summary = "人员注册-分页列表")
    @GetMapping(value = "/listData")
    public Result<IPage<XuehuaPerson>> listData(XuehuaPerson person,
                                                @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                HttpServletRequest req) {
        QueryWrapper<XuehuaPerson> qw = QueryGenerator.initQueryWrapper(person, req.getParameterMap());
        qw.orderByDesc("create_time");
        return Result.OK(xuehuaPersonService.page(new Page<>(pageNo, pageSize), qw));
    }

    /** 详情 */
    @Operation(summary = "人员注册-详情")
    @GetMapping(value = "/queryById")
    public Result<XuehuaPerson> queryById(@RequestParam(name = "id") Integer id) {
        XuehuaPerson person = xuehuaPersonService.getById(id);
        if (person == null) {
            return Result.error("人员不存在");
        }
        return Result.OK(person);
    }

    /** Excel 导出（@Excel 注解字段） */
    @Operation(summary = "人员注册-导出Excel")
    @RequestMapping(value = "/exportXls")
    public org.springframework.web.servlet.ModelAndView exportXls(HttpServletRequest request, XuehuaPerson person) {
        return super.exportXls(request, person, XuehuaPerson.class, "人员注册");
    }

    /** Excel 导入（按工号唯一，重复行会报错提示） */
    @AutoLog(value = "人员注册-导入Excel")
    @Operation(summary = "人员注册-导入Excel")
    @PostMapping(value = "/importExcel")
    public Result<?> importExcel(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        return super.importExcel(request, response, XuehuaPerson.class);
    }

    /** 启用/停用（WCS 侧作业许可开关；档案以海康为准） */
    @AutoLog(value = "人员注册-启停")
    @Operation(summary = "人员注册-启用停用")
    @PostMapping(value = "/toggleStatus")
    public Result<String> toggleStatus(@RequestParam(name = "id") Integer id,
                                       @RequestParam(name = "status") int status) {
        try {
            xuehuaPersonService.toggleStatus(id, status);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
        return Result.OK(status == 1 ? "已启用" : "已停用");
    }
}
