package com.keytech.xuehua.controller;

import com.keytech.xuehua.entity.XuehuaDisposal;
import com.keytech.xuehua.service.IXuehuaAgingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库龄分析 Controller（说明书 3.5.2：库龄分布 / 超期预警 / 分类分析 + 原型处置审批流）
 *
 * <p>四层规范对齐版：业务逻辑全部下沉 {@link IXuehuaAgingService}，
 * 这里只做参数接收 / Result 包装 / IllegalStateException → Result.error 转换
 * （与入库/出库 Controller 同款异常约定）。
 */
@Tag(name = "库龄分析")
@RestController
@RequestMapping("/xuehua/aging")
@Slf4j
public class XuehuaAgingController {

    @Resource
    private IXuehuaAgingService agingService;

    /**
     * 在库库龄明细（分页 + 筛选在内存行上做：量级=在库库位数，几十行）
     * 返回行：binCode/zoneCode/partCode/partName/category/qty/firstPutaway/agingDays/status(0正常1偏高2超期)
     */
    @Operation(summary = "库龄分析-在库明细")
    @GetMapping(value = "/listData")
    public Result<Map<String, Object>> listData(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) Integer status) {
        var rows = agingService.agingRows(keyword, status);
        // 包装成 jeecg useListPage 消费的 {records, total} 结构（前端查询表单/分页与仓库管理同款）
        Map<String, Object> page = new HashMap<>(4);
        page.put("records", rows);
        page.put("total", rows.size());
        return Result.OK(page);
    }

    /**
     * 统计聚合：五档分布（件数）+ 分类分析 + KPI（在库件数/超期件数/超期库位数/最高库龄）
     */
    @Operation(summary = "库龄分析-统计聚合")
    @GetMapping(value = "/stat")
    public Result<Map<String, Object>> stat() {
        return Result.OK(agingService.stat());
    }

    /** 超期库位的有效处置（最新一条，非驳回）——明细行"处置"列四态判定用 */
    @Operation(summary = "库龄分析-处置记录")
    @GetMapping(value = "/disposal")
    public Result<List<XuehuaDisposal>> disposalList(
            @RequestParam(name = "binCode", required = false) String binCode,
            @RequestParam(name = "pendingOnly", required = false, defaultValue = "false") boolean pendingOnly) {
        return Result.OK(agingService.disposalList(binCode, pendingOnly));
    }

    /**
     * 发起处置申请（超期库位）：落 0待审批。
     * 同库位已有待审批时拒绝（防重复发起）
     */
    @AutoLog(value = "库龄分析-发起处置")
    @Operation(summary = "库龄分析-发起处置")
    @PostMapping(value = "/disposal/apply")
    public Result<String> apply(@RequestBody XuehuaDisposal body) {
        try {
            agingService.applyDisposal(body);
            return Result.OK("处置申请已提交，待库房主管审批");
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审批（通过/驳回）：Service 内条件状态流转防并发双审
     */
    @AutoLog(value = "库龄分析-处置审批")
    @Operation(summary = "库龄分析-处置审批(通过/驳回)")
    @PostMapping(value = "/disposal/approve")
    public Result<String> approve(@RequestParam(name = "id") Integer id,
                                  @RequestParam(name = "pass") boolean pass,
                                  @RequestParam(name = "note", required = false) String note) {
        try {
            agingService.approveDisposal(id, pass, note);
            return Result.OK(pass ? "审批通过" : "已驳回");
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }
}
