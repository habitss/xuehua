<template>
  <div>
    <FlowStrip :steps="flowSteps" :activeKey="activeStatus" @select="handleFlowSelect" />
    <BasicTable @register="registerTable" :rowSelection="rowSelection" :expandedRowRender="expandedRowRender">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd">新建领用申请</a-button>
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
    <OutboundModal @register="registerModal" @success="handleReload" />
    <PickGuideModal @register="registerPick" @success="handleReload" />
  </div>
</template>
<script lang="ts" name="xuehua-outbound" setup>
  import { ref, onMounted } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { Icon } from '/@/components/Icon';
  import { getOutboundList, deleteOutbound, batchDeleteOutbound, getOutboundStat, getExportUrl } from './outbound.api';
  import { columns, searchFormSchema } from './outbound.data';
  import OutboundModal from './OutboundModal.vue';
  import PickGuideModal from './PickGuideModal.vue';
  import OutboundLines from './OutboundLines.vue';
  import { h } from 'vue';
  import FlowStrip from '../components/FlowStrip.vue';
  import { useListPage } from '/@/hooks/system/useListPage';

  /**
   * 行展开：显示自动分配的拣配明细行（库位/数量/顺序/状态）——
   * 2026-09-13 起领料单不填库位，由 WCS 库龄优先 FIFO 自动拆行
   */
  function expandedRowRender({ record }) {
    return h(OutboundLines, { woNo: record.woNo });
  }

  const [registerModal, { openModal }] = useModal();
  const [registerPick, { openModal: openPick }] = useModal();

  // 流程概览：点击卡片按状态筛选，再次点击取消
  const activeStatus = ref<number | null>(null);
  const statMap = ref<Record<string, number>>({});
  const flowSteps = ref([
    { key: '0', title: '待拣配', count: 0 },
    { key: '1', title: '拣配中', count: 0 },
    { key: '2', title: '已出库', count: 0 },
  ]);

  // 列表页面公共参数、方法
  const { onExportXls, tableContext } = useListPage({
    designScope: 'xuehua-outbound',
    tableProps: {
      title: '出库工单列表',
      api: getOutboundList,
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
      name: '出库工单列表',
      url: getExportUrl,
    },
  });

  const [registerTable, { reload }, { rowSelection, selectedRowKeys }] = tableContext;

  onMounted(loadStat);

  async function loadStat() {
    statMap.value = (await getOutboundStat()) || {};
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
        label: '拣配指引',
        ifShow: record.status !== 2,
        onClick: handlePick.bind(null, record),
      },
      {
        label: '编辑',
        ifShow: record.status !== 2,
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
   * 新建领用申请
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
   * 拣配指引（PTL 取货 + 扫码确认出库）
   */
  function handlePick(record) {
    openPick(true, { record });
  }

  /**
   * 删除事件
   */
  async function handleDelete(record) {
    await deleteOutbound({ id: record.id }, handleReload);
  }

  /**
   * 批量删除事件
   */
  async function batchHandleDelete() {
    await batchDeleteOutbound({ ids: selectedRowKeys.value }, () => {
      selectedRowKeys.value = [];
      handleReload();
    });
  }
</script>
