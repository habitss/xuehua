<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button preIcon="ant-design:export-outlined" @click="doExport"> 导出</a-button>
        <a-button preIcon="ant-design:import-outlined" @click="fileInput?.click()"> 导入</a-button>
        <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display: none" @change="doImport" />
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[{ label: '详情 / 回放', onClick: handleDetail.bind(null, record) }]"
        />
      </template>
    </BasicTable>
    <AccessDetailModal @register="registerDetail" />
  </div>
</template>
<script lang="ts" name="xuehua-monitor-access" setup>
  import { ref } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getAccessList, downloadXls, importXls } from '../monitor.api';
  import { accessColumns, accessSearchSchema } from '../monitor.data';
  import AccessDetailModal from './AccessDetailModal.vue';
  import { useListPage } from '/@/hooks/system/useListPage';

  const [registerDetail, { openModal: openDetail }] = useModal();
  const { createMessage } = useMessage();
  const fileInput = ref<HTMLInputElement | null>(null);

  const { tableContext } = useListPage({
    designScope: 'xuehua-monitor-access',
    tableProps: {
      title: '进出识别记录',
      api: getAccessList,
      columns: accessColumns,
      formConfig: { schemas: accessSearchSchema },
      actionColumn: { width: 120 },
      showIndexColumn: false,
    },
  });
  const [registerTable] = tableContext;

  function handleDetail(record) {
    openDetail(true, { record });
  }
  async function doExport() {
    await downloadXls('/xuehua/access/exportXls', {}, '进出记录.xlsx');
    createMessage.success('导出完成');
  }
  async function doImport(e: any) {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const res: any = await importXls('/xuehua/access/importExcel', file);
      createMessage.success(res?.message || '导入完成');
    } finally {
      e.target.value = '';
    }
  }
</script>
