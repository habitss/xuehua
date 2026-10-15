package com.keytech.xuehua.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.keytech.xuehua.entity.XuehuaInbound;
import com.keytech.xuehua.entity.XuehuaInventory;
import com.keytech.xuehua.entity.XuehuaOutbound;
import com.keytech.xuehua.mapper.XuehuaInboundMapper;
import com.keytech.xuehua.mapper.XuehuaInventoryMapper;
import com.keytech.xuehua.mapper.XuehuaOutboundMapper;
import com.keytech.xuehua.service.IXuehuaWorkorderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 工单中心 服务实现（说明书 3.5.1）
 *
 * <p>三域拉平（原型 woRows 同构）：入库(IN-)/出库(WO-)/盘点(PV-)各取全部在库单据，
 * Java 侧统一装饰状态文案/状态色并过滤——单据量级几十到几百行，内存过滤足够
 * （与库龄明细同款决策）；SQL UNION 需要跨三套状态码翻译，内存里做更直白。
 */
@Slf4j
@Service
public class XuehuaWorkorderServiceImpl implements IXuehuaWorkorderService {

    @Resource
    private XuehuaInboundMapper inboundMapper;
    @Resource
    private XuehuaOutboundMapper outboundMapper;
    @Resource
    private XuehuaInventoryMapper inventoryMapper;

    @Override
    public List<Map<String, Object>> workorderRows(String keyword, String woType, Date from, Date to) {
        List<Map<String, Object>> rows = new ArrayList<>();

        // 入库（登记域）：0待登记 1已登记；执行人=登记操作人
        for (XuehuaInbound i : inboundMapper.selectList(new QueryWrapper<XuehuaInbound>().orderByDesc("create_time"))) {
            Map<String, Object> r = baseRow("入库", i.getInboundNo(), i.getPartName(), i.getQty(), i.getOperator(), i.getCreateTime());
            int s = i.getStatus() == null ? 0 : i.getStatus();
            r.put("statusCode", s);
            r.put("statusText", s == 1 ? "已登记" : "待登记");
            rows.add(r);
        }
        // 出库：0待拣配 1拣配中 2已出库；执行人=领用人（空回落申请人）
        for (XuehuaOutbound o : outboundMapper.selectList(new QueryWrapper<XuehuaOutbound>().orderByDesc("create_time"))) {
            Map<String, Object> r = baseRow("出库", o.getWoNo(), o.getPartName(), o.getQty(),
                    StrUtil.blankToDefault(o.getReceiveUser(), o.getApplyUser()), o.getCreateTime());
            int s = o.getStatus() == null ? 0 : o.getStatus();
            r.put("statusCode", s);
            r.put("statusText", s == 2 ? "已出库" : s == 1 ? "拣配中" : "待拣配");
            rows.add(r);
        }
        // 盘点：0待 1盘点中 2有差异 3已完成 4已终止；内容=区域 · 范围（空段跳过，避免前导点号）；数量=账面数
        for (XuehuaInventory p : inventoryMapper.selectList(new QueryWrapper<XuehuaInventory>().orderByDesc("create_time"))) {
            String content = Stream.of(p.getZoneCode(), p.getScope())
                    .filter(s -> StrUtil.isNotBlank(s) && !"null".equals(s)).collect(Collectors.joining(" · "));
            Map<String, Object> r = baseRow("盘点", p.getPvNo(), content, p.getBookQty(), p.getExecutor(), p.getCreateTime());
            int s = p.getStatus() == null ? 0 : p.getStatus();
            r.put("statusCode", s);
            r.put("statusText", new String[] {"待盘点", "盘点中", "有差异", "已完成", "已终止"}[s]);
            rows.add(r);
        }

        // 状态色（原型规则：待/拣配中/盘点中=warn，退/差异/超期=err，其余=ok）
        for (Map<String, Object> r : rows) {
            String st = String.valueOf(r.get("statusText"));
            r.put("statusTag", st.matches(".*(待|拣配中|盘点中).*") ? "warn"
                    : st.matches(".*(退|差异|超期|终止).*") ? "err" : "ok");
        }

        // 过滤：类型 / 关键字 / 时间区间；时间倒序
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows) {
            if (StrUtil.isNotBlank(woType) && !woType.equals(r.get("woType"))) {
                continue;
            }
            if (StrUtil.isNotBlank(keyword)) {
                String k = keyword.trim().toLowerCase();
                String hay = String.join(" ", String.valueOf(r.get("no")), String.valueOf(r.get("content")),
                        String.valueOf(r.get("person")), String.valueOf(r.get("statusText"))).toLowerCase();
                if (!hay.contains(k)) {
                    continue;
                }
            }
            Date t = (Date) r.get("time");
            if (from != null && t != null && t.before(from)) {
                continue;
            }
            if (to != null && t != null && t.after(to)) {
                continue;
            }
            result.add(r);
        }
        result.sort((a, b) -> ((Date) b.get("time")).compareTo((Date) a.get("time")));
        return result;
    }

    @Override
    public Map<String, Object> stat() {
        List<Map<String, Object>> all = workorderRows(null, null, null, null);
        Map<String, Object> result = new LinkedHashMap<>(8);
        long total = 0, done = 0;
        for (String type : new String[] {"入库", "出库", "盘点"}) {
            long t = 0, d = 0;
            for (Map<String, Object> r : all) {
                if (!type.equals(r.get("woType"))) {
                    continue;
                }
                t++;
                // 完成判定：入库=已登记(1)；出库=已出库(2)；盘点=已完成(3)
                int code = (int) r.get("statusCode");
                boolean isDone = ("入库".equals(type) && code == 1) || ("出库".equals(type) && code == 2) || ("盘点".equals(type) && code == 3);
                if (isDone) {
                    d++;
                }
            }
            String key = "入库".equals(type) ? "ib" : "出库".equals(type) ? "ob" : "pv";
            Map<String, Object> item = new HashMap<>(4);
            item.put("total", t);
            item.put("done", d);
            result.put(key, item);
            total += t;
            done += d;
        }
        result.put("total", total);
        result.put("done", done);
        return result;
    }

    private Map<String, Object> baseRow(String type, String no, String content, Integer qty, String person, Date time) {
        Map<String, Object> r = new LinkedHashMap<>(8);
        r.put("no", no);
        r.put("woType", type);
        r.put("content", content);
        r.put("qty", qty == null ? 0 : qty);
        r.put("person", StrUtil.blankToDefault(person, "—"));
        r.put("time", time);
        return r;
    }
}
