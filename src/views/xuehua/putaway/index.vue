<template>
  <div>
    <FlowStrip :steps="flowSteps" :activeKey="activeStatus" @select="handleFlowSelect" />
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:export-outlined" @click="onExportXls"> 导出</a-button>
        <a-alert type="info" show-icon message="上架任务由入库登记成功后自动生成；按上架指引完成 PTL 放置后点击确认" class="table-alert" />
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
    </BasicTable>
    <PutawayGuideModal @register="registerModal" @success="handleReload" />
  </div>
</template>
<script lang="ts" name="xuehua-putaway" setup>

  import { ref, onMounted } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { getPutawayList, getPutawayStat, confirmPutaway, getExportUrl } from './putaway.api';
  import { columns, searchFormSchema } from './putaway.data';
  import PutawayGuideModal from './PutawayGuideModal.vue';
  import FlowStrip from '../components/FlowStrip.vue';
  import { useListPage } from '/@/hooks/system/useListPage';

  const [registerModal, { openModal }] = useModal();

  // 流程概览：待上架/已上架 可点击筛选，再次点击取消
  const activeStatus = ref<number | null>(null);
  const statMap = ref<Record<string, number>>({});
  const flowSteps = ref([
    { key: '0', title: '待上架', count: 0 },
    { key: '1', title: '已上架', count: 0 },
  ]);

  const { onExportXls, tableContext } = useListPage({
    designScope: 'xuehua-putaway',
    tableProps: {
      title: '上架任务',
      api: getPutawayList,
      columns: columns,
      formConfig: {
        schemas: searchFormSchema,
      },
      actionColumn: {
        width: 180,
      },
      showIndexColumn: true,
      beforeFetch: (params) => {
        if (activeStatus.value !== null) {
          params.status = activeStatus.value;
        }
        return params;
      },
    },
    exportConfig: {
      name: '上架任务',
      url: getExportUrl,
    },
  });

  const [registerTable, { reload }] = tableContext;

  onMounted(loadStat);

  async function loadStat() {
    statMap.value = (await getPutawayStat()) || {};
    flowSteps.value.forEach((s) => (s.count = Number(statMap.value['s' + s.key] || 0)));
  }

  function handleFlowSelect(key: string) {
    activeStatus.value = activeStatus.value === Number(key) ? null : Number(key);
    reload();
  }

  function handleReload() {
    reload();
    loadStat();
  }

  function getActions(record) {
    return [
      {
        label: '上架指引',
        ifShow: record.status === 0,
        onClick: handleGuide.bind(null, record),
      },
      {
        label: '确认上架',
        ifShow: record.status === 0,
        popConfirm: {
          title: '确认该备件已放置到位？',
          confirm: handleConfirm.bind(null, record),
        },
      },
    ];
  }

  function handleGuide(record) {
    openModal(true, { record });
  }

  async function handleConfirm(record) {
    await confirmPutaway({ id: record.id });
    handleReload();
  }
</script>
<style lang="less" scoped>
  .table-alert {
    margin-bottom: 0;
  }
</style>
