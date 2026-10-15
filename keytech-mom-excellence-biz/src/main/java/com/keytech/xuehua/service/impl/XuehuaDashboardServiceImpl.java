package com.keytech.xuehua.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.keytech.xuehua.entity.XuehuaBin;
import com.keytech.xuehua.entity.XuehuaInventory;
import com.keytech.xuehua.entity.XuehuaAccessLog;
import com.keytech.xuehua.entity.XuehuaOpLog;
import com.keytech.xuehua.entity.XuehuaOutbound;
import com.keytech.xuehua.entity.XuehuaPart;
import com.keytech.xuehua.entity.XuehuaPutaway;
import com.keytech.xuehua.mapper.XuehuaAccessLogMapper;
import com.keytech.xuehua.mapper.XuehuaBinMapper;
import com.keytech.xuehua.mapper.XuehuaDashboardMapper;
import com.keytech.xuehua.mapper.XuehuaInboundMapper;
import com.keytech.xuehua.mapper.XuehuaInventoryMapper;
import com.keytech.xuehua.mapper.XuehuaOpLogMapper;
import com.keytech.xuehua.mapper.XuehuaOutboundMapper;
import com.keytech.xuehua.mapper.XuehuaPartMapper;
import com.keytech.xuehua.mapper.XuehuaPutawayMapper;
import com.keytech.xuehua.service.IXuehuaDashboardService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台（运营驾驶舱）聚合查询实现
 *
 * <p>区块与原型 dashboard.html 一一对应（renderTopKpis/renderTkTodos/renderCatDonut/
 * renderCostTrend/renderCostStruct/renderZoneBars/renderTkLiveFeed/renderTkAlarms）。
 * 数据缺口处理（2026-09-13 用户拍板）：
 * - "库龄"用已确认上架记录的 putaway_time 距今分布替代（系统无独立库龄表）；
 * - "监控报警"（原 AI 视频进出异常，泳道未对接）改为库存预警：
 *   err=回调处理失败记录（op_log success=0），warn=低位库存（在库且 qty<=阈值）。
 */
@Slf4j
@Service
public class XuehuaDashboardServiceImpl implements IXuehuaDashboardService {

    /** 低位库存预警兜底阈值：备件未设置 safe_stock 时取字典 xuehua_safe_stock"默认安全库存"（与库位地图同口径） */
    private int fallbackSafeStock() {
        return xuehuaBinService.fallbackSafeStock();
    }

    @Resource
    private XuehuaDashboardMapper dashboardMapper;
    @Resource
    private XuehuaInboundMapper inboundMapper;
    @Resource
    private XuehuaOutboundMapper outboundMapper;
    @Resource
    private XuehuaInventoryMapper inventoryMapper;
    @Resource
    private XuehuaPutawayMapper putawayMapper;
    @Resource
    private XuehuaPartMapper partMapper;
    @Resource
    private XuehuaBinMapper binMapper;
    @Resource
    private com.keytech.xuehua.service.IXuehuaBinService xuehuaBinService;
    @Resource
    private XuehuaOpLogMapper opLogMapper;

    @Resource
    private XuehuaAccessLogMapper accessLogMapper;

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> result = new HashMap<>(16);

        // ===== KPI 概览（5卡）：服务工单/入库/出库/在库备件/库龄超期 =====
        long ibCnt = inboundMapper.selectCount(null);
        long obCnt = outboundMapper.selectCount(null);
        long pvCnt = inventoryMapper.selectCount(null);
        long partKinds = partMapper.selectCount(null);
        long overAged = dashboardMapper.countOverAged();
        Map<String, Object> kpi = new LinkedHashMap<>(8);
        kpi.put("workOrders", ibCnt + obCnt + pvCnt);
        kpi.put("inbound", ibCnt);
        kpi.put("outbound", obCnt);
        kpi.put("partKinds", partKinds);
        kpi.put("overAged", overAged);
        result.put("kpi", kpi);

