<template>
  <div class="wcs-dash">
    <!-- ===== 头部：标题 + 时钟 + 周期切换 + 快捷按钮（样式移植原型 .tk-head） ===== -->
    <div class="tk-head">
      <div class="tk-title">
        <div class="tk-logo">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#7fd8ff" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 21h18M4 21V8l8-5 8 5v13M9 21v-6h6v6M9 11h.01M15 11h.01M12 11h.01" />
          </svg>
        </div>
        <div><h2>中央共享备件库 · 运营驾驶舱</h2></div>
      </div>
      <div class="tk-meta">
        <span class="tk-time">{{ clock }}</span>
        <div class="tk-seg">
          <span v-for="p in (['day', 'week', 'month'] as const)" :key="p" class="seg" :class="{ on: period === p }" @click="period = p">
            {{ { day: '日', week: '周', month: '月' }[p] }}
          </span>
        </div>
        <button class="tk-acts" @click="nav('/xuehua/stock/map')">库位地图</button>
        <button class="tk-acts pri" @click="nav('/xuehua/inbound')">＋ 新增入库</button>
      </div>
    </div>

    <!-- ===== KPI 概览（5卡，渐变发光数字，样式移植 .tk-kpis/.tk-kpi） ===== -->
    <div class="tk-kpis">
      <div v-for="k in kpiCards" :key="k.lab" class="tk-card tk-kpi" :title="'点击查看' + k.lab" @click="k.path && nav(k.path)">
        <div class="ktop">
          <span class="lab">{{ k.lab }}</span>
          <span class="kico" :style="{ color: k.c, background: k.c + '22', borderColor: k.c + '33' }" v-html="icons[k.ic]" />
        </div>
        <div class="nrow">
          <span class="num" :style="{ backgroundImage: `linear-gradient(180deg,#ffffff,${k.c})` }">{{ k.v }}</span>
          <span class="unit">{{ k.u }}</span>
        </div>
      </div>
    </div>

    <!-- ===== 今日待办（聚合各业务状态，0 项灰显；样式移植 dashboard.html 内联 .tk-todos） ===== -->
    <div class="tk-todos">
      <div
        v-for="t in todoCards"
        :key="t.lab"
        class="tk-todo"
        :class="{ zero: !t.v }"
        @click="t.v && t.path && nav(t.path)"
      >
        <span class="td-ic" :style="{ color: t.v ? t.c : '#6f93b8' }" v-html="icons[t.ic]" />
        <div class="td-n" :style="{ color: t.v ? t.c : '#6f93b8' }">{{ t.v }}<small>{{ t.u }}</small></div>
        <div class="td-l">{{ t.lab }}</div>
      </div>
    </div>

    <!-- ===== 第一横排：分类占比 / 出入库趋势 / 库龄结构（SVG 逐函数移植） ===== -->
    <div class="tk-row c3" style="margin-bottom: 16px">
      <div class="tk-card">
        <div class="tk-ctitle"><div class="t">备件分类占比</div><span class="tk-tab">{{ catTotal }} 种</span></div>
        <div class="tk-cbody"><div v-html="catDonutSvg" /></div>
      </div>
      <div class="tk-card">
        <div class="tk-ctitle"><div class="t">出入库件数趋势</div><span class="tk-tab">近12个月 · 单</span></div>
        <div class="tk-cbody"><div v-html="trendSvg" /></div>
      </div>
      <div class="tk-card">
        <div class="tk-ctitle"><div class="t">库龄结构</div><span class="tk-tab">{{ catTotal }} 批</span></div>
        <div class="tk-cbody"><div v-html="agingSvg" /></div>
      </div>
    </div>

    <!-- ===== 第二横排：分区占用 / 实时出入库 / 库存预警 ===== -->
    <div class="tk-row c3">
      <div class="tk-card">
        <div class="tk-ctitle"><div class="t">分区库存占比</div><span class="tk-tab">红线 90%</span></div>
        <div class="tk-cbody">
          <div v-for="z in zoneRows" :key="z.name" class="tz-row">
            <div class="tz-info">
              <span class="tz-name">{{ z.name }}</span>
              <span class="tz-sub">{{ z.total }} 位</span>
            </div>
            <div class="tz-bar">
              <div class="tz-segs">
                <i
                  v-for="i in SEGS"
                  :key="i"
                  class="seg"
                  :class="{ on: i <= z.fill }"
                  :style="{ '--c': z.color, '--i': i }"
                />
              </div>
              <div class="tz-mark" style="left: 90%" />
            </div>
            <div class="tz-val">
              <b :style="{ '--c': z.color }">{{ z.pct }}<small>%</small></b>
              <span class="tz-st" :class="z.stat.c">● {{ z.stat.t }}</span>
            </div>
          </div>
          <div v-if="!zoneRows.length" class="empty">暂无库位数据</div>
        </div>
      </div>
      <div class="tk-card">
        <div class="tk-ctitle"><div class="t">实时出入库信息</div><span class="tk-tab" style="color: #ff6b7d; border-color: rgba(255, 107, 125, 0.3)">● LIVE</span></div>
        <div class="tk-cbody tk-tl">
          <div v-for="e in feed" :key="e.type + e.no" class="tev">
            <span class="tdot" :style="{ background: typeColor(e.type), boxShadow: `0 0 7px ${typeColor(e.type)}` }" />
            <div style="flex: 1">
              <div class="tmt">{{ (e.t || '').slice(5, 16) }}</div>
              <div class="tec">
                <b :style="{ color: typeColor(e.type) }">{{ e.no }}</b> · {{ e.partname }} ×{{ e.qty }} · {{ e.person }}
                <span
                  :style="{ marginLeft: '4px', color: statusColor(e.status), border: `1px solid ${statusColor(e.status)}55`, background: statusColor(e.status) + '14', borderRadius: '4px', padding: '0 6px', fontSize: '10.5px', lineHeight: '16px', whiteSpace: 'nowrap' }"
                  >{{ e.status }}</span
                >
              </div>
            </div>
          </div>
          <div v-if="!feed.length" class="empty">暂无出入库记录</div>
        </div>
      </div>
      <div class="tk-card">
        <div class="tk-ctitle">
          <div class="t">监控报警</div>
          <span class="tk-tab" style="color: #ff6b7d; border-color: rgba(255, 107, 125, 0.3)">{{ alarms.errCount }} 报警 · {{ alarms.warnCount }} 预警</span>
        </div>
        <div class="tk-cbody">
          <div v-for="(a, i) in alarms.items" :key="i" class="tk-alarm">
            <span class="alv" :class="a.lv" />
            <div>
              <div class="alt">{{ a.t }}</div>
              <div class="alm">{{ a.m }}</div>
            </div>
          </div>
          <div v-if="!alarms.items.length" class="empty">运行平稳，无报警与预警</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" name="xuehua-dashboard" setup>
  import { ref, computed, onMounted, onUnmounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { getOverview, type DashboardOverview } from './dashboard.api';

  const router = useRouter();
  const period = ref<'day' | 'week' | 'month'>('week');
  const clock = ref('');
  const ov = ref<DashboardOverview | null>(null);

  // ===== 线性图标（移植原型 li() 图标的常用子集，stroke 风格一致） =====
  const svgWrap = (paths: string) =>
    `<svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${paths}</svg>`;
  const icons: Record<string, string> = {
    file: svgWrap('<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M9 13h6M9 17h6"/>'),
    inbound: svgWrap('<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3"/>'),
    outbound: svgWrap('<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12"/>'),
    box: svgWrap('<path d="M21 8l-9-5-9 5v8l9 5 9-5zM3 8l9 5 9-5M12 13v8"/>'),
    alert: svgWrap('<path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0zM12 9v4M12 17h.01"/>'),
    inventory: svgWrap('<path d="M9 11l3 3 8-8"/><path d="M21 12v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h11"/>'),
    refresh: svgWrap('<path d="M23 4v6h-6M1 20v-6h6"/><path d="M3.5 9a9 9 0 0 1 14.9-3.4L23 10M1 14l4.6 4.4A9 9 0 0 0 20.5 15"/>'),
  };

  // ===== 时钟（移植原型 tick()） =====
  let clockTimer: ReturnType<typeof setInterval>;
  const pad = (n: number) => String(n).padStart(2, '0');
  function tickClock() {
    const d = new Date();
    clock.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }

  function nav(path: string) {
    router.push(path);
  }

  // ===== 数据加载（60s 轮询，对齐原型 LIVE 语义） =====
  let dataTimer: ReturnType<typeof setInterval>;
  async function load() {
    try {
      ov.value = await getOverview();
    } catch (e) {
      console.error('工作台数据加载失败', e);
    }
  }
  onMounted(() => {
    tickClock();
    clockTimer = setInterval(tickClock, 1000);
    load();
    dataTimer = setInterval(load, 60000);
  });
  onUnmounted(() => {
    clearInterval(clockTimer);
    clearInterval(dataTimer);
  });

  // ===== KPI 卡（数值映射 ov.kpi；跳转只指向已上线路由，缺页 toast） =====
  const kpiCards = computed(() => {
    const k = ov.value?.kpi;
    return [
      { ic: 'file', lab: '服务工单', v: k?.workOrders ?? '-', u: '单', c: '#00d4ff', path: '' },
      { ic: 'inbound', lab: '入库', v: k?.inbound ?? '-', u: '笔', c: '#36c6d6', path: '/xuehua/inbound' },
      { ic: 'outbound', lab: '出库', v: k?.outbound ?? '-', u: '笔', c: '#9b8aff', path: '/xuehua/outbound' },
      { ic: 'box', lab: '在库备件', v: k?.partKinds ?? '-', u: '种', c: '#2cc873', path: '' },
      { ic: 'alert', lab: '库龄超期', v: k?.overAged ?? '-', u: '批', c: '#ff5b6e', path: '' },
    ];
  });

  // ===== 今日待办卡 =====
  const todoCards = computed(() => {
    const t = ov.value?.todos;
    return [
      { ic: 'inbound', lab: '待登记入库', v: 0, u: '单', path: '/xuehua/inbound', c: '#36c6d6' },
      { ic: 'box', lab: '待上架', v: t?.toPutaway ?? '-', u: '单', path: '/xuehua/putaway', c: '#2cc873' },
      { ic: 'outbound', lab: '待拣配出库', v: t?.toPick ?? '-', u: '单', path: '/xuehua/outbound', c: '#9b8aff' },
      { ic: 'inventory', lab: '待盘点', v: t?.toInventory ?? '-', u: '区', path: '/xuehua/inventory', c: '#ffb020' },
      { ic: 'refresh', lab: '低位库存预警', v: t?.lowStock ?? '-', u: '位', path: '', c: '#ff6b9d' },
    ];
  });

  // ===== 通用环形图（逐行移植原型 donutSVG） =====
  function donutSVG(data: { n: string; v: number }[], total: number, colors: string[], cLabel: string): string {
    const R = 56, r = 34, cx = 70, cy = 70;
    let a = -90, p = '';
    data.forEach((d, i) => {
      const ang = (d.v / total) * 360, a0 = (a * Math.PI) / 180, a1 = ((a + ang) * Math.PI) / 180, lg = ang > 180 ? 1 : 0;
      const x0 = cx + R * Math.cos(a0), y0 = cy + R * Math.sin(a0), x1 = cx + R * Math.cos(a1), y1 = cy + R * Math.sin(a1);
      const xi1 = cx + r * Math.cos(a1), yi1 = cy + r * Math.sin(a1), xi0 = cx + r * Math.cos(a0), yi0 = cy + r * Math.sin(a0);
      p += `<path d="M ${x0.toFixed(1)} ${y0.toFixed(1)} A ${R} ${R} 0 ${lg} 1 ${x1.toFixed(1)} ${y1.toFixed(1)} L ${xi1.toFixed(1)} ${yi1.toFixed(1)} A ${r} ${r} 0 ${lg} 0 ${xi0.toFixed(1)} ${yi0.toFixed(1)} Z" fill="${colors[i % colors.length]}"/>`;
      a += ang;
    });
    return `<svg viewBox="0 0 140 140" style="width:130px;height:130px">${p}<text x="70" y="66" text-anchor="middle" fill="#eafcff" font-size="18" font-weight="800">${cLabel}</text><text x="70" y="82" text-anchor="middle" fill="#6f93b8" font-size="10">总计</text></svg>`;
  }

  function legendList(data: { n: string; v: number }[], colors: string[], unit: string): string {
    const tot = data.reduce((a, b) => a + b.v, 0) || 1;
    return `<div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap">${donutSVG(data, tot, colors, String(tot))}<div style="flex:1;min-width:120px">${data
      .map(
        (d, i) =>
          `<div style="display:flex;align-items:center;justify-content:space-between;padding:3px 0;font-size:12px"><span style="color:#cfe3f5"><i style="display:inline-block;width:9px;height:9px;border-radius:2px;background:${colors[i % colors.length]};margin-right:6px"></i>${d.n}</span><b style="color:#eafcff">${unit === '%' ? ((d.v / tot) * 100).toFixed(1) + '%' : d.v + ' 批'}</b></div>`,
      )
      .join('')}</div></div>`;
  }

  const catTotal = computed(() => ov.value?.catDonut?.reduce((a, b) => a + b.v, 0) ?? 0);
  const catDonutSvg = computed(() => {
    const data = ov.value?.catDonut ?? [];
    if (!data.length) return '<div class="empty">暂无备件分类数据</div>';
    const cols = ['#00d4ff', '#36c6d6', '#2cc873', '#9b8aff', '#ffb020', '#ff6b9d', '#6f93b8'];
    return legendList(data, cols, '%');
  });

  const AGING_LABEL: Record<string, string> = { lt30: '<30天', d30_60: '30-60天', d60_90: '60-90天', gt90: '>90天' };
  const agingSvg = computed(() => {
    const rows = ov.value?.aging ?? [];
    if (!rows.length) return '<div class="empty">暂无上架记录</div>';
    const data = rows.map((r) => ({ n: AGING_LABEL[r.bucket] ?? r.bucket, v: r.cnt }));
    const cols = ['#2cc873', '#16b8a8', '#f0a93c', '#e05a5a'];
    return legendList(data, cols, '批');
  });

  // ===== 出入库趋势（逐行移植原型 renderCostTrend 的面积+折线 SVG） =====
  const trendSvg = computed(() => {
    const rows = ov.value?.trend ?? [];
    if (!rows.length) return '<div class="empty">暂无趋势数据</div>';
    const A = rows.map((r) => r.cin);
    const B = rows.map((r) => r.cout);
    const ms = rows.map((r) => parseInt(r.ym.slice(5, 7), 10) + '月');
    const W = 560, H = 180, padL = 42, padR = 16, padT = 14, padB = 34;
    const plotW = W - padL - padR, plotH = H - padT - padB;
    const maxA = Math.ceil(Math.max(...A, 1) / 10) * 10;
    const X = (i: number) => padL + (i * plotW) / (A.length - 1);
    const Y = (v: number) => padT + (1 - v / maxA) * plotH;
    let g = '';
    for (let i = 0; i <= 4; i++) {
      const v = (maxA * i) / 4, y = Y(v);
      g += `<line x1="${padL}" y1="${y.toFixed(1)}" x2="${W - padR}" y2="${y.toFixed(1)}" stroke="rgba(120,160,200,.12)" stroke-dasharray="3 4"/><text x="${padL - 7}" y="${(y + 3).toFixed(1)}" fill="#6f93b8" font-size="9.5" text-anchor="end">${v}</text>`;
    }
    const area = `M ${X(0)} ${Y(A[0])} ` + A.map((v, i) => `L ${X(i)} ${Y(v).toFixed(1)}`).join('') + ` L ${X(A.length - 1)} ${padT + plotH} L ${X(0)} ${padT + plotH} Z`;
    const lineA = A.map((v, i) => `${i ? 'L' : 'M'} ${X(i)} ${Y(v).toFixed(1)}`).join('');
    const lineB = B.map((v, i) => `${i ? 'L' : 'M'} ${X(i)} ${Y(v).toFixed(1)}`).join('');
    const dotsB = B.map((v, i) => `<circle cx="${X(i).toFixed(1)}" cy="${Y(v).toFixed(1)}" r="2.6" fill="#ffb020"/>`).join('');
    const xlab = ms.map((m, i) => (i % 2 ? `<text x="${X(i).toFixed(1)}" y="${H - 12}" fill="#6f93b8" font-size="9.5" text-anchor="middle">${m}</text>` : '')).join('');
    return `<svg viewBox="0 0 ${W} ${H}" style="width:100%;height:auto;display:block">
      <defs><linearGradient id="cArea" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#2cc873" stop-opacity=".42"/><stop offset="1" stop-color="#2cc873" stop-opacity="0"/></linearGradient></defs>
      ${g}<path d="${area}" fill="url(#cArea)"/><path d="${lineA}" fill="none" stroke="#2cc873" stroke-width="2.2"/>
      <path d="${lineB}" fill="none" stroke="#ffb020" stroke-width="2" stroke-dasharray="5 3"/>${dotsB}${xlab}</svg>
      <div style="display:flex;gap:18px;font-size:11.5px;margin-top:6px"><span style="color:#2ee08a">● 入库(单)</span><span style="color:#ffb020">● 出库(单)</span></div>`;
  });

  // ===== 分区占用（移植 .tz-* 分段条；20 段、90% 红线、三态判定同原型 stat()） =====
  const SEGS = 20;
  const ZONE_COLORS = ['#00e0ff', '#ffb020', '#00ffa3', '#9b8aff', '#ff6b9d', '#36c6d6'];
  const zoneRows = computed(() => {
    const rows = ov.value?.zones ?? [];
    return rows.map((r, i) => {
      const pct = Number(r.pct ?? 0);
      const stat = pct >= 90 ? { c: 'err', t: '偏高告警' } : pct >= 60 ? { c: 'ok', t: '运行正常' } : { c: 'warn', t: '偏低闲置' };
      return { name: r.zone + '区', total: r.total, pct, fill: Math.round((pct / 100) * SEGS), color: ZONE_COLORS[i % ZONE_COLORS.length], stat };
    });
  });

  // ===== 实时出入库 / 预警（配色映射移植原型 cols 表） =====
  const feed = computed(() => ov.value?.liveFeed ?? []);
  const alarms = computed(() => ov.value?.alarms ?? { errCount: 0, warnCount: 0, items: [] });
  function typeColor(type: string) {
    return type === '入库' ? '#2cc873' : '#00d4ff';
  }
  function statusColor(status: string) {
    const cols: Record<string, string> = { 已上架: '#2cc873', 已出库: '#2cc873', 已登记: '#6f93b8', 待上架: '#ffb020', 拣配中: '#00d4ff', 待拣配: '#ffb020' };
    return cols[status] ?? '#6f93b8';
  }
</script>

<style lang="less">
  /* ============================================================
     工作台 · 深色驾驶舱 —— 样式 1:1 移植原型 common.css「Tech-Blue Dashboard」段
     （#dashboard 背景/网格、.tk-card 四角光标、.tk-kpi 渐变数字、.tz-* 分段条、
      .tk-tl 时间线、.tk-alarm 脉冲点），选择器统一挂在 .wcs-dash 下防泄漏
     ============================================================ */
  .wcs-dash {
    position: relative;
    /* 原型 #dashboard：径向深蓝渐变 + 负 margin 出血盖掉 jeecg 内容区留白 */
    background: radial-gradient(1100px 520px at 78% -10%, #1a4f88 0%, #0e2c52 40%, #081a31 74%, #050f1f 100%);
    margin: -16px;
    padding: 22px 26px 30px;
    color: #cfe3f5;
    min-height: calc(100vh - 110px);
    overflow: hidden;
    font-family: 'Microsoft YaHei', '微软雅黑', -apple-system, Arial, sans-serif;
  }
  .wcs-dash:before {
    content: '';
    position: absolute;
    inset: 0;
    background-image: linear-gradient(rgba(0, 170, 255, 0.045) 1px, transparent 1px),
      linear-gradient(90deg, rgba(0, 170, 255, 0.045) 1px, transparent 1px);
    background-size: 44px 44px;
    pointer-events: none;
    -webkit-mask: radial-gradient(ellipse at 50% 25%, #000 35%, transparent 85%);
    mask: radial-gradient(ellipse at 50% 25%, #000 35%, transparent 85%);
  }
  .wcs-dash > * { position: relative; z-index: 1; }

  /* ---- 头部 ---- */
  .wcs-dash .tk-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
  .wcs-dash .tk-title { display: flex; align-items: center; gap: 14px; }
  .wcs-dash .tk-logo {
    width: 48px; height: 48px; border-radius: 12px; background: none;
    display: flex; align-items: center; justify-content: center;
    box-shadow: 0 0 24px rgba(0, 180, 255, 0.45);
  }
  .wcs-dash .tk-title h2 { font-size: 23px; color: #fff; letter-spacing: 1.5px; font-weight: 700; margin: 0; }
  .wcs-dash .tk-meta { display: flex; align-items: center; gap: 12px; font-size: 12.5px; flex-wrap: wrap; }
  .wcs-dash .tk-time { color: #9fc4e8; font-variant-numeric: tabular-nums; }
  .wcs-dash .tk-seg { display: inline-flex; background: rgba(0, 200, 255, 0.08); border: 1px solid rgba(0, 200, 255, 0.22); border-radius: 8px; padding: 2px; }
  .wcs-dash .tk-seg .seg { padding: 5px 15px; font-size: 12px; color: #9fc1e2; cursor: pointer; border-radius: 6px; user-select: none; transition: 0.15s; }
  .wcs-dash .tk-seg .seg.on { background: rgba(0, 200, 255, 0.28); color: #eafcff; font-weight: 700; }
  .wcs-dash .tk-seg .seg:hover:not(.on) { color: #cfe3f5; background: rgba(0, 200, 255, 0.1); }
  .wcs-dash .tk-acts {
    padding: 7px 14px; border-radius: 7px; font-size: 12.5px; border: 1px solid rgba(0, 212, 255, 0.32);
    background: rgba(0, 150, 255, 0.1); color: #bfe6ff; cursor: pointer;
  }
  .wcs-dash .tk-acts:hover { background: rgba(0, 150, 255, 0.22); }
  .wcs-dash .tk-acts.pri { background: linear-gradient(135deg, #00b4ff, #0066ff); color: #fff; border-color: transparent; box-shadow: 0 0 18px rgba(0, 140, 255, 0.45); }

  /* ---- 卡片基底（四角光标） ---- */
  .wcs-dash .tk-card {
    position: relative;
    background: linear-gradient(180deg, rgba(32, 84, 146, 0.3), rgba(8, 28, 54, 0.46));
    border: 1px solid rgba(0, 200, 255, 0.14);
    border-radius: 13px;
    box-shadow: 0 6px 26px rgba(2, 10, 24, 0.42), inset 0 1px 0 rgba(120, 210, 255, 0.14);
  }
  .wcs-dash .tk-card::before, .wcs-dash .tk-card::after { content: ''; position: absolute; width: 13px; height: 13px; pointer-events: none; }
  .wcs-dash .tk-card::before { left: 7px; top: 7px; border-left: 2px solid #00e0ff; border-top: 2px solid #00e0ff; opacity: 0.55; }
  .wcs-dash .tk-card::after { right: 7px; bottom: 7px; border-right: 2px solid #00e0ff; border-bottom: 2px solid #00e0ff; opacity: 0.55; }

  /* ---- KPI ---- */
  .wcs-dash .tk-kpis { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; margin-bottom: 16px; }
  .wcs-dash .tk-kpi { padding: 17px 19px; cursor: pointer; transition: transform 0.15s, border-color 0.15s, box-shadow 0.15s; }
  .wcs-dash .tk-kpi:hover { border-color: rgba(0, 224, 255, 0.5); box-shadow: 0 8px 26px rgba(2, 10, 24, 0.5), inset 0 1px 0 rgba(120, 210, 255, 0.2); transform: translateY(-2px); }
  .wcs-dash .tk-kpi .ktop { display: flex; align-items: center; justify-content: space-between; }
  .wcs-dash .tk-kpi .lab { font-size: 12.5px; color: #9fc1e2; }
  .wcs-dash .tk-kpi .kico {
    width: 32px; height: 32px; border-radius: 9px;
    display: flex; align-items: center; justify-content: center;
    border: 1px solid rgba(0, 200, 255, 0.18);
  }
  .wcs-dash .tk-kpi .nrow { margin: 8px 0 2px; }
  .wcs-dash .tk-kpi .num {
    font-size: 34px; font-weight: 800; line-height: 1;
    background-clip: text; -webkit-background-clip: text; -webkit-text-fill-color: transparent;
    filter: drop-shadow(0 0 12px rgba(0, 200, 255, 0.45));
  }
  .wcs-dash .tk-kpi .unit { font-size: 13px; color: #7fa8d6; font-weight: 600; margin-left: 5px; }

  /* ---- 今日待办（dashboard.html 内联样式原样移植） ---- */
  .wcs-dash .tk-todos { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin: 0 0 16px; }
  .wcs-dash .tk-todo {
    background: linear-gradient(160deg, rgba(0, 80, 140, 0.16), rgba(8, 28, 54, 0.42));
    border: 1px solid rgba(120, 180, 230, 0.16); border-radius: 12px;
    padding: 12px 14px; cursor: pointer; transition: 0.15s;
    display: flex; flex-direction: column; gap: 4px;
  }
  .wcs-dash .tk-todo:hover { border-color: rgba(0, 224, 255, 0.45); background: linear-gradient(160deg, rgba(0, 100, 160, 0.24), rgba(8, 32, 60, 0.5)); }
  .wcs-dash .tk-todo.zero { opacity: 0.5; cursor: default; }
  .wcs-dash .tk-todo .td-ic { display: flex; align-items: center; }
  .wcs-dash .tk-todo .td-n { font-size: 26px; font-weight: 800; line-height: 1; }
  .wcs-dash .tk-todo .td-n small { font-size: 12px; font-weight: 500; color: #7fa8d6; margin-left: 3px; }
  .wcs-dash .tk-todo .td-l { font-size: 12px; color: #9fb8d4; }

  /* ---- 图表行 ---- */
  .wcs-dash .tk-row { display: grid; gap: 14px; margin-bottom: 16px; }
  .wcs-dash .tk-row.c3 { grid-template-columns: repeat(3, 1fr); }
  .wcs-dash .tk-ctitle { display: flex; align-items: center; justify-content: space-between; padding: 14px 18px; border-bottom: 1px solid rgba(0, 200, 255, 0.1); }
  .wcs-dash .tk-ctitle .t { font-size: 14.5px; font-weight: 600; color: #e6f3ff; display: flex; align-items: center; gap: 9px; }
  .wcs-dash .tk-ctitle .t::before {
    content: ''; width: 3px; height: 15px;
    background: linear-gradient(180deg, #00e0ff, #0066ff); border-radius: 2px;
    box-shadow: 0 0 6px rgba(0, 212, 255, 0.6);
  }
  .wcs-dash .tk-cbody { padding: 16px 18px; }
  .wcs-dash .tk-tab { font-size: 11px; color: #7fa8d6; border: 1px solid rgba(0, 200, 255, 0.2); padding: 3px 9px; border-radius: 6px; }

  /* ---- 分区占用（.tz-*） ---- */
  .wcs-dash .tz-row { display: flex; align-items: center; gap: 14px; padding: 12px 0; border-bottom: 1px dashed rgba(120, 180, 230, 0.1); }
  .wcs-dash .tz-row:last-child { border: none; }
  .wcs-dash .tz-info { width: 70px; flex-shrink: 0; }
  .wcs-dash .tz-name { display: block; font-size: 14px; font-weight: 700; color: #fff; letter-spacing: 0.5px; }
  .wcs-dash .tz-sub { font-size: 10.5px; color: #6f93b8; }
  .wcs-dash .tz-bar { flex: 1; position: relative; height: 24px; display: flex; align-items: center; }
  .wcs-dash .tz-bar::before, .wcs-dash .tz-bar::after { content: ''; position: absolute; width: 7px; height: 7px; border: 1px solid rgba(0, 224, 255, 0.45); }
  .wcs-dash .tz-bar::before { left: -3px; top: -3px; border-right: 0; border-bottom: 0; }
  .wcs-dash .tz-bar::after { right: -3px; bottom: -3px; border-left: 0; border-top: 0; }
  .wcs-dash .tz-segs { display: flex; gap: 3px; height: 20px; width: 100%; }
  .wcs-dash .tz-segs .seg { flex: 1; background: rgba(120, 180, 230, 0.07); border: 1px solid rgba(120, 180, 230, 0.16); border-radius: 2px; }
  .wcs-dash .tz-segs .seg.on {
    background: var(--c); border-color: transparent;
    box-shadow: 0 0 7px var(--c), inset 0 1px 0 rgba(255, 255, 255, 0.5);
    animation: segOn 0.5s ease both; animation-delay: calc(var(--i) * 35ms);
  }
  @keyframes segOn {
    0% { background: rgba(120, 180, 230, 0.07); box-shadow: none; transform: scaleY(0.4); }
    100% { background: var(--c); box-shadow: 0 0 7px var(--c), inset 0 1px 0 rgba(255, 255, 255, 0.5); transform: scaleY(1); }
  }
  .wcs-dash .tz-mark { position: absolute; top: -3px; bottom: -3px; width: 0; border-left: 1px dashed #ff6b7d; box-shadow: 0 0 5px rgba(255, 107, 125, 0.7); opacity: 0.85; }
  .wcs-dash .tz-val { width: 100px; flex-shrink: 0; text-align: right; }
  .wcs-dash .tz-val b {
    font-size: 23px; font-weight: 800; line-height: 1;
    background: linear-gradient(180deg, #fff, var(--c));
    -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
    filter: drop-shadow(0 0 6px var(--c));
  }
  .wcs-dash .tz-val small { font-size: 11px; color: #7fa8d6; -webkit-text-fill-color: #7fa8d6; }
  .wcs-dash .tz-st { display: block; font-size: 10px; margin-top: 2px; font-weight: 600; }
  .wcs-dash .tz-st.ok { color: #00ffa3; }
  .wcs-dash .tz-st.warn { color: #ffb020; }
  .wcs-dash .tz-st.err { color: #ff6b7d; }

  /* ---- 实时出入库时间线（.tk-tl） ---- */
  .wcs-dash .tk-tl .tev { display: flex; gap: 10px; padding: 10px 0; border-bottom: 1px dashed rgba(120, 180, 230, 0.13); }
  .wcs-dash .tk-tl .tev:last-child { border: none; }
  .wcs-dash .tk-tl .tdot { width: 8px; height: 8px; border-radius: 50%; margin-top: 6px; flex-shrink: 0; }
  .wcs-dash .tk-tl .tmt { font-size: 11px; color: #6f93b8; }
  .wcs-dash .tk-tl .tec { font-size: 12.5px; color: #cfe3f5; margin-top: 1px; }

  /* ---- 监控报警（.tk-alarm + 脉冲） ---- */
  .wcs-dash .tk-alarm { display: flex; gap: 10px; padding: 11px 0; border-bottom: 1px dashed rgba(120, 180, 230, 0.13); }
  .wcs-dash .tk-alarm:last-child { border: none; }
  .wcs-dash .tk-alarm .alv { width: 8px; height: 8px; border-radius: 50%; margin-top: 6px; flex-shrink: 0; }
  .wcs-dash .tk-alarm .alv.err { background: #ff5b6e; box-shadow: 0 0 9px #ff5b6e; animation: tkpulse 1.2s infinite; }
  .wcs-dash .tk-alarm .alv.warn { background: #ffb020; box-shadow: 0 0 9px #ffb020; }
  .wcs-dash .tk-alarm .alt { font-size: 13px; color: #e6f3ff; font-weight: 500; }
  .wcs-dash .tk-alarm .alm { font-size: 11px; color: #7fa8d6; margin-top: 2px; }
  @keyframes tkpulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.35; }
  }

  .wcs-dash .empty { text-align: center; color: #6f93b8; font-size: 12px; padding: 26px 0; }

  @media (max-width: 1280px) {
    .wcs-dash .tk-kpis { grid-template-columns: repeat(3, 1fr); }
  }
  @media (max-width: 980px) {
    .wcs-dash .tk-kpis, .wcs-dash .tk-todos { grid-template-columns: 1fr 1fr; }
    .wcs-dash .tk-row.c3 { grid-template-columns: 1fr; }
  }
</style>
