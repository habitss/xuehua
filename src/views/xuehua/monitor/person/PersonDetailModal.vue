<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="人员档案（海康推送）" :showOkBtn="false" cancelText="关闭">
    <a-descriptions :column="2" size="small" bordered v-if="record">
      <a-descriptions-item label="姓名">{{ record.name }}</a-descriptions-item>
      <a-descriptions-item label="工号">{{ record.empNo }}</a-descriptions-item>
      <a-descriptions-item label="部门">{{ record.dept || '—' }}</a-descriptions-item>
      <a-descriptions-item label="识别方式">{{ record.authMethod || '—' }}</a-descriptions-item>
      <a-descriptions-item label="人脸库">
        <a-tag :color="faceColor">{{ faceLabel }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="卡号">{{ record.cardNo || '—' }}</a-descriptions-item>
      <a-descriptions-item label="授权区域">{{ record.zones || '—' }}</a-descriptions-item>
      <a-descriptions-item label="作业许可">
        <a-tag :color="record.status === 1 ? 'success' : 'default'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="海康门禁授权">
        <a-tag :color="authColor">{{ authLabel }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="海康人员ID">{{ record.hikPersonId || '—' }}</a-descriptions-item>
      <a-descriptions-item label="注册时间" :span="2">{{ record.createTime || '—' }}</a-descriptions-item>
    </a-descriptions>
    <div class="src-note">数据来源：海康平台推送（档案变更请到海康侧操作后重新推送）</div>
  </BasicModal>
</template>
<script lang="ts" name="xuehua-monitor-person-detail" setup>
  import { ref, computed } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';

  const record = ref<any>(null);

  const faceLabel = computed(() => ({ 1: '已采集', 0: '未采集', 2: '采集失败' } as any)[record.value?.faceStatus] ?? '未知');
  const faceColor = computed(() => ({ 1: 'success', 0: 'warning', 2: 'error' } as any)[record.value?.faceStatus] ?? 'default');
  const authLabel = computed(() => ({ 1: '已授权', 0: '待授权', 2: '授权失败' } as any)[record.value?.hikAuthStatus] ?? '待授权');
  const authColor = computed(() => ({ 1: 'success', 0: 'warning', 2: 'error' } as any)[record.value?.hikAuthStatus] ?? 'warning');

  const [registerModal] = useModalInner((data) => {
    record.value = data.record;
  });
</script>
<style lang="less" scoped>
  .src-note {
    margin-top: 10px;
    color: #999;
    font-size: 12px;
  }
</style>
