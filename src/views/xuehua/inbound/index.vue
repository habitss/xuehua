<template>
  <div>
    <FlowStrip :steps="flowSteps" :activeKey="activeStatus" @select="handleFlowSelect" />
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd">新增入库登记</a-button>
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
        <TableAction :actions="getActions(record)" />
      </template>
    </BasicTable>
    <InboundModal @register="registerModal" @success="handleReload" />
  </div>
</template>
<script lang="ts" name="xuehua-inbound" setup>
  import { ref, onMounted } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { Icon } from '/@/components/Icon';
  import { getInboundList, deleteInbound, batchDeleteInbound, getInboundStat, getExportUrl } from './inbound.api';
  import { columns, searchFormSchema } from './inbound.data';
  import InboundModal from './InboundModal.vue';
  import FlowStrip from '../components/FlowStrip.vue';
  import { useListPage } from '/@/hooks/system/useListPage';

  const [registerModal, { openModal }] = useModal();

  // 流程概览：点击卡片按状态筛选，再次点击取消
  const activeStatus = ref<number | null>(null);
  const statMap = ref<Record<string, number>>({});
  const flowSteps = ref([
    { key: '0', title: '待登记', count: 0 },
    { key: '1', title: '已登记', count: 0 },
  ]);

  // 列表页面公共参数、方法
  const { onExportXls, tableContext } = useListPage({
    designScope: 'xuehua-inbound',
    tableProps: {
      title: '入库列表',
      api: getInboundList,
      columns: columns,
      formConfig: {
        schemas: searchFormSchema,
      },
      actionColumn: {
        width: 140,
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
      name: '入库列表',
      url: getExportUrl,
    },
  });

  const [registerTable, { reload }, { rowSelection, selectedRowKeys }] = tableContext;

  onMounted(loadStat);

  async function loadStat() {
    statMap.value = (await getInboundStat()) || {};
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
        label: '编辑',
        onClick: handleEdit.bind(null, record),
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

  /**
   * 新增事件
   */
  function handleAdd() {
    openModal(true, {
      isUpdate: false,
    });
  }

  /**
   * 编辑事件
   */
  function handleEdit(record) {
    openModal(true, {
      record,
      isUpdate: true,
    });
  }

  /**
   * 删除事件
   */
  async function handleDelete(record) {
    await deleteInbound({ id: record.id }, handleReload);
  }

  /**
   * 批量删除事件
   */
  async function batchHandleDelete() {
    await batchDeleteInbound({ ids: selectedRowKeys.value }, () => {
      selectedRowKeys.value = [];
      handleReload();
    });
  }
</script>