        // ===== 今日待办（5卡）：待上架/待拣配/待盘点/盘点有差异/低位库存预警 =====
        Map<String, Object> todos = new LinkedHashMap<>(8);
        todos.put("toPutaway", putawayMapper.selectCount(new QueryWrapper<XuehuaPutaway>().eq("status", 0)));
        todos.put("toPick", outboundMapper.selectCount(new QueryWrapper<XuehuaOutbound>().eq("status", 0)));
        todos.put("toInventory", inventoryMapper.selectCount(new QueryWrapper<XuehuaInventory>().eq("status", 0)));
        todos.put("diffInventory", inventoryMapper.selectCount(new QueryWrapper<XuehuaInventory>().eq("status", 2)));
        // 低位库存预警口径=备件安全库存（safe_stock，未设置用字典默认），与库位地图报警一致
        todos.put("lowStock", dashboardMapper.lowStockCount(fallbackSafeStock()));
        result.put("todos", todos);

        // ===== 备件分类占比：part 按类别计数（跳过空类别归"其他"） =====
        List<Map<String, Object>> catRows = partMapper.selectMaps(
                new QueryWrapper<XuehuaPart>().select("category", "count(*) AS cnt")
                        .isNotNull("category").groupBy("category")
                        .orderByDesc("count(*)"));
        List<Map<String, Object>> catDonut = new ArrayList<>(catRows.size());
        long otherCnt = 0;
        for (Map<String, Object> row : catRows) {
            String name = row.get("category") == null ? "" : row.get("category").toString().trim();
            long cnt = row.get("cnt") == null ? 0 : Long.parseLong(row.get("cnt").toString());
            if (name.isEmpty()) {
                otherCnt += cnt;
            } else {
                Map<String, Object> cat = new HashMap<>(4);
                cat.put("n", name);
                cat.put("v", cnt);
                catDonut.add(cat);
            }
        }
        if (otherCnt > 0) {
            Map<String, Object> other = new HashMap<>(4);
            other.put("n", "其他");
            other.put("v", otherCnt);
            catDonut.add(other);
        }
        result.put("catDonut", catDonut);

        // ===== 出入库趋势（近12个月，后端已补零） / 库龄结构 / 分区占用 =====
        result.put("trend", dashboardMapper.monthlyTrend());
        result.put("aging", dashboardMapper.agingBuckets());
        result.put("zones", dashboardMapper.zoneUsage());

        // ===== 实时出入库（最新6条） =====
        result.put("liveFeed", dashboardMapper.liveFeed());

        // ===== 监控报警：err=进出异常事件（非授权/尾随，access_log） / warn=低位库存明细 =====
        List<XuehuaAccessLog> anomalies = accessLogMapper.selectList(new QueryWrapper<XuehuaAccessLog>()
                .in("event_type", XuehuaAccessLog.EVT_UNAUTHORIZED, XuehuaAccessLog.EVT_TAILGATE)
                .orderByDesc("event_time").last("limit 5"));
        int fallbackSafe = fallbackSafeStock();
        List<Map<String, Object>> lowBins = dashboardMapper.lowStockBins(fallbackSafe);
        long errCount = accessLogMapper.selectCount(new QueryWrapper<XuehuaAccessLog>()
                .in("event_type", XuehuaAccessLog.EVT_UNAUTHORIZED, XuehuaAccessLog.EVT_TAILGATE));
        long warnCount = dashboardMapper.lowStockCount(fallbackSafe);
        List<Map<String, Object>> items = new ArrayList<>(anomalies.size() + lowBins.size());
        for (XuehuaAccessLog a : anomalies) {
            Map<String, Object> item = new HashMap<>(4);
            String evName = a.getEventType() == XuehuaAccessLog.EVT_TAILGATE ? "尾随进入" : "非授权进入";
            item.put("lv", "err");
            item.put("t", evName + " · " + StrUtil.blankToDefault(a.getZone(), "—"));
            item.put("m", (a.getEventTime() == null ? "" : a.getEventTime().toString().substring(5, 16))
                    + " · " + StrUtil.blankToDefault(a.getPersonName(), "未识别") + " · 已关联监控时间线");
            items.add(item);
        }
        for (Map<String, Object> b : lowBins) {
            Map<String, Object> item = new HashMap<>(4);
            item.put("lv", "warn");
            item.put("t", "低库存预警 · " + b.get("binCode"));
            item.put("m", b.get("partCode") + " · 现存 " + b.get("qty")
                    + " · 已低于安全库存 " + b.get("safeStock") + " · 建议补货");
            items.add(item);
        }
        Map<String, Object> alarms = new LinkedHashMap<>(4);
        alarms.put("errCount", errCount);
        alarms.put("warnCount", warnCount);
        alarms.put("items", items);
        result.put("alarms", alarms);
        return result;
    }
}
