package com.keytech.xuehua.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.keytech.base.service.impl.BaseServiceImpl;
import com.keytech.xuehua.entity.XuehuaBin;
import com.keytech.xuehua.entity.XuehuaOutbound;
import com.keytech.xuehua.entity.XuehuaOutboundDetail;
import com.keytech.xuehua.mapper.XuehuaBinMapper;
import com.keytech.xuehua.mapper.XuehuaOutboundDetailMapper;
import com.keytech.xuehua.mapper.XuehuaOutboundMapper;
import com.keytech.xuehua.ptl.PtlClient;
import com.keytech.xuehua.service.IXuehuaInventoryService;
import com.keytech.xuehua.service.IXuehuaOutboundService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 出库工单 服务实现类
 *
 * <p>业务全流程（2026-09-13 自动分配模型）：领料单进来（EMIS 推送或页面申请，
 * 只带 备件+数量+人员，不填库位）→ 盘点冻结拦截 → 生成 WO- 工单号 →
 * <b>库龄优先 FIFO 自动分配</b>拣配行（可能多库位）→ 点亮首行库位 PTL 标签（蓝色闪烁）
 * → 拣货人按标签 OKKey → 平台 Webhook 回调 → confirmPickLine 占行+原子扣台账
 * → 回调应答链式点亮下一行库位（跨标签），全部行完成工单转"已出库"。
 *
 * <p>状态机（头表）：0 待拣配 / 1 拣配中 / 2 已出库。行表：0 待拣配 / 1 已拣配。
 *
 * <p>并发防护：
 * <ul>
 *   <li>工单号：pg_advisory_xact_lock(9702) + countNoAll 含软删除查重 + 唯一索引（三件套）；
 *       EMIS 外部单号 external_no 可空唯一索引幂等判重；</li>
 *   <li>行确认：占行条件更新 WHERE status=0（双击恰一胜者）+ 扣减 WHERE qty&gt;=n（防负库存，
 *       不足抛异常整体回滚——单参重载自带 @Transactional，防止自调用绕过事务的真实踩坑再现）；</li>
 *   <li>分配不预留（已知简化，用户确认）：两单并发分配可能超配，由行确认的原子扣减兜底。</li>
 * </ul>
 */
@Slf4j
@Service
public class XuehuaOutboundServiceImpl extends BaseServiceImpl<XuehuaOutboundMapper, XuehuaOutbound> implements IXuehuaOutboundService {

    @Resource
    private XuehuaBinMapper xuehuaBinMapper;

    @Resource
    private XuehuaOutboundDetailMapper outboundDetailMapper;

    @Resource
    private PtlClient ptlClient;

    @Resource
    private IXuehuaInventoryService xuehuaInventoryService;

    @Resource
    private com.keytech.xuehua.service.IXuehuaMonitorService xuehuaMonitorService;

