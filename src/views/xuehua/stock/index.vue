<template>
  <div>
    <!-- KPI 统计：真实计数（总库位/在库/空位/利用率/低库存预警） -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :flex="'1'" v-for="k in kpis" :key="k.name">
        <a-card size="small" :style="{ borderTop: '3px solid ' + k.color }">
          <a-statistic :title="k.name" :value="k.value" :suffix="k.unit" :value-style="{ color: k.color }" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 库位平面图卡片 -->
    <a-card title="仓库库位平面图" :body-style="{ padding: '16px' }">
      <template #extra>
        <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap">
          <a-input-search v-model:value="query" placeholder="搜索库位号或备件编码，即时定位高亮" style="width: 300px" allow-clear />
          <a-select v-model:value="zoneFilter" style="width: 150px" :options="zoneFilterOptions" />
        </div>
      </template>

      <!-- 图例：六状态 + 低库存预警 -->
      <div class="map-legend">
        <span><i class="dot-i" style="background: #e6ecf2; border: 1px solid #c5d0db"></i>空位</span>
        <span><i class="dot-i" style="background: #2e6da4"></i>在库</span>
        <span><i class="dot-i" style="background: #f5a623"></i>拣配中(PTL)</span>
        <span><i class="dot-i low-dot"></i>低库存报警（低于安全库存）</span>
        <span><i class="dot-i" style="background: #8e44ad"></i>超大件区</span>
        <span><i class="dot-i" style="background: #00e6ff"></i>搜索命中</span>
      </div>

      <!-- 网格地图：按货架分组（坐标以 layer/col_no 列为准，空缺位灰色占位） -->
      <div class="whmap">
        <div v-for="z in shownRacks" :key="z.rack" class="zg-zone">
          <div class="zg-head">
            <b>{{ z.rack }} 货架 · {{ z.zoneName }}</b>
            <span>利用率 {{ z.util }}% · {{ z.occupied }}/{{ z.total }}</span>
          </div>
          <div class="zg-grid" :style="{ gridTemplateColumns: `56px repeat(${z.cols}, 1fr)` }">
            <div class="zg-corner"></div>
            <div v-for="c in z.cols" :key="'c' + c" class="zg-col">{{ c }}</div>
            <template v-for="(row, ri) in z.rows" :key="'r' + ri">
              <div class="zg-layer">第{{ ri + 1 }}层</div>
              <div
                v-for="cell in row"
                :key="cell.key"
                class="lc"
                :class="[cell.cls, { found: hit(cell), low: cell.bin && cell.bin.lowStock }]"
                :style="{ opacity: dim(cell) }"
                :title="cellTitle(cell.bin)"
                @click="cell.bin && openDetail(cell.bin)"
              >
                <template v-if="cell.bin">
                  <span class="lc-code">{{ shortCode(cell.bin.binCode) }}</span>
                  <b v-if="isOccupied(cell.bin)" class="lc-qty">{{ cell.bin.qty ?? 0 }}</b>
                  <span class="lc-st">{{ cell.bin.lowStock ? '⚠ 低库存' : statusLabel(cell.bin.status, cell.bin.zoneCode) }}</span>
                </template>
              </div>
            </template>
          </div>
        </div>
      </div>
    </a-card>

    <!-- 库位详情弹窗 -->
    <BasicModal
      v-bind="$attrs"
      @register="registerDetail"
      :title="'库位详情 — ' + (current?.binCode || '')"
      okText="查看库龄"
      @ok="goAging"
      cancelText="关闭"
      :width="620"
    >
      <div class="loc-detail" v-if="current">
        <div class="ld-note" :class="{ 'ld-warn': current.lowStock }">
          <template v-if="current.lowStock">
            ⚠ 低库存预警：现存 <b>{{ current.qty }}</b> 已低于安全库存 <b>{{ current.safeStock }}</b>，建议补货。
          </template>
          <template v-else>
            库位 <b>{{ current.binCode }}</b> · PTL 指示灯{{ current.status === 2 ? '已点亮' : '待命' }}。
          </template>
        </div>
        <div class="ld-grid">
          <div class="ld-item"><label>备件编码</label><div>{{ current.partCode || '—' }}</div></div>
          <div class="ld-item"><label>备件名称</label><div>{{ current.partName || current.partCode || '—' }}</div></div>
          <div class="ld-item"><label>数量</label><div>{{ current.qty ?? 0 }}</div></div>
          <div class="ld-item"><label>安全库存</label><div>{{ current.partCode ? (current.safeStock ?? '—') : '—' }}</div></div>
          <div class="ld-item"><label>所属货架</label><div>{{ current.rackCode || '—' }} · 第{{ current.layer || 1 }}层第{{ current.colNo || 1 }}位</div></div>
          <div class="ld-item">
            <label>预警状态</label>
            <div>
              <span v-if="current.lowStock" class="tag t-err">低库存 · 建议补货</span>
              <span v-else class="tag t-info">{{ statusLabel(current.status, current.zoneCode) }}</span>
            </div>
          </div>
          <div class="ld-item"><label>库位状态</label><div><span class="tag" :class="statusTagCls(current)">{{ statusLabel(current.status, current.zoneCode) }}</span></div></div>
          <div class="ld-item"><label>PTL 标签地址</label><div>{{ current.tagAdd ?? '未绑定' }}</div></div>
        </div>
        <div class="ld-foot">
          <a-button style="margin-right: 8px" @click="goVideo">查看库位视频</a-button>
          <a-button @click="enter3D(current)">进入 3D</a-button>
        </div>
      </div>
    </BasicModal>

    <!-- 3D 货架查看器（全屏覆盖层） -->
    <Rack3D v-if="show3D" :bins="all" :initial-rack="target3D?.rackCode" :target-bin="target3D" @close="show3D = false" />
  </div>
