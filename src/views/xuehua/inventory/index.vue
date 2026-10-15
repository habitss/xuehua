<template>
  <div>
    <FlowStrip :steps="flowSteps" :activeKey="activeStatus" @select="handleFlowSelect" />
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd">新建盘点任务</a-button>
        <a-button type="primary" preIcon="ant-design:export-outlined" @click="onExportXls"> 导出</a-button>
        <a-dropdown v-if="selectedRowKeys.length > 0">
          <template #overlay>
            <a-menu>
              <a-menu-item key="1" @click="batchHandleDelete">
                <Icon icon="ant-design:delete-outlined"></Icon>
                删除
              </a-menu-item>
            </a-menu>
          </template>
          <a-button
            >批量操作
            <Icon icon="ant-design:down-outlined"></Icon>
          </a-button>
        </a-dropdown>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" :dropDownActions="getDropDownActions(record)" />
      </template>
    </BasicTable>
    <InventoryModal @register="registerModal" @success="handleReload" />
    <InventoryDetailModal @register="registerDetail" />
    <InventoryDisposeModal @register="registerDispose" @success="handleReload" />
    <InventoryBinsModal @register="registerBins" @success="handleReload" />
  </div>
</template>
<script lang="ts" name="xuehua-inventory" setup>
  import { ref, onMounted } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { Icon } from '/@/components/Icon';
  import {
    getInventoryList,
    deleteInventory,
    batchDeleteInventory,
    getInventoryStat,
    getExportUrl,
    startInventory,
    advanceInventory,
    freezeInventory,
    stopInventory,
  } from './inventory.api';
  import { columns, searchFormSchema } from './inventory.data';
  import InventoryModal from './InventoryModal.vue';
  import InventoryDetailModal from './InventoryDetailModal.vue';
  import InventoryDisposeModal from './InventoryDisposeModal.vue';
  import InventoryBinsModal from './InventoryBinsModal.vue';
  import FlowStrip from '../components/FlowStrip.vue';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';

  const { createMessage } = useMessage();
  const [registerModal, { openModal }] = useModal();
  const [registerDetail, { openModal: openDetail }] = useModal();
  const [registerDispose, { openModal: openDispose }] = useModal();
  const [registerBins, { openModal: openBins }] = useModal();

  // 流程概览：点击卡片按状态筛选，再次点击取消
  const activeStatus = ref<number | null>(null);
  const statMap = ref<Record<string, number>>({});
  const flowSteps = ref([
    { key: '0', title: '待盘点', count: 0 },
    { key: '1', title: '盘点中', count: 0 },
    { key: '2', title: '有差异', count: 0 },
    { key: '3', title: '已完成', count: 0 },
  ]);

  // 列表页面公共参数、方法
  const { onExportXls, tableContext } = useListPage({
    designScope: 'xuehua-inventory',
    tableProps: {
      title: '盘点任务列表',
      api: getInventoryList,
      columns: columns,
      formConfig: {
        schemas: searchFormSchema,
      },
      actionColumn: {
        width: 200,
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
      name: '盘点任务列表',
      url: getExportUrl,
    },
  });

  const [registerTable, { reload }, { rowSelection, selectedRowKeys }] = tableContext;

  onMounted(loadStat);

  async function loadStat() {
    statMap.value = (await getInventoryStat()) || {};
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

  /**
   * 操作列定义
   * @param record
   */
  function getActions(record) {
    return [
      {
        label: '详情',
        onClick: () => openDetail(true, { record }),
      },
      {
        label: '开始',
        ifShow: record.status === 0,
        popConfirm: {
          title: '开始该盘点任务？',
          confirm: () => doAction(() => startInventory(record.id), '盘点已开始'),
        },
      },
      {
        label: '库位明细',
        ifShow: record.status === 1 || record.status === 2,
        onClick: () => openBins(true, { record }),
      },
      {
        label: '推进',
        ifShow: record.status === 1 || record.status === 2,
        popConfirm: {
          title: '推进到下一盘点阶段？',
          confirm: () => doAction(() => advanceInventory(record.id), '盘点已推进到下一阶段'),
        },
      },
    ];
  }

  /**
   * 下拉操作列：冻结 / 差异处置 / 终止
   */
  function getDropDownActions(record) {
    return [
      {
        label: '冻结库存',
        ifShow: (record.status === 1 || record.status === 2) && record.frozenFlag !== 1,
        popConfirm: {
          title: '冻结后入库登记将被拦截，确认冻结？',
          confirm: () => doAction(() => freezeInventory(record.id), '库存已冻结'),
        },
      },
      {
        label: '差异处置',
        ifShow: record.status === 1 || record.status === 2,
        onClick: () => openDispose(true, { record }),
      },
      {
        label: '终止',
        ifShow: record.status === 1 || record.status === 2,
        popConfirm: {
          title: '确认终止该盘点任务？',
          confirm: () => doAction(() => stopInventory(record.id), '盘点已终止'),
        },
      },
      {
        label: '删除',
        popConfirm: {
          title: '是否确认删除',
          confirm: handleDelete.bind(null, record),
        },
      },
    ];
  }

  async function doAction(fn, successMsg) {
    await fn();
    createMessage.success(successMsg);
    handleReload();
  }

  /**
   * 新建盘点任务
   */
  function handleAdd() {
    openModal(true, {
      isUpdate: false,
    });
  }

  /**
   * 删除事件
   */
  async function handleDelete(record) {
    await deleteInventory({ id: record.id }, handleReload);
  }

  /**
   * 批量删除事件
   */
  async function batchHandleDelete() {
    await batchDeleteInventory({ ids: selectedRowKeys.value }, () => {
      selectedRowKeys.value = [];
      handleReload();
    });
  }
</script>
