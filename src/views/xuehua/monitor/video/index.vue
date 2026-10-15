<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <span class="src-hint">事件录像检索 · 海康 7×24 自录 · “回放”调取海康取流地址</span>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[{ label: '回放', onClick: handlePlayback.bind(null, record) }]"
        />
      </template>
    </BasicTable>
    <AccessDetailModal @register="registerDetail" />
  </div>
</template>
<script lang="ts" name="xuehua-monitor-video" setup>
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { getAccessList } from '../monitor.api';
  import { accessColumns, accessSearchSchema } from '../monitor.data';
  import AccessDetailModal from '../access/AccessDetailModal.vue';
  import { useListPage } from '/@/hooks/system/useListPage';

  const [registerDetail, { openModal: openDetail }] = useModal();

  const { tableContext } = useListPage({
    designScope: 'xuehua-monitor-video',
    tableProps: {
      title: '视频搜索',
      api: getAccessList,
      columns: accessColumns,
      formConfig: { schemas: accessSearchSchema },
      actionColumn: { width: 90 },
      showIndexColumn: false,
    },
  });
  const [registerTable] = tableContext;

  function handlePlayback(record) {
    openDetail(true, { record });
  }
</script>
<style lang="less" scoped>
  .src-hint {
    font-size: 12px;
    color: #999;
  }
</style>
