<template>
  <div>
    <!-- 工单统计三卡（原型 flow-strip 同构，点击卡片筛选该类型，再点取消） -->
    <FlowStrip :steps="steps" :active-key="stripType" @select="onStripSelect" />
    <div class="wo-done small">已完成 {{ stat.done ?? 0 }}/{{ stat.total ?? 0 }}：入库 {{ stat.ib?.done ?? 0 }}/{{ stat.ib?.total ?? 0 }} · 出库 {{ stat.ob?.done ?? 0 }}/{{ stat.ob?.total ?? 0 }} · 盘点 {{ stat.pv?.done ?? 0 }}/{{ stat.pv?.total ?? 0 }}</div>

    <!-- 工单聚合列表（与入库管理同款：formConfig 查询表单 + BasicTable 分页） -->
    <BasicTable @register="registerTable" :custom-row="(r) => ({ style: { cursor: 'pointer' }, onClick: () => openDetail(r) })">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'no'">
          <span style="font-family: Consolas, monospace; font-weight: 700">{{ record.no }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'woType'">
          <a-tag :color="typeColor(record.woType)">{{ record.woType }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'statusText'">
          <a-tag :color="tagColor(record.statusTag)">{{ record.statusText }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'time'">
          <span style="color: #666; white-space: nowrap">{{ fmtTime(record.time) }}</span>
        </template>
      </template>
    </BasicTable>

    <!-- 工单详情弹窗（原型 openWoDetail 同构） -->
    <BasicModal v-bind="$attrs" @register="registerDetail" title="工单详情" :showOkBtn="false" cancelText="关闭" :width="520">
      <a-descriptions v-if="detailRow" :column="2" size="small" bordered>
        <a-descriptions-item label="工单号" :span="2">
          <span style="font-family: Consolas, monospace; font-weight: 700">{{ detailRow.no }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="类型">
          <a-tag :color="typeColor(detailRow.woType)">{{ detailRow.woType }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="tagColor(detailRow.statusTag)">{{ detailRow.statusText }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="内容" :span="2">{{ detailRow.content || '—' }}</a-descriptions-item>
        <a-descriptions-item label="数量">{{ detailRow.qty }}</a-descriptions-item>
        <a-descriptions-item label="执行人">{{ detailRow.person }}</a-descriptions-item>
        <a-descriptions-item label="时间" :span="2">{{ fmtTime(detailRow.time) }}</a-descriptions-item>
      </a-descriptions>
    </BasicModal>
  </div>
</template>

<script lang="ts" name="xuehua-workorder" setup>
  import { ref, computed, onMounted } from 'vue';
  import { BasicTable, useTable, type FormSchema } from '/@/components/Table';
  import { BasicModal, useModal } from '/@/components/Modal';
  import dayjs from 'dayjs';
  import FlowStrip from '../components/FlowStrip.vue';
  import { getWorkorderList, getWorkorderStat } from './workorder.api';

  const stat = ref<any>({});
  const stripType = ref<string>('');
  const detailRow = ref<any>(null);
  const [registerDetail, { openModal: openDetailModal }] = useModal();

  /** 三卡：入库/出库/盘点（数量来自 /stat，点击筛选类型） */
  const steps = computed(() => [
    { key: '入库', title: '入库工单', count: stat.value.ib?.total ?? 0 },
    { key: '出库', title: '出库工单', count: stat.value.ob?.total ?? 0 },
    { key: '盘点', title: '盘点任务', count: stat.value.pv?.total ?? 0 },
  ]);

  function onStripSelect(key: any) {
    stripType.value = stripType.value === key ? '' : String(key);
    reload();
  }

  const columns = [
    { title: '工单号', dataIndex: 'no', width: 150 },
    { title: '类型', dataIndex: 'woType', width: 80 },
    { title: '内容', dataIndex: 'content', width: 220 },
    { title: '数量', dataIndex: 'qty', width: 70, align: 'center' as const },
    { title: '执行人', dataIndex: 'person', width: 90 },
    { title: '时间', dataIndex: 'time', width: 150 },
    { title: '状态', dataIndex: 'statusText', width: 90 },
  ];

  const searchFormSchema: FormSchema[] = [
    { label: '关键字', field: 'keyword', component: 'Input', colProps: { span: 6 }, componentProps: { placeholder: '工单号/内容/执行人/状态' } },
    {
      label: '类型',
      field: 'woType',
      component: 'Select',
      colProps: { span: 6 },
      componentProps: {
        options: [
          { label: '入库', value: '入库' },
          { label: '出库', value: '出库' },
          { label: '盘点', value: '盘点' },
        ],
        allowClear: true,
      },
    },
    { label: '时间', field: 'createTime', component: 'RangePicker', colProps: { span: 8 } },
  ];

  const [registerTable, { reload }] = useTable({
    api: getWorkorderList,
    columns,
    useSearchForm: true,
    beforeFetch: (params: any) => {
      // RangePicker 自动拆 _begin/_end，转后端 from/to；三卡筛选优先于表单类型
      params.from = params.createTime_begin;
      params.to = params.createTime_end;
      delete params.createTime_begin;
      delete params.createTime_end;
      if (stripType.value) params.woType = stripType.value;
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

  function openDetail(row: any) {
    detailRow.value = row;
    openDetailModal(true, {});
  }

  onMounted(async () => {
    stat.value = (await getWorkorderStat()) || {};
  });

  function typeColor(t: string) {
    return t === '入库' ? 'success' : t === '出库' ? 'processing' : 'purple';
  }
  function tagColor(tag: string) {
    return tag === 'warn' ? 'warning' : tag === 'err' ? 'error' : 'success';
  }
  function fmtTime(t?: string) {
    return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '—';
  }
</script>

<style lang="less" scoped>
  .wo-done {
    margin: -6px 0 10px;
    color: #6b7c8d;
    text-align: right;
  }
</style>
