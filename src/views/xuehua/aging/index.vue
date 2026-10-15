<template>
  <div>
    <!-- KPI 统计卡（在库件数/超期件数/超期库位/最高库龄） -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6" v-for="k in kpiCards" :key="k.name">
        <a-card size="small" :style="{ borderTop: '3px solid ' + k.color }">
          <a-statistic :title="k.name" :value="k.value" :suffix="k.unit" :value-style="{ color: k.color }" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 图表行：五档分布（柱状）+ 分类分析（双色柱状） -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="10">
        <a-card title="库龄分布（件）" size="small">
          <div ref="distChartRef" style="height: 250px"></div>
        </a-card>
      </a-col>
      <a-col :span="14">
        <a-card title="分类分析（在库 / 超期件数）" size="small">
          <div ref="catChartRef" style="height: 250px"></div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 在库明细表（与人员注册同款：formConfig 查询表单 + BasicTable 分页） -->
    <BasicTable
      @register="registerTable"
      :custom-row="(r) => ({ style: { cursor: 'pointer' }, onClick: () => openFlow(r) })"
    >
      <template #tableTitle>
        <a-button preIcon="ant-design:export-outlined" @click="doExport"> 导出</a-button>
        <a-badge v-if="pendingCount" :count="pendingCount" title="待审批处置" style="margin-left: 10px" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'agingDays'">
          <div class="age-cell">
            <div class="agebar" title="库龄阈值 90 天">
              <i :style="{ width: barPct(record.agingDays) + '%', background: barColor(record.status) }"></i>
            </div>
            <b :style="{ color: daysColor(record.status), minWidth: '28px' }">{{ record.agingDays }}</b>
          </div>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'dispose'">
          <span class="ld-link" style="margin-right: 6px; color: #1e6091; cursor: pointer" @click.stop="openFlow(record)">详情</span>
          <template v-if="record.status === 2">
            <a-button v-if="!disposalOf(record.binCode)" type="primary" size="small" @click="openApply(record)">发起处置</a-button>
            <a-button v-else-if="disposalOf(record.binCode)!.status === 0" size="small" @click="openApprove(disposalOf(record.binCode)!)">处置中(审批)</a-button>
            <a-tag v-else-if="disposalOf(record.binCode)!.status === 1" color="success">已处置·{{ disposalOf(record.binCode)!.method }}</a-tag>
            <a-button v-else type="primary" size="small" ghost @click="openApply(record)">重新处置</a-button>
          </template>
          <span v-else style="color: #999">—</span>
        </template>
        <template v-else-if="column.dataIndex === 'binCode'">
          <span style="color: #b7791f; font-family: Consolas, monospace">{{ record.binCode }}</span>
        </template>
      </template>
    </BasicTable>

    <!-- 发起处置弹窗 -->
    <BasicModal v-bind="$attrs" @register="registerApply" title="发起处置申请" okText="提交申请" @ok="submitApply" :width="520">
      <a-form v-if="applyRow" layout="vertical">
        <a-alert type="warning" show-icon style="margin-bottom: 14px"
          :message="`${applyRow.partName || applyRow.partCode || applyRow.binCode} · 库龄 ${applyRow.agingDays} 天，已超 90 天阈值`" />
        <a-form-item label="处置方式" required>
          <a-select v-model:value="applyForm.method" :options="[{ value: '报废' }, { value: '调拨他用' }, { value: '退库处置' }]" placeholder="选择处置方式" />
        </a-form-item>
        <a-form-item label="处置数量" required>
          <a-input-number v-model:value="applyForm.qty" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="申请理由" required>
          <a-textarea v-model:value="applyForm.reason" :rows="3" placeholder="请填写超期原因与处置建议" />
        </a-form-item>
      </a-form>
    </BasicModal>

    <!-- 审批弹窗 -->
    <BasicModal v-bind="$attrs" @register="registerApprove" title="处置审批" :width="560" cancelText="关闭">
      <template #footer>
        <a-button @click="closeApproveModal">关闭</a-button>
        <a-button danger @click="doApprove(false)">驳回</a-button>
        <a-button type="primary" @click="doApprove(true)">审批通过</a-button>
      </template>
      <a-form v-if="approveRow" layout="vertical">
        <a-descriptions :column="2" size="small" bordered style="margin-bottom: 14px">
          <a-descriptions-item label="库位">{{ approveRow.binCode }}</a-descriptions-item>
          <a-descriptions-item label="备件">{{ approveRow.partCode || '—' }}</a-descriptions-item>
          <a-descriptions-item label="处置方式">{{ approveRow.method }}</a-descriptions-item>
          <a-descriptions-item label="数量">{{ approveRow.qty }}</a-descriptions-item>
          <a-descriptions-item label="申请人">{{ approveRow.applicant }}</a-descriptions-item>
          <a-descriptions-item label="申请时间">{{ fmtTime(approveRow.applyTime) }}</a-descriptions-item>
          <a-descriptions-item label="申请理由" :span="2">{{ approveRow.reason }}</a-descriptions-item>
        </a-descriptions>
        <a-form-item label="审批意见">
          <a-textarea v-model:value="approveNote" :rows="2" placeholder="选填" />
        </a-form-item>
      </a-form>
    </BasicModal>

    <!-- 处置流程弹窗（原型 openAgingDetail + flow-mini 同构） -->
    <BasicModal
      v-bind="$attrs"
      @register="registerFlow"
      :title="'处置流程 — ' + (flowData?.row?.binCode || '')"
      :width="560"
      cancelText="关闭"
    >
      <div class="flow-detail" v-if="flowData">
        <div class="fd-note" :class="flowData.row.status === 2 ? 'err' : flowData.row.status === 1 ? 'warn' : ''">
          <b>{{ flowData.row.partName || flowData.row.partCode }}</b> · 库位 {{ flowData.row.binCode }} · 库龄
          <b class="fd-days">{{ flowData.row.agingDays }}</b> 天
          <template v-if="flowData.row.status === 2">⚠ 已超 90 天，需处置。</template>
          <template v-else-if="flowData.row.status === 1">⚠ 库龄偏高，关注消耗。</template>
        </div>
        <div class="fd-grid">
          <div class="fd-item"><label>备件编号</label><div>{{ flowData.row.partCode || '—' }}</div></div>
          <div class="fd-item"><label>入库日期</label><div>{{ fmtTime(flowData.row.firstPutaway) }}</div></div>
          <div class="fd-item"><label>库龄状态</label><div><a-tag :color="statusColor(flowData.row.status)">{{ statusLabel(flowData.row.status) }}</a-tag></div></div>
          <div class="fd-item"><label>库位</label><div style="color: #b7791f; font-family: Consolas, monospace">{{ flowData.row.binCode }}</div></div>
        </div>
        <template v-if="flowData.row.status === 2">
          <div class="flow-mini">
            <template v-for="(s, i) in flowSteps" :key="i">
              <span v-if="i > 0" class="fm-arr">›</span>
              <span class="fm-step" :class="s">{{ ['发起申请', '库房主管审批', '处置执行', '回传 Emis'][i] }}</span>
            </template>
          </div>
          <div class="fd-note" style="margin-top: 10px">{{ flowNote }}</div>
        </template>
      </div>
      <template #footer>
        <a-button @click="closeFlowModal">关闭</a-button>
        <a-button v-if="flowData?.row.status === 2 && !flowData?.disposal" type="primary" @click="openApply(flowData.row)">发起处置</a-button>
        <a-button v-else-if="flowData?.disposal?.status === 0" type="primary" @click="openApprove(flowData.disposal)">进入审批</a-button>
      </template>
    </BasicModal>
  </div>
