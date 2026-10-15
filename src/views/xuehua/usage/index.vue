<template>
  <div>
    <!-- 出库使用明细（原型 usage.html 单表 · 状态可筛选 · 布局对齐入库管理） -->
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button preIcon="ant-design:bell-outlined" @click="remindAll"> 一键提醒确认</a-button>
        <a-button preIcon="ant-design:export-outlined" @click="doExport" style="margin-left: 8px"> 导出报表</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'partCode'">
          <div><b>{{ record.partCode }}</b></div>
          <div class="part-name">{{ record.partName || '—' }}</div>
        </template>
        <template v-else-if="column.dataIndex === 'category'">
          <a-tag v-if="record.category">{{ record.category }}</a-tag>
          <span v-else>—</span>
        </template>
        <template v-else-if="column.dataIndex === 'outTime'">
          {{ fmtDay(record.outTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'days'">
          <span :style="{ color: overOf(record) ? '#e74c3c' : '#2c3e50', fontWeight: overOf(record) ? 700 : 400 }">{{ record.days }} 天</span>
        </template>
        <template v-else-if="column.dataIndex === 'purpose'">
          <span style="color: #666">{{ record.status === 1 ? record.purpose || '—' : '—' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'statusText'">
          <a-tag :color="statusColor(record)">{{ record.statusText }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-button v-if="record.status === 0" type="primary" size="small" @click="openConfirm(record)">✓ 确认</a-button>
          <template v-else-if="record.status === 2">
            <a-button size="small" @click="remindOne(record)">🔔 提醒</a-button>
            <span class="ld-link" style="margin-left: 6px; color: #1e6091; cursor: pointer" @click="openDetail(record)">详情</span>
          </template>
          <span v-else class="ld-link" style="color: #1e6091; cursor: pointer" @click="openDetail(record)">详情</span>
        </template>
      </template>
    </BasicTable>

    <!-- 使用确认弹窗（实际用量 + 安装照片 + 说明；提交确认 / 登记未使用） -->
    <UseConfirmModal @register="registerConfirm" @success="onConfirmed" />

    <!-- 使用详情弹窗（已使用/未使用记录 + 照片回看） -->
    <BasicModal v-bind="$attrs" @register="registerDetail" title="使用详情" :showOkBtn="false" cancelText="关闭" :width="560">
      <a-descriptions v-if="detailRow" :column="2" size="small" bordered>
        <a-descriptions-item label="出库工单" :span="2">
          <span style="font-family: Consolas, monospace">{{ detailRow.woNo || '—' }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="备件" :span="2">{{ detailRow.partCode }} {{ detailRow.partName || '' }}</a-descriptions-item>
        <a-descriptions-item label="类别">{{ detailRow.category || '—' }}</a-descriptions-item>
        <a-descriptions-item label="领用产线">{{ detailRow.workshop || '—' }}</a-descriptions-item>
        <a-descriptions-item label="领用人">{{ detailRow.receiver || '—' }}</a-descriptions-item>
        <a-descriptions-item label="出库日期">{{ fmtDay(detailRow.outTime) }}（{{ detailRow.days }} 天）</a-descriptions-item>
        <a-descriptions-item label="领用数量">{{ detailRow.qty }}</a-descriptions-item>
        <a-descriptions-item label="实际使用">{{ detailRow.status === 1 ? detailRow.usedQty ?? '—' : '—' }}</a-descriptions-item>
        <a-descriptions-item label="使用状态"><a-tag :color="statusColor(detailRow)">{{ detailRow.statusText }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="用途" :span="2">{{ detailRow.purpose || '—' }}</a-descriptions-item>
      </a-descriptions>
      <template v-if="detailRow?.photos">
        <div class="photo-title">安装照片</div>
        <a-image-preview-group>
          <a-image v-for="(p, i) in photoList(detailRow.photos)" :key="i" :src="p" :width="120" style="margin-right: 8px; border-radius: 6px" />
        </a-image-preview-group>
      </template>
    </BasicModal>
  </div>
</template>

<script lang="ts" name="xuehua-usage" setup>
  import { ref } from 'vue';
  import { BasicTable, useTable, type FormSchema } from '/@/components/Table';
  import { BasicModal, useModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import dayjs from 'dayjs';
  import UseConfirmModal from './UseConfirmModal.vue';
  import { getUsageList, remindUsage, downloadUsageXls, type UsageRow } from './usage.api';

  const { createMessage } = useMessage();
  const detailRow = ref<UsageRow | null>(null);
  const [registerConfirm, { openModal: openConfirmModal }] = useModal();
  const [registerDetail, { openModal: openDetailModal }] = useModal();

  const columns = [
    { title: '备件', dataIndex: 'partCode', width: 200 },
    { title: '数量', dataIndex: 'qty', width: 70, align: 'center' as const },
    { title: '类别', dataIndex: 'category', width: 90 },
    { title: '领用产线', dataIndex: 'workshop', width: 110 },
    { title: '领用人', dataIndex: 'receiver', width: 90 },
    { title: '出库日期', dataIndex: 'outTime', width: 100 },
    { title: '出库天数', dataIndex: 'days', width: 90, align: 'center' as const },
    { title: '用途', dataIndex: 'purpose', width: 160 },
    { title: '使用状态', dataIndex: 'statusText', width: 110, align: 'center' as const },
    { title: '操作', dataIndex: 'action', width: 130, align: 'center' as const },
  ];

  const searchFormSchema: FormSchema[] = [
    { label: '关键字', field: 'keyword', component: 'Input', colProps: { span: 6 }, componentProps: { placeholder: '备件/类别/产线/领用人' } },
    {
      label: '使用状态',
      field: 'status',
      component: 'Select',
      colProps: { span: 6 },
      componentProps: {
        options: [
          { label: '待确认', value: '0' },
          { label: '已使用', value: '1' },
          { label: '未使用', value: '2' },
          { label: '超1周未使用', value: 'over' },
        ],
        allowClear: true,
      },
    },
    { label: '出库日期', field: 'outTime', component: 'RangePicker', colProps: { span: 8 } },
  ];

  const [registerTable, { reload }] = useTable({
    api: getUsageList,
    columns,
    useSearchForm: true,
    beforeFetch: (params: any) => {
      // RangePicker 自动拆 _begin/_end，转后端 from/to
      params.from = params.outTime_begin;
      params.to = params.outTime_end;
      delete params.outTime_begin;
      delete params.outTime_end;
      return params;
    },
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

  function openConfirm(row: UsageRow) {
    openConfirmModal(true, { record: row });
  }
  async function onConfirmed() {
    await reload();
  }
  function openDetail(row: UsageRow) {
    detailRow.value = row;
    openDetailModal(true, {});
  }

  /** 提醒（单条 / 一键全部未确认） */
  async function remindOne(row: UsageRow) {
    const res: any = await remindUsage([row.id]);
    createMessage.success(res || '已提醒');
    await reload();
  }
  async function remindAll() {
    const res: any = await remindUsage(null);
    createMessage.success(res || '已提醒');
    await reload();
  }

  /** 导出报表（真实 xls，导出当前筛选结果） */
  async function doExport() {
    await downloadUsageXls({}, `备件使用报表_${dayjs().format('YYYYMMDD_HHmm')}.xls`);
    createMessage.success('已导出备件使用报表 · Excel');
  }

  /** 超1周未使用（派生态）：未确认且超 7 天 */
  function overOf(r: UsageRow) {
    return r.status !== 1 && (r.days ?? 0) > 7;
  }
  function statusColor(r: UsageRow) {
    return r.status === 1 ? 'success' : overOf(r) ? 'error' : r.status === 2 ? 'processing' : 'warning';
  }
  function fmtDay(t?: string) {
    return t ? dayjs(t).format('YYYY-MM-DD') : '—';
  }
  function photoList(photos?: string) {
    return photos ? photos.split(',').filter(Boolean) : [];
  }
</script>

<style lang="less" scoped>
  .part-name {
    font-size: 12px;
    color: #6b7c8d;
  }
  .photo-title {
    margin: 12px 0 8px;
    font-size: 13px;
    font-weight: 600;
    color: #2c3e50;
  }
</style>