    @Override
    public Map<String, Object> stat() {
        Map<String, Object> result = new HashMap<>(8);
        // 按状态分组计数（一条 SQL 拿到各状态的数量，避免多次查询）
        List<Map<String, Object>> rows = this.listMaps(
                new QueryWrapper<XuehuaOutbound>().select("status", "count(*) as cnt").groupBy("status"));
        long total = 0L;
        // 预置 s0/s1/s2 = 0：某状态恰好无工单时分组结果没有它的行，前端概览卡仍需要这三个 key
        for (int i = 0; i <= 2; i++) {
            result.put("s" + i, 0L);
        }
        for (Map<String, Object> row : rows) {
            Object status = row.get("status");
            Object cnt = row.get("cnt");
            long count = cnt == null ? 0L : Long.parseLong(cnt.toString());
            result.put("s" + status, count);
            total += count;
        }
        result.put("total", total);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<XuehuaOutboundDetail> addOutbound(XuehuaOutbound entity) {
        // 盘点冻结拦截：冻结期间账实可能正在校正，禁止新增出库申请/EMIS 推单
        if (xuehuaInventoryService.isFrozen()) {
            throw new IllegalStateException("库存盘点冻结中，禁止出库申请");
        }
        // 【防撞号第一层】事务级咨询锁串行化"查重→定号"临界区（9702 出库专属，
        // 事务提交/回滚自动释放）；EMIS 幂等由 external_no 唯一索引在 receive 端点判重，不走这里
        this.baseMapper.advisoryLock();
        if (StrUtil.isBlank(entity.getWoNo())) {
            entity.setWoNo(generateNo("WO"));
        }
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        entity.setCreateTime(new Date());
        // 【库龄优先 FIFO 分配】领料单不填库位——按备件在库库位的最早上架时间升序拆行，
        // 总量不足整单拒绝（异常抛出 → 事务回滚，头表不落库）
        List<XuehuaOutboundDetail> lines = allocateLines(entity.getPartCode(), entity.getQty());
        // 首拣库位冗余到头表：列表/导出/工作台展示兼容（实际拣配以行表为准）
        entity.setBinCode(lines.get(0).getBinCode());
        boolean ok = this.save(entity);
        if (!ok) {
            throw new IllegalStateException("领用申请保存失败");
        }
        for (XuehuaOutboundDetail line : lines) {
            line.setOutboundId(entity.getId());
            line.setWoNo(entity.getWoNo());
            line.setPartCode(entity.getPartCode());
            line.setCreateTime(new Date());
            outboundDetailMapper.insert(line);
        }
        // 点亮首行库位 PTL 标签（蓝闪+首行数量+该库位现存量，拣完到下一库位时由回调
        // 应答链式点亮下一行标签）。
        // 【关键】挪到事务提交后(afterCommit)才发 HTTP：临界区内不做网络调用（防灯光平台
        // 慢拖住排队等锁的其他申请），且回滚绝不会亮出"幽灵灯"
        XuehuaOutboundDetail first = lines.get(0);
        XuehuaBin bin = xuehuaBinMapper.selectOne(
                new QueryWrapper<XuehuaBin>().eq("bin_code", first.getBinCode()).last("limit 1"));
        if (bin != null) {
            // 匿名内部类只能捕获 final 局部变量
            final String woNo = entity.getWoNo();
            final Integer tagAdd = bin.getTagAdd();
            final Integer lineQty = first.getQty();
            final Integer stockNow = bin.getQty() == null ? 0 : bin.getQty();
            final String partCode = entity.getPartCode();
            final String partName = entity.getPartName();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 工单号回显在标签屏上，OKKey 时平台将其作为 orderNo 回传，是回调路由的依据
                    ptlClient.lightOn(PtlClient.TASK_OUTBOUND, woNo, tagAdd, lineQty,
                            PtlClient.COLOR_BLUE, partCode, partName, stockNow);
                }
            });
        }
        return lines;
    }

    /**
     * 库龄优先 FIFO 分配：对在库的该备件库位按"最早确认上架时间"升序贪心凑满申请量
     *
     * <p>库龄口径（用户确认，库位级）：MIN(该库位 status=1 上架记录的 putaway_time)，
     * 无上架记录的历史库位（台账由盘点补录）排最后；同龄按库位编码稳定序。
     * 分配不预留库存（已知简化）：并发申请可能超配，最终由行确认的
     * 原子扣减 WHERE qty&gt;=n 兜底，扣不到整体回滚并提示盘点校正。
     *
     * @return 拣配行列表（seq 1..n，未落库）
     * @throws IllegalStateException 备件/数量非法 或 全库总量不足
     */
    private List<XuehuaOutboundDetail> allocateLines(String partCode, Integer needQty) {
        if (StrUtil.isBlank(partCode)) {
            throw new IllegalStateException("备件编码为空，无法分配库位");
        }
        if (needQty == null || needQty <= 0) {
            throw new IllegalStateException("领用数量必须大于 0");
        }
        // 子查询取该库位最早确认上架时间作库龄（同一行 SQL 内完成，避免 N+1 查询）
        List<Map<String, Object>> rows = xuehuaBinMapper.selectMaps(new QueryWrapper<XuehuaBin>()
                .select("bin_code", "qty",
                        "(SELECT MIN(p.putaway_time) FROM mes4_xuehua_putaway p"
                                + " WHERE p.del_flag = 0 AND p.status = 1 AND p.bin_code = mes4_xuehua_bin.bin_code) AS first_putaway")
                .eq("status", 1)
                .eq("part_code", partCode)
                .gt("qty", 0));
        // 库龄升序、NULL（无上架记录）最后、同龄按库位编码稳定序
        rows.sort((a, b) -> {
            Object ta = a.get("first_putaway"), tb = b.get("first_putaway");
            int byAge;
            if (ta == null && tb == null) {
                byAge = 0;
            } else if (ta == null) {
                return 1;
            } else if (tb == null) {
                return -1;
            } else {
                byAge = ((Comparable) ta).compareTo(tb);
            }
            return byAge != 0 ? byAge
                    : String.valueOf(a.get("bin_code")).compareTo(String.valueOf(b.get("bin_code")));
        });
        long total = rows.stream().mapToLong(r -> Long.parseLong(String.valueOf(r.get("qty")))).sum();
        if (total < needQty) {
            throw new IllegalStateException("备件 " + partCode + " 全库可用仅 " + total
                    + " 件，申请 " + needQty + " 件，请调整数量");
        }
        // 贪心拆行：库龄最大的库位先给，给满再给下一库位
        List<XuehuaOutboundDetail> lines = new ArrayList<>(rows.size());
        int remaining = needQty;
        int seq = 1;
        for (Map<String, Object> r : rows) {
            if (remaining <= 0) {
                break;
            }
            int take = (int) Math.min(Long.parseLong(String.valueOf(r.get("qty"))), remaining);
            XuehuaOutboundDetail line = new XuehuaOutboundDetail()
                    .setBinCode(String.valueOf(r.get("bin_code")))
                    .setQty(take)
                    .setSeq(seq++)
                    .setStatus(0);
            lines.add(line);
            remaining -= take;
        }
        return lines;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public XuehuaOutboundDetail confirmPick(Integer id) {
        // 【勿删此注解】页面路径 Controller 调的是本单参重载；内部 this.confirmPick(id,null)
        // 是自调用不走代理，两参方法的 @Transactional 对它无效——事务必须在本方法上声明
        // （Controller 经接口调用会过代理，注解生效，内部代码即运行在事务里）。
        // 真实教训（2026-09-13 压测暴露）：缺注解时"扣库存失败抛异常"无法回滚已提交的
        // 更新，造成"单据已出库但库存没扣"的半截状态
        return confirmPick(id, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public XuehuaOutboundDetail confirmPick(Integer id, String operator) {
        XuehuaOutbound outbound = this.getById(id);
        if (outbound == null) {
            throw new IllegalStateException("出库工单不存在");
        }
        // 页面按钮语义：一按确认一行（多库位单按 seq 顺序逐行拣）
        XuehuaOutboundDetail line = outboundDetailMapper.selectOne(new QueryWrapper<XuehuaOutboundDetail>()
                .eq("outbound_id", id).eq("status", 0)
                .orderByAsc("seq").last("limit 1"));
        if (line == null) {
            throw new IllegalStateException("工单已出库，不可重复确认");
        }
        XuehuaOutboundDetail picked = confirmPickLine(line.getId(), operator);
        if (picked == null) {
            // 并发输家：占行被对手抢到（claimed=0 幂等返回 null）——对页面要如实报"已出库"，
            // 不能把 null 当成功回给前端（2026-09-13 自测发现）
            throw new IllegalStateException("工单已出库，不可重复确认");
        }
        return picked;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public XuehuaOutboundDetail confirmPickByWoNo(String woNo, Integer tagAdd, String operator) {
        // PTL OKKey 标准路径：一张单拆多库位时，按键标签(tagAdd→库位)决定确认哪一行；
        // 该库位没有该单的待拣行（如人在别的库位盲按）则退化为该单最早待拣行
        XuehuaOutboundDetail line = null;
        if (tagAdd != null) {
            XuehuaBin bin = xuehuaBinMapper.selectOne(
                    new QueryWrapper<XuehuaBin>().eq("tag_add", tagAdd).last("limit 1"));
            if (bin != null) {
                line = outboundDetailMapper.selectOne(new QueryWrapper<XuehuaOutboundDetail>()
                        .eq("wo_no", woNo).eq("bin_code", bin.getBinCode()).eq("status", 0)
                        .orderByAsc("seq").last("limit 1"));
            }
        }
        if (line == null) {
            line = outboundDetailMapper.selectOne(new QueryWrapper<XuehuaOutboundDetail>()
                    .eq("wo_no", woNo).eq("status", 0)
                    .orderByAsc("seq").last("limit 1"));
        }
        if (line == null) {
            // 该单无待拣行：已完成或单号不存在——幂等应答，不抛异常（网关重试安全）
            return null;
        }
        return confirmPickLine(line.getId(), operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public XuehuaOutboundDetail confirmPickByTagAdd(Integer tagAdd, String operator) {
        // 兜底路径：回调未回带单号（清屏后盲按）时，标签→库位→该库位最早待拣行（跨工单）。
        // ORDER BY id：行创建顺序即申请顺序——修复旧实现无 ORDER BY 的 limit 1 随机取行隐患
        if (tagAdd == null) {
            return null;
        }
        XuehuaBin bin = xuehuaBinMapper.selectOne(
                new QueryWrapper<XuehuaBin>().eq("tag_add", tagAdd).last("limit 1"));
        if (bin == null) {
            // 标签没绑定任何库位，无法定位拣配行
            return null;
        }
        XuehuaOutboundDetail line = outboundDetailMapper.selectOne(new QueryWrapper<XuehuaOutboundDetail>()
                .eq("bin_code", bin.getBinCode()).eq("status", 0)
                .orderByAsc("id").last("limit 1"));
        if (line == null) {
            return null;
        }
        return confirmPickLine(line.getId(), operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public XuehuaOutboundDetail confirmPickLine(Integer lineId, String operator) {
        // 事务范围：占行 + 扣台账 + 头表推进必须同生共死；任意一步失败整体回滚
        //（本方法可能经 confirmPick/confirmPickByWoNo 等入口进来，各入口均已带事务注解）
        XuehuaOutboundDetail line = outboundDetailMapper.selectById(lineId);
        if (line == null) {
            return null;
        }
        // 【占行】条件更新 WHERE status=0：并发双击按键/网关重试同时进来，UPDATE 行级串行，
        // 第一个改到 1 行占行成功，第二个 WHERE 不满足改到 0 行 → 幂等返回 null
        int claimed = outboundDetailMapper.update(null, new UpdateWrapper<XuehuaOutboundDetail>()
                .eq("id", lineId)
                .eq("status", 0)
                .set("status", 1)
                .set("pick_time", new Date())
                .set(StrUtil.isNotBlank(operator), "pick_by", operator)
                .set("update_time", new Date()));
        if (claimed == 0) {
            return null;
        }
        // 【扣台账】该行库位原子扣减（算术进 SET，行锁内串行防丢失更新）：
        // WHERE qty>=n 把"检查+扣减"焊成一条语句，扣不出负库存；
        // 影响行数=0 即库存不足 → 抛异常整个事务回滚（含上面的占行），提示盘点校正。
        // PG 的 SET 右侧引用列一律取更新前旧值，所以 status/part_code 的 CASE 必须重算 "qty - n"
        int n = line.getQty() == null ? 0 : line.getQty();
        int deducted = xuehuaBinMapper.update(null, new UpdateWrapper<XuehuaBin>()
                .eq("bin_code", line.getBinCode())
                .ge("qty", n)
                .setSql("status = CASE WHEN qty - " + n + " <= 0 THEN 0 ELSE 1 END")
                .setSql("qty = qty - " + n)
                .setSql("part_code = CASE WHEN qty - " + n + " <= 0 THEN NULL ELSE part_code END")
                .set("update_time", new Date()));
        if (deducted == 0) {
            throw new IllegalStateException("库位 " + line.getBinCode()
                    + " 库存不足，无法出库 " + n + " 件，请先盘点校正库存");
        }
        // 【头表推进】该单已无待拣行 → 2 已出库；否则从待拣配推进到 1 拣配中
        long pending = outboundDetailMapper.selectCount(new QueryWrapper<XuehuaOutboundDetail>()
                .eq("outbound_id", line.getOutboundId())
                .eq("status", 0));
        if (pending == 0) {
            this.update(new UpdateWrapper<XuehuaOutbound>()
                    .eq("id", line.getOutboundId())
                    .in("status", 0, 1)
                    .set("status", 2)
                    .set("update_time", new Date()));
            // 【出库完成判定（监控管理/EMIS 回传）】全部拣配行完成 → 通知回传 EMIS
            xuehuaMonitorService.notifyAfterCommit(() -> xuehuaMonitorService.notifyOutboundDone(line.getWoNo()));
        } else {
            this.update(new UpdateWrapper<XuehuaOutbound>()
                    .eq("id", line.getOutboundId())
                    .eq("status", 0)
                    .set("status", 1)
                    .set("update_time", new Date()));
        }
        // 熄灭当前行库位标签（主动通道）。链式场景：回调层在应答体里携带下一行任务
        // 重新点亮下一库位标签——先灭后亮由平台按到达顺序执行，最终态是亮下一行；
        // 页面确认路径没有应答反控，这次主动熄灯就是唯一熄灯通道
        XuehuaBin bin = xuehuaBinMapper.selectOne(
                new QueryWrapper<XuehuaBin>().eq("bin_code", line.getBinCode()).last("limit 1"));
        if (bin != null) {
            ptlClient.lightOff(PtlClient.TASK_OUTBOUND, line.getWoNo(), bin.getTagAdd());
        }
        // 回填确认结果给调用方（回调层取 binCode/woNo 做链式下发）
        line.setStatus(1);
        line.setPickBy(StrUtil.isNotBlank(operator) ? operator : line.getPickBy());
        return line;
    }

    @Override
    public XuehuaOutboundDetail findNextLineByWoNo(String woNo) {
        if (StrUtil.isBlank(woNo)) {
            return null;
        }
        // 跨库位链式：同单下一行按 seq（分配时的库龄序）
        return outboundDetailMapper.selectOne(new QueryWrapper<XuehuaOutboundDetail>()
                .eq("wo_no", woNo).eq("status", 0)
                .orderByAsc("seq").last("limit 1"));
    }

    @Override
    public XuehuaOutboundDetail findNextLineByBinCode(String binCode) {
        if (StrUtil.isBlank(binCode)) {
            return null;
        }
        // 同库位压单链式：跨工单按行创建顺序（id）即申请先后
        return outboundDetailMapper.selectOne(new QueryWrapper<XuehuaOutboundDetail>()
                .eq("bin_code", binCode).eq("status", 0)
                .orderByAsc("id").last("limit 1"));
    }

    @Override
    public List<XuehuaOutboundDetail> listLinesByWoNo(String woNo) {
        if (StrUtil.isBlank(woNo)) {
            return new ArrayList<>(0);
        }
        return outboundDetailMapper.selectList(new QueryWrapper<XuehuaOutboundDetail>()
                .eq("wo_no", woNo)
                .orderByAsc("seq"));
    }

    @Override
    public XuehuaOutbound findByWoNo(String woNo) {
        if (StrUtil.isBlank(woNo)) {
            return null;
        }
        return this.getOne(new QueryWrapper<XuehuaOutbound>()
                .eq("wo_no", woNo).orderByDesc("id").last("limit 1"));
    }

    /**
     * 生成工单号：WO-年份-4位序号
     *
     * <p>防撞号三层配合（锁在调用方 addOutbound 事务内拿）：
     * do-while 用 countNoAll 原生计数查重（含软删除行——唯一索引管全表而 MP 自动过滤
     * 看不见"幽灵号"，WO-2026-0009 真实撞过）+ 咨询锁串行 + 唯一索引兜底。
     */
    private String generateNo(String prefix) {
        String year = String.valueOf(new Date().getYear() + 1900);
        long seq = this.count() + 1;
        String no;
        do {
            no = prefix + "-" + year + "-" + String.format("%04d", seq++);
        } while (this.baseMapper.countNoAll(no) > 0);
        return no;
    }
}