</template>

<script lang="ts" name="xuehua-aging" setup>
  import { ref, computed, onMounted, nextTick } from 'vue';
  import { BasicTable, useTable, type FormSchema } from '/@/components/Table';
  import { BasicModal, useModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import * as echarts from 'echarts';
  import dayjs from 'dayjs';
  import {
    getAgingList, getAgingStat, getDisposalList, applyDisposal, approveDisposal,
    type DisposalRow,
  } from './aging.api';

  const { createMessage } = useMessage();
  const statData = ref<any>({});
  const disposals = ref<DisposalRow[]>([]);
  const distChartRef = ref<HTMLDivElement | null>(null);
  const catChartRef = ref<HTMLDivElement | null>(null);

  /** 处置流程弹窗（原型 openAgingDetail/flow-mini 的 Vue 版） */
  const flowData = ref<{ row: any; disposal: DisposalRow | null } | null>(null);
  const flowSteps = computed(() => {
    const d = flowData.value?.disposal;
    if (!d) return ['cur', '', '', ''];
    if (d.status === 0) return ['done', 'cur', '', ''];
    if (d.status === 1) return ['done', 'done', 'done', 'done'];
    return ['done', '', '', ''];
  });
  const flowNote = computed(() => {
    const d = flowData.value?.disposal;
    if (!d) return '超期未处置，需发起处置申请';
    if (d.status === 0) return '当前状态：待库房主管审批';
    if (d.status === 1) return '当前状态：已通过，处置执行与 Emis 回传由线下承接';
    return '当前状态：已驳回，可重新发起处置申请';
  });

  function openFlow(row: any) {
    flowData.value = { row, disposal: disposalOf(row.binCode) };
    openFlowModal(true, {});
  }
  const [registerApply, { openModal: openApplyModal, closeModal: closeApplyModal, setModalProps: setApplyProps }] = useModal();
  const [registerFlow, { openModal: openFlowModal, closeModal: closeFlowModal }] = useModal();

  const [registerApprove, { openModal: openApproveModal, closeModal: closeApproveModal }] = useModal();
  const applyRow = ref<any>(null);
  const approveRow = ref<DisposalRow | null>(null);
  const approveNote = ref('');
  const applyForm = ref({ method: '报废', qty: 1, reason: '' });

  const columns = [
    { title: '备件编码', dataIndex: 'partCode', width: 110 },
    { title: '备件名称', dataIndex: 'partName', width: 150 },
    { title: '类别', dataIndex: 'category', width: 90 },
    { title: '库位', dataIndex: 'binCode', width: 100 },
    { title: '入库日期', dataIndex: 'firstPutaway', width: 110 },
    { title: '库龄(天)', dataIndex: 'agingDays', width: 170 },
    { title: '库龄状态', dataIndex: 'status', width: 90 },
    { title: '处置', dataIndex: 'dispose', width: 140 },
  ];

  /** 查询表单（与人员注册/仓库管理同款交互：查询/重置由 jeecg 表单承载，值自动并入列表查询） */
  const searchFormSchema: FormSchema[] = [
    { label: '关键字', field: 'keyword', component: 'Input', colProps: { span: 6 }, componentProps: { placeholder: '备件编码/名称/库位/类别' } },
    {
      label: '库龄状态',
      field: 'status',
      component: 'Select',
      colProps: { span: 6 },
      componentProps: {
        options: [
          { label: '正常', value: 0 },
          { label: '偏高', value: 1 },
          { label: '超期', value: 2 },
        ],
      },
    },
  ];

  /** jeecg BasicTable：api 模式（表单查询值自动并入请求参数，返回 {records,total}） */
  const [registerTable, { reload, getDataSource }] = useTable({
    api: getAgingList,
    columns,
    useSearchForm: true,
    pagination: {
      pageSize: 10,
      showSizeChanger: true,
      pageSizeOptions: ['10', '20', '50'],
      showQuickJumper: true,
      showTotal: (t: any) => '共 ' + t + ' 条记录',
    },
    showIndexColumn: false,
    showTableSetting: true,
    formConfig: { schemas: searchFormSchema, autoSubmitOnEnter: true },
  });

  const pendingCount = computed(() => disposals.value.filter((d) => d.status === 0).length);

  /** KPI 卡 */
  const kpiCards = computed(() => {
    const k = statData.value.kpi || {};
    return [
      { name: '在库件数', value: k.pieces ?? 0, unit: '件', color: '#1e6091' },
      { name: '超期件数', value: k.overPieces ?? 0, unit: '件', color: '#e74c3c' },
      { name: '超期库位', value: k.overBins ?? 0, unit: '个', color: '#f39c12' },
      { name: '最高库龄', value: k.maxDays ?? 0, unit: '天', color: '#8e44ad' },
    ];
  });

  onMounted(load);
  async function load() {
    const [st, ds] = await Promise.all([getAgingStat(), getDisposalList({})]);
    statData.value = st || {};
    disposals.value = (ds || []) as DisposalRow[];
    nextTick(() => renderCharts());
  }

  /** 库位最新有效处置（非驳回优先，取最新） */
  function disposalOf(binCode: string): DisposalRow | null {
    const list = disposals.value.filter((d) => d.binCode === binCode);
    if (!list.length) return null;
    const active = list.find((d) => d.status !== 2);
    return active || list[0];
  }

  /** 状态三档：0正常 1偏高 2超期（>90 超期 / 60-90 偏高） */
  function statusLabel(s: number) { return ['', '偏高', '超期'][s] || '正常'; }
  function statusColor(s: number) { return ['success', 'warning', 'error'][s] || 'default'; }
  function daysColor(s: number) { return ['#2c3e50', '#f39c12', '#e74c3c'][s] || '#2c3e50'; }
  function barColor(s: number) { return ['#1d9bd1', '#e8a317', '#e05a5a'][s] || '#1d9bd1'; }
  function barPct(d: number) { return Math.min(100, Math.round((d / 90) * 100)); }
  function fmtTime(t?: string) { return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—'; }

  /** 图表：五档分布柱状 + 分类分析双色柱状 */
  function renderCharts() {
    const dist = statData.value.distribution || [];
    const labels: Record<string, string> = { lt30: '<30天', d30_60: '30-60天', d60_90: '60-90天', d90_180: '90-180天', gt180: '>180天' };
    const colors: Record<string, string> = { lt30: '#2cc873', d30_60: '#16b8a8', d60_90: '#f0a93c', d90_180: '#e8743c', gt180: '#e05a5a' };
    if (distChartRef.value) {
      const chart = echarts.init(distChartRef.value);
      chart.setOption({
        grid: { left: 40, right: 16, top: 30, bottom: 30 },
        xAxis: { type: 'category', data: dist.map((d: any) => labels[d.bucket] || d.bucket) },
        yAxis: { type: 'value' },
        tooltip: { trigger: 'axis' },
        series: [{
          type: 'bar', barWidth: 34,
          data: dist.map((d: any) => ({ value: d.pieces, itemStyle: { color: colors[d.bucket] } })),
          label: { show: true, position: 'top' },
        }],
      });
    }
    const cats = statData.value.byCategory || [];
    if (catChartRef.value) {
      const chart = echarts.init(catChartRef.value);
      chart.setOption({
        grid: { left: 50, right: 16, top: 30, bottom: 30 },
        legend: { top: 0 },
        xAxis: { type: 'category', data: cats.map((c: any) => c.category) },
        yAxis: { type: 'value' },
        tooltip: { trigger: 'axis' },
        series: [
          { name: '在库件数', type: 'bar', barWidth: 18, itemStyle: { color: '#1d9bd1' }, data: cats.map((c: any) => c.pieces) },
          { name: '超期件数', type: 'bar', barWidth: 18, itemStyle: { color: '#e05a5a' }, data: cats.map((c: any) => c.overPieces) },
        ],
      });
    }
  }

  /** 处置申请/审批 */
  function openApply(row: any) {
    applyRow.value = row;
    applyForm.value = { method: '报废', qty: 1, reason: '' };
    openApplyModal(true, {});
  }
  async function submitApply() {
    if (!applyForm.value.method) return createMessage.warning('请选择处置方式');
    if (!applyForm.value.reason?.trim()) return createMessage.warning('请填写申请理由');
    try {
      setApplyProps({ confirmLoading: true });
      const res: any = await applyDisposal({
        binCode: applyRow.value?.binCode,
        partCode: applyRow.value?.partCode,
        partName: applyRow.value?.partName,
        agingDays: applyRow.value?.agingDays,
        ...applyForm.value,
      });
      createMessage.success(res?.message || '已提交');
      closeApplyModal();
      await Promise.all([load(), reload()]);
    } finally {
      setApplyProps({ confirmLoading: false });
    }
  }
  function openApprove(d: DisposalRow) {
    approveRow.value = d;
    approveNote.value = '';
    openApproveModal(true, {});
  }
  async function doApprove(pass: boolean) {
    if (!approveRow.value) return;
    const res: any = await approveDisposal({ id: approveRow.value.id, pass, note: approveNote.value || '' });
    if (res?.success === false) return createMessage.error(res.message);
    createMessage.success(pass ? '审批通过' : '已驳回');
    closeApproveModal();
    await Promise.all([load(), reload()]);
  }

  /** 导出当前表格数据 CSV */
  function doExport() {
    const head = '备件编码,备件名称,类别,库位,入库日期,库龄天数,库龄状态';
    const rows = (getDataSource() || []).map((r: any) =>
      [r.partCode || '', r.partName || '', r.category || '', r.binCode, r.firstPutaway ? dayjs(r.firstPutaway).format('YYYY-MM-DD') : '', r.agingDays, statusLabel(r.status)].join(','));
    const blob = new Blob(['\ufeff' + head + '\n' + rows.join('\n')], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `库龄分析_${dayjs().format('YYYYMMDD_HHmm')}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
    createMessage.success('已导出库龄分析 · CSV');
  }
</script>

<style lang="less" scoped>
  /* 行内库龄进度条（原型 .agebar 同构：90 天红线在右端） */
  .age-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .agebar {
      position: relative;
      width: 90px;
      height: 6px;
      border-radius: 3px;
      background: #eef3f8;
      overflow: hidden;

      &::after {
        content: '';
        position: absolute;
        right: 0;
        top: 0;
        bottom: 0;
        width: 2px;
        background: rgba(231, 76, 60, 0.45);
      }

      i {
        display: block;
        height: 100%;
        border-radius: 3px;
      }
    }
  }
</style>

<style lang="less">
  /* ====== 处置流程弹窗（传送至 body 外，必须非 scoped）；flow-detail 命名空间防泄漏 ====== */
  .flow-detail {
    .fd-note {
      font-size: 13px;
      color: #2c3e50;
      padding: 10px 14px;
      background: #eaf4fb;
      border-left: 3px solid #2980b9;
      border-radius: 6px;
      margin-bottom: 14px;

      &.warn { background: #fef5e0; border-left-color: #f39c12; }
      &.err { background: #fdecea; border-left-color: #e74c3c; }

      b { color: #15497a; }
      &.err b { color: #c0392b; }

      .fd-days { font-size: 18px; }
    }

    .fd-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 14px;
      margin-bottom: 6px;

      .fd-item label {
        display: block;
        font-size: 12px;
        color: #6b7c8d;
        margin-bottom: 3px;
      }

      .fd-item div {
        font-size: 13px;
        font-weight: 600;
        color: #2c3e50;
      }
    }

    .flow-mini {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
      margin: 14px 0 6px;
    }

    .fm-step {
      padding: 6px 16px;
      border-radius: 14px;
      font-size: 13px;
      font-weight: 600;
      background: #eef1f5;
      color: #6b7c8d;
      border: 1px solid #dfe5ec;
    }

    .fm-step.done {
      background: #e8f7ee;
      color: #1a7a3a;
      border-color: #b7e0c8;
    }

    .fm-step.cur {
      background: #e3f0fa;
      color: #15497a;
      border-color: #9cc3e8;
    }

    .fm-arr {
      color: #9aa8b5;
      font-weight: 700;
    }
  }
</style>