</template>

<script lang="ts" name="xuehua-stock" setup>
  import { ref, computed, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { BasicModal, useModal } from '/@/components/Modal';
  import { initDictOptions } from '/@/utils/dict';
  import { getBinList, type BinRow } from './stock.api';
  import Rack3D from './Rack3D.vue';

  const router = useRouter();
  const all = ref<BinRow[]>([]);
  const query = ref('');
  const zoneFilter = ref('');
  const current = ref<BinRow | null>(null);
  const show3D = ref(false);
  const target3D = ref<BinRow | null>(null);
  // 库位状态标签走 jeecg 数据字典（xuehua_bin_status），字典未配置时回退本地枚举
  const statusDict = ref<Record<string, string>>({});
  const [registerDetail, { openModal: openDetailModal }] = useModal();

  onMounted(async () => {
    await load();
    try {
      const items: any[] = (await initDictOptions('xuehua_bin_status')) || [];
      const map: Record<string, string> = {};
      for (const it of items) map[String(it.value)] = it.text || it.label || '';
      if (Object.keys(map).length) statusDict.value = map;
    } catch (e) {
      // 字典接口异常不阻断地图
    }
  });
  async function load() {
    const res = await getBinList();
    all.value = (res?.records || res || []) as BinRow[];
  }

  interface Cell {
    key: string;
    bin: BinRow | null;
    cls: string;
  }

  /** 区筛选下拉数据源 */
  const zoneFilterOptions = computed(() => {
    const map = new Map<string, string>();
    for (const b of all.value) {
      const code = b.zoneCode || '?';
      if (!map.has(code)) map.set(code, b.zoneName || code + '区');
    }
    return [{ label: '全部区域', value: '' }, ...Array.from(map.entries()).map(([value, label]) => ({ value, label }))];
  });

  /** 按货架分组（说明书口径：按区域、货架展示库位网格；同区多货架 (layer,col) 会冲突，必须按架拆卡） */
  interface RackGroup {
    rack: string;
    zoneCode: string;
    zoneName: string;
    bins: BinRow[];
  }
  const rackGroups = computed<RackGroup[]>(() => {
    const map = new Map<string, RackGroup>();
    for (const b of all.value) {
      const rack = b.rackCode || '?';
      if (!map.has(rack)) {
        map.set(rack, { rack, zoneCode: b.zoneCode || '', zoneName: b.zoneName || (b.zoneCode || '?') + '区', bins: [] });
      }
      map.get(rack)!.bins.push(b);
    }
    return Array.from(map.values()).sort((a, b) => a.rack.localeCompare(b.rack));
  });

  const shownRacks = computed(() => {
    return rackGroups.value
      .filter((g) => !zoneFilter.value || g.zoneCode === zoneFilter.value)
      .map((g) => {
        const maxLayer = Math.max(1, ...g.bins.map((b) => b.layer || 1));
        const maxCol = Math.max(1, ...g.bins.map((b) => b.colNo || 1));
        const rows: Cell[][] = [];
        for (let l = 1; l <= maxLayer; l++) {
          const row: Cell[] = [];
          for (let c = 1; c <= maxCol; c++) {
            const bin = g.bins.find((b) => (b.layer || 1) === l && (b.colNo || 1) === c) || null;
            row.push({ key: `${g.rack}-${l}-${c}`, bin, cls: bin ? stCls(bin) : 'gap' });
          }
          rows.push(row);
        }
        const occupied = g.bins.filter((b) => isOccupied(b)).length;
        const total = g.bins.length;
        return { rack: g.rack, zoneName: g.zoneName, rows, cols: maxCol, occupied, total, util: total ? Math.round((occupied / total) * 100) : 0 };
      });
  });

  /** KPI：真实计数（含低库存预警数，口径与格子报警一致） */
  const kpis = computed(() => {
    const total = all.value.length;
    const inuse = all.value.filter((b) => isOccupied(b)).length;
    const low = all.value.filter((b) => b.lowStock).length;
    return [
      { name: '总库位', value: total, unit: '个', color: '#1e6091' },
      { name: '在库', value: inuse, unit: '个', color: '#27ae60' },
      { name: '空位', value: total - inuse, unit: '个', color: '#f39c12' },
      { name: '利用率', value: total ? Math.round((inuse / total) * 100) : 0, unit: '%', color: '#2980b9' },
      { name: '低库存预警', value: low, unit: '位', color: low ? '#e74c3c' : '#7f8c9b' },
    ];
  });

  /** 状态：0空位/1在库/2拣配中/3报警/4超大件存放；D 区空位显示超大件区空位 */
  function isOccupied(b: BinRow): boolean {
    return b.status !== 0;
  }
  function stCls(b: BinRow): string {
    if (b.status === 0) return (b.zoneCode || '').toUpperCase() === 'D' ? 'bigempty' : 'empty';
    return ['', 'full', 'pick', 'alarm', 'big'][b.status] || 'empty';
  }
  function statusLabel(s: number, zoneCode?: string): string {
    if (statusDict.value[String(s)]) return statusDict.value[String(s)];
    if (s === 0) return (zoneCode || '').toUpperCase() === 'D' ? '空位(超大件区)' : '空位';
    return ['', '在库', '拣配中', '报警', '超大件存放'][s] || '未知';
  }
  function statusTagCls(b: BinRow): string {
    return ['', 't-info', 't-warn', 't-err', 't-purple'][b.status] || 't-gray';
  }
  function shortCode(code: string): string {
    const parts = (code || '').split('-');
    return parts.length >= 3 ? parts.slice(1).join('-') : code;
  }
  /** 格子悬浮提示：低库存给预警详情，其余给状态 */
  function cellTitle(b: BinRow | null): string {
    if (!b) return '';
    return b.lowStock
      ? `低库存预警：${b.partCode || ''} 现存 ${b.qty ?? 0} ≤ 安全库存 ${b.safeStock ?? '—'}，建议补货`
      : `${b.binCode} · ${statusLabel(b.status, b.zoneCode)}`;
  }

  /** 搜索：命中青色脉冲、未命中降透明 */
  function match(b: BinRow | null): boolean {
    const q = query.value.trim().toLowerCase();
    if (!q || !b) return true;
    return (b.binCode || '').toLowerCase().includes(q) || (b.partCode || '').toLowerCase().includes(q);
  }
  function hit(cell: Cell): boolean {
    return !!query.value.trim() && !!cell.bin && match(cell.bin);
  }
  function dim(cell: Cell): string | undefined {
    return query.value.trim() && !match(cell.bin) ? '.12' : undefined;
  }

  function openDetail(b: BinRow) {
    current.value = b;
    openDetailModal(true, {});
  }
  function goVideo() {
    localStorage.setItem('_vq', current.value?.binCode || '');
    router.push('/xuehua/monitor/video');
  }
  function goAging() {
    router.push('/xuehua/dashboard');
  }
  function enter3D(b: BinRow) {
    target3D.value = b;
    show3D.value = true;
  }
</script>

<style lang="less">
  /* 详情弹窗被 BasicModal 传送到 body 外，必须非 scoped；.loc-detail 命名空间防泄漏 */
  .loc-detail {
    .ld-note {
      font-size: 13px;
      color: #2c3e50;
      padding: 10px 14px;
      background: #e3f0fa;
      border-left: 3px solid #1e6091;
      border-radius: 6px;
      margin-bottom: 14px;

      b { color: #15497a; }

      &.ld-warn {
        background: #fdecea;
        border-left-color: #e74c3c;
        b { color: #c0392b; }
      }
    }

    .ld-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 16px 14px;
      margin-bottom: 14px;

      .ld-item label {
        display: block;
        font-size: 12px;
        color: #6b7c8d;
        margin-bottom: 4px;
      }

      .ld-item div {
        font-size: 14px;
        font-weight: 600;
        color: #2c3e50;
      }
    }

    .ld-foot {
      display: flex;
      justify-content: flex-start;
      gap: 8px;
      margin-top: 6px;
    }

    .tag {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 10px;
      font-size: 11.5px;
      font-weight: 600;
    }
    .t-info { background: #eaf4fb; color: #1f6391; }
    .t-warn { background: #fef5e0; color: #b7791f; }
    .t-err { background: #fdecea; color: #c0392b; }
    .t-purple { background: #f3e5f5; color: #6a1b9a; }
  }
</style>

<style lang="less" scoped>
  /* ====== 库位网格可视化（地图本体） ====== */
  .map-legend {
    display: flex;
    gap: 14px;
    flex-wrap: wrap;
    font-size: 12px;
    color: #6b7c8d;
    margin-bottom: 12px;

    span {
      display: flex;
      align-items: center;
      gap: 5px;
    }
  }

  .dot-i {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    margin-right: 5px;
    vertical-align: middle;
  }

  .whmap {
    border-radius: 12px;
  }

  .zg-zone {
    margin-bottom: 18px;
  }

  .zg-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 14px;
    background: linear-gradient(135deg, #1d5fa8, #0a3d80);
    color: #fff;
    border-radius: 9px 9px 0 0;

    b {
      font-size: 14.5px;
      letter-spacing: 0.5px;
    }

    span {
      font-size: 12px;
      opacity: 0.85;
    }
  }

  .zg-grid {
    display: grid;
    gap: 5px;
    background: #eaeff4;
    padding: 10px;
    border: 1px solid #e3e8ee;
    border-top: none;
    border-radius: 0 0 9px 9px;
  }

  .zg-corner { min-height: 1px; }

  .zg-col {
    text-align: center;
    font-size: 11px;
    color: #9aa8b5;
    font-weight: 700;
    padding: 2px 0;
  }

  .zg-layer {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 11px;
    color: #56697a;
    font-weight: 700;
    background: #dbe2ea;
    border-radius: 6px;
  }

  .lc {
    position: relative;
    min-height: 56px;
    border-radius: 7px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #fff;
    font-size: 11px;
    transition: 0.15s;
    border: 2px solid transparent;
    text-align: center;
    line-height: 1.25;

    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 6px 14px rgba(0, 0, 0, 0.2);
      z-index: 2;
    }

    .lc-code {
      font-family: Consolas, monospace;
      font-size: 10px;
      opacity: 0.92;
      font-weight: 600;
    }

    .lc-qty {
      font-size: 16px;
      font-weight: 800;
    }

    .lc-st {
      font-size: 9px;
      opacity: 0.85;
    }

    &.full { background: linear-gradient(135deg, #3a8fd0, #1d5fa8); }
    &.empty { background: #e8edf2; color: #9aa8b5; border-color: #d4dde5; }
    &.pick { background: linear-gradient(135deg, #ffb84d, #f5a623); animation: papulse 1.1s infinite; }
    &.alarm { background: linear-gradient(135deg, #f56c5e, #e74c3c); }
    &.big { background: linear-gradient(135deg, #a06bd0, #8e44ad); }
    &.bigempty { background: #ece2f3; color: #b88bd0; border-color: #dec5ec; }
    &.gap {
      background: repeating-linear-gradient(45deg, #f0f3f6, #f0f3f6 6px, #e8edf2 6px, #e8edf2 12px);
      cursor: default;
    }

    &.found {
      border-color: #00e6ff !important;
      box-shadow: 0 0 0 2px rgba(0, 230, 255, 0.5), 0 0 14px rgba(0, 230, 255, 0.6);
      animation: lcfind 1s infinite;
      z-index: 3;
    }

    /* 低库存报警：红色描边脉冲 + 角标（不覆盖底色，与拣配中/超大件状态共存） */
    &.low {
      border-color: #e74c3c;
      animation: lowpulse 1.2s infinite;

      &::after {
        content: '⚠';
        position: absolute;
        top: 2px;
        right: 4px;
        font-size: 10px;
        color: #fff;
        text-shadow: 0 0 3px rgba(0, 0, 0, 0.6);
      }
    }
  }

  .low-dot {
    background: #fdf0ee;
    border: 2px solid #e74c3c;
    animation: lowpulse 1.2s infinite;
  }

  @keyframes lowpulse {
    0%,
    100% {
      box-shadow: 0 0 6px rgba(231, 76, 60, 0.45);
    }
    50% {
      box-shadow: 0 0 16px rgba(231, 76, 60, 0.9);
    }
  }

  @keyframes papulse {
    0%,
    100% {
      box-shadow: 0 0 10px rgba(245, 166, 35, 0.45);
    }
    50% {
      box-shadow: 0 0 22px rgba(245, 166, 35, 0.85);
    }
  }

  @keyframes lcfind {
    0%,
    100% {
      box-shadow: 0 0 0 2px rgba(0, 230, 255, 0.4), 0 0 8px rgba(0, 230, 255, 0.5);
    }
    50% {
      box-shadow: 0 0 0 3px rgba(0, 230, 255, 0.7), 0 0 18px rgba(0, 230, 255, 0.9);
    }
  }
</style>
