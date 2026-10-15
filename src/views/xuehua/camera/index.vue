<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button preIcon="ant-design:plus-outlined" type="primary" @click="openModal(true, {})"> 新增点位</a-button>
        <a-button preIcon="ant-design:cloud-download-outlined" @click="discover" style="margin-left: 8px"> 从设备拉取通道</a-button>
        <a-button preIcon="ant-design:export-outlined" @click="doExport" style="margin-left: 8px"> 导出</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'cameraCode'">
          <span style="font-family: Consolas, monospace; font-weight: 700">{{ record.cameraCode }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <span class="cam-link" @click="openModal(true, { record, isUpdate: true })">编辑</span>
          <span class="cam-link" style="margin-left: 8px" @click="showPreview(record)">RTSP地址</span>
          <a-popconfirm title="确认删除该点位？" @confirm="doDelete(record)">
            <span class="cam-link" style="margin-left: 8px; color: #c0392b">删除</span>
          </a-popconfirm>
        </template>
      </template>
    </BasicTable>

    <CameraModal @register="registerModal" @success="reload" />

    <!-- 通道发现结果弹窗 -->
    <BasicModal v-bind="$attrs" @register="registerDiscover" title="设备通道列表（ISAPI）" :showOkBtn="false" cancelText="关闭" :width="560">
      <a-alert v-if="discoverMsg" :message="discoverMsg" type="info" show-icon style="margin-bottom: 12px" />
      <a-table :columns="channelCols" :data-source="channels" :pagination="false" size="small" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'op'">
            <a @click="fillFromChannel(record)">填入新增表单</a>
          </template>
        </template>
      </a-table>
    </BasicModal>

    <!-- RTSP 地址弹窗 -->
    <BasicModal v-bind="$attrs" @register="registerPreview" :title="'RTSP 地址 — ' + (previewRow?.cameraCode || '')" :showOkBtn="false" cancelText="关闭" :width="640">
      <a-alert :message="previewMsg || '现场可用 VLC 验证取流；浏览器内播放待流媒体代理转 HLS/HTTP-FLV（二期）'" type="info" show-icon style="margin-bottom: 12px" />
      <div class="rtsp-row" v-for="(v, k) in previewUrls" :key="k">
        <label>{{ k === 'live' ? '实时主码流' : k === 'liveSub' ? '实时子码流' : '录像回放' }}</label>
        <code>{{ v }}</code>
        <a @click="copy(v)">复制</a>
      </div>
    </BasicModal>
  </div>
</template>

<script lang="ts" name="xuehua-camera" setup>
  import { ref } from 'vue';
  import { BasicTable, useTable } from '/@/components/Table';
  import { BasicModal, useModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import CameraModal from './CameraModal.vue';
  import { getCameraList, deleteCamera, discoverChannels, previewUrl, downloadCameraXls } from './camera.api';
  import { cameraColumns, cameraSearchSchema } from './camera.data';

  const { createMessage } = useMessage();
  const [registerModal, { openModal: openPointModal }] = useModal();
  const [registerDiscover, { openModal: openDiscover }] = useModal();
  const [registerPreview, { openModal: openPreview }] = useModal();
  const channels = ref<any[]>([]);
  const discoverMsg = ref('');
  const previewRow = ref<any>(null);
  const previewUrls = ref<Record<string, string>>({});
  const previewMsg = ref('');

  const channelCols = [
    { title: '通道编号', dataIndex: 'id', key: 'id', width: 100 },
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '源IP', dataIndex: 'sourceIp', key: 'sourceIp', width: 130 },
    { title: '操作', key: 'op', width: 110 },
  ];

  const [registerTable, { reload }] = useTable({
    api: getCameraList,
    columns: cameraColumns,
    actionColumn: { width: 160, title: '操作', dataIndex: 'action' },
    useSearchForm: true,
    beforeFetch: (params: any) => {
      // keyword 是后端 QueryGenerator 不认识的模糊字段——转成 cameraCode 模糊
      if (params.keyword) {
        params.cameraCode = '*' + params.keyword + '*';
        delete params.keyword;
      }
      return params;
    },
    pagination: { pageSize: 10, showSizeChanger: true, showTotal: (t: any) => '共 ' + t + ' 条记录' },
    showIndexColumn: false,
    showTableSetting: true,
    formConfig: { schemas: cameraSearchSchema, autoSubmitOnEnter: true },
  });

  function openModal(isUpdate: boolean, data: any) {
    openPointModal(true, { isUpdate, ...data });
  }

  /** 从设备拉通道（ISAPI；未接入时后端降级提示） */
  async function discover() {
    try {
      const r: any = await discoverChannels();
      channels.value = r?.channels || [];
      discoverMsg.value = channels.value.length
        ? `已发现 ${channels.value.length} 个通道（设备返回原文见后端日志）`
        : '设备返回了空通道列表';
      openDiscover(true, {});
    } catch (e: any) {
      createMessage.warning(e?.message || '设备未接入');
    }
  }

  /** 通道一键填入新增表单 */
  function fillFromChannel(ch: any) {
    const num = parseInt(String(ch.id).replace(/\D/g, '')) || 1;
    openModal(false, { preset: { deviceType: 0, channelNo: num, indexCode: ch.id, cameraName: ch.name || '' } });
    createMessage.success('已带入通道信息，请补全点位编码与绑定位置');
  }

  /** RTSP 地址生成展示 */
  async function showPreview(row: any) {
    previewRow.value = row;
    try {
      const r: any = await previewUrl({ id: row.id });
      previewUrls.value = { live: r?.live || '', liveSub: r?.liveSub || '', ...(r?.playback ? { playback: r.playback } : {}) };
      previewMsg.value = r?.message || '';
      openPreview(true, {});
    } catch (e: any) {
      createMessage.error(e?.message || '生成失败');
    }
  }

  function copy(text: string) {
    navigator.clipboard?.writeText(text);
    createMessage.success('已复制');
  }

  async function doDelete(row: any) {
    await deleteCamera(row, () => {
      createMessage.success('已删除');
      reload();
    });
  }

  async function doExport() {
    await downloadCameraXls({}, `视频点位_${new Date().toISOString().slice(0, 10)}.xls`);
    createMessage.success('已导出视频点位');
  }

  // 预置通道带入：新增弹窗打开后写表单
  defineExpose({ openModal });
</script>

<style lang="less" scoped>
  .cam-link {
    color: #1e6091;
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }

  .rtsp-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;

    label {
      flex: 0 0 72px;
      font-size: 12px;
      color: #6b7c8d;
    }

    code {
      flex: 1;
      font-size: 11.5px;
      word-break: break-all;
      background: #f4f7fa;
      padding: 6px 8px;
      border-radius: 4px;
    }

    a {
      font-size: 12px;
    }
  }
</style>
