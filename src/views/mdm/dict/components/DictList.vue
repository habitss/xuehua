<template>
  <!--引用表格-->
  <BasicTable @register="registerTable" :rowSelection="rowSelection">
    <!--插槽:table标题-->
    <template #tableTitle>
      <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleCreate"> 新增</a-button>
      <a-button type="primary" preIcon="ant-design:export-outlined" @click="onExportXls"> 导出</a-button>
      <j-upload-button type="primary" preIcon="ant-design:import-outlined" @click="onImportXls">导入</j-upload-button>

      <a-dropdown v-if="selectedRowKeys.length > 0">
        <template #overlay>
          <a-menu>
            <a-menu-item key="1" @click="batchHandleDelete">
              <Icon icon="ant-design:delete-outlined"></Icon>
              删除
            </a-menu-item>
          </a-menu>
        </template>
        <a-button>
          批量操作
          <Icon icon="ant-design:down-outlined"></Icon>
        </a-button>
      </a-dropdown>
    </template>
    <!--操作栏-->
    <template #action="{ record }">
      <TableAction :actions="getTableAction(record)" />
    </template>
  </BasicTable>
  <!--字典组弹窗-->
  <DictModal @register="registerModal" @success="handleSuccess" />
  <!--字典配置项抽屉-->
  <DictItemList @register="registerDrawer" />
</template>

<script lang="ts" setup name="mdm-dict-list">
  import { ref, reactive } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useModal } from '/@/components/Modal';
  import DictItemList from './DictItemList.vue';
  import DictModal from './DictModal.vue';
  import { columns, searchFormSchema } from '../dict.data';
  import { list, deleteDict, batchDeleteDict, getExportUrl, getImportUrl } from '../dict.api';
  import { useListPage } from '/@/hooks/system/useListPage';

  const queryParam = reactive<any>({});
  // 分类id
  const categorizeId = ref();
  // 字典组弹窗
  const [registerModal, { openModal }] = useModal();
  // 字典配置项抽屉
  const [registerDrawer, { openDrawer }] = useDrawer();

  // 列表页面公共参数、方法
  const { tableContext, onExportXls, onImportXls } = useListPage({
    designScope: 'dict-template',
    tableProps: {
      title: '数据字典',
      api: list,
      beforeFetch: (params) => {
        params.dictGroupName = params.dictGroupName ? '*' + params.dictGroupName + '*' : '';
        return Object.assign(params, queryParam);
      },
      columns: columns,
      immediate: false,
      formConfig: {
        schemas: searchFormSchema,
      },
      actionColumn: {
        width: 240,
      },
    },
    exportConfig: {
      name: '数据字典列表',
      url: getExportUrl,
    },
    importConfig: {
      url: getImportUrl,
    },
  });

  // 注册table数据
  const [registerTable, { reload, updateTableDataRecord }, { rowSelection, selectedRowKeys }] = tableContext;

  /**
   * 新增事件
   */
  function handleCreate() {
    openModal(true, {
      record: { categorizeId: categorizeId.value },
      isUpdate: false,
    });
  }
  /**
   * 编辑事件
   */
  async function handleEdit(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
    });
  }
  /**
   * 删除事件
   */
  async function handleDelete(record) {
    await deleteDict({ id: record.id }, reload);
  }
  /**
   * 批量删除事件
   */
  async function batchHandleDelete() {
    await batchDeleteDict({ ids: selectedRowKeys.value }, () => {
      reload();
      selectedRowKeys.value = [];
    });
  }
  /**
   * 成功回调
   */
  function handleSuccess({ isUpdate, values }) {
    if (isUpdate) {
      updateTableDataRecord(values.id, values);
    } else {
      reload();
    }
  }
  /**
   * 字典配置（打开配置项抽屉）
   */
  function handleItem(record) {
    openDrawer(true, {
      id: record.id,
      record: record,
    });
  }
  /**
   * 操作栏
   */
  function getTableAction(record) {
    return [
      {
        label: '编辑',
        onClick: handleEdit.bind(null, record),
      },
      {
        label: '字典配置',
        onClick: handleItem.bind(null, record),
      },
      {
        label: '删除',
        popConfirm: {
          title: '确定删除吗?',
          confirm: handleDelete.bind(null, record),
        },
      },
    ];
  }

  /**
   * 左侧树选中大类后刷新列表
   */
  function reloadList(cId) {
    if (!cId) {
      return;
    }
    queryParam.categorizeId = cId;
    categorizeId.value = cId;
    reload();
  }

  defineExpose({
    reloadList,
  });
</script>

<style scoped></style>
