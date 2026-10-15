<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button preIcon="ant-design:export-outlined" @click="doExport"> 导出</a-button>
        <a-button preIcon="ant-design:import-outlined" @click="fileInput?.click()"> 导入</a-button>
        <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display: none" @change="doImport" />
        <span class="src-hint">人员档案由海康平台推送，WCS 仅记录与查询</span>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            { label: '详情', onClick: handleDetail.bind(null, record) },
            {
              label: record.status === 1 ? '停用' : '启用',
              popConfirm: {
                title: record.status === 1 ? '确认停用该人员的进仓作业许可？' : '确认启用该人员？',
                confirm: handleToggle.bind(null, record),
              },
            },
          ]"
        />
      </template>
    </BasicTable>
    <PersonDetailModal @register="registerDetail" />
  </div>
</template>
<script lang="ts" name="xuehua-monitor-person" setup>
  import { ref } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getPersonList, togglePerson, downloadXls, importXls } from '../monitor.api';
  import { personColumns, personSearchSchema } from '../monitor.data';
  import PersonDetailModal from './PersonDetailModal.vue';
  import { useListPage } from '/@/hooks/system/useListPage';

  const [registerDetail, { openModal: openDetail }] = useModal();
  const { createMessage } = useMessage();
  const fileInput = ref<HTMLInputElement | null>(null);

  const { tableContext } = useListPage({
    designScope: 'xuehua-monitor-person',
    tableProps: {
      title: '人员注册',
      api: getPersonList,
      columns: personColumns,
      formConfig: { schemas: personSearchSchema },
      actionColumn: { width: 140 },
      showIndexColumn: false,
    },
  });
  const [registerTable, { reload }] = tableContext;

  function handleDetail(record) {
    openDetail(true, { record });
  }
  async function handleToggle(record) {
    await togglePerson({ id: record.id, status: record.status === 1 ? 0 : 1 });
    reload();
  }
  async function doExport() {
    await downloadXls('/xuehua/person/exportXls', {}, '人员注册.xlsx');
    createMessage.success('导出完成');
  }
  async function doImport(e: any) {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const res: any = await importXls('/xuehua/person/importExcel', file);
      createMessage.success(res?.message || '导入完成');
      reload();
    } finally {
      e.target.value = '';
    }
  }
</script>
<style lang="less" scoped>
  .src-hint {
    margin-left: 10px;
    font-size: 12px;
    color: #999;
  }
</style>
