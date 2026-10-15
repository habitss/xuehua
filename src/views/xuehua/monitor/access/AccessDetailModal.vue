<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="事件详情" okText="🎬 调取回放" @ok="handlePlayback" cancelText="关闭">
    <div class="acc-detail" v-if="record">
      <div class="acc-head" :class="isAlarm ? 'alarm' : ''">
        <div class="acc-type">{{ eventTypeLabel }}</div>
        <div class="acc-time">{{ record.eventTime || '' }}</div>
      </div>
      <a-descriptions :column="2" size="small" bordered>
        <a-descriptions-item label="人员">{{ record.
          personName || '—' }}</a-descriptions-item>
        <a-descriptions-item label="部门">{{ record.dept || '—' }}</a-descriptions-item>
        <a-descriptions-item label="区域">{{ record.zone || '—' }}</a-descriptions-item>
        <a-descriptions-item label="识别方式">{{ record.method || '—' }}</a-descriptions-item>
        <a-descriptions-item label="关联单据">{{ record.bizNo || '—' }}</a-descriptions-item>
        <a-descriptions-item label="摄像头点位">{{ record.cameraIndex || '—' }}</a-descriptions-item>
      </a-descriptions>
      <a-alert
        v-if="isAlarm"
        type="error"
        :message="'异常事件：' + eventTypeLabel"
        description="已记录告警时间线；处置与派单流程二期接入"
        show-icon
        style="margin-top: 12px"
      />
    </div>
  </BasicModal>
</template>
<script lang="ts" name="xuehua-monitor-access-detail" setup>
  import { ref, computed } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getPlayback } from '../monitor.api';

  const { createMessage } = useMessage();
  const record = ref<any>(null);

  const EVENT_NAMES: Record<number, string> = {
    1: '进仓',
    2: '出仓',
    3: '放置确认',
    4: '拣配确认',
    5: '非授权进入',
    6: '尾随进入',
  };
  const eventTypeLabel = computed(() => EVENT_NAMES[record.value?.eventType] || '未知');
  /** 报警事件（非授权/尾随）——模板高亮与告警条用 */
  const isAlarm = computed(() => record.value?.eventType === 5 || record.value?.eventType === 6);

  const [registerModal, { setModalProps }] = useModalInner((data) => {
    record.value = data.record;
    setModalProps({ confirmLoading: false });
  });

  /** 回放：调海康 playbackURLs（hik 未启用时后端返回提示文案） */
  async function handlePlayback() {
    const res = await getPlayback(record.value.id);
    createMessage.info(String(res));
  }
</script>
<style lang="less" scoped>
  .acc-detail {
    .acc-head {
      margin-bottom: 12px;
      padding: 14px;
      text-align: center;
      background: #f0f7ff;
      border-radius: 8px;

      &.alarm {
        background: #fff1f0;
      }

      .acc-type {
        font-size: 20px;
        font-weight: 700;
        color: #1e6091;
      }

      &.alarm .acc-type {
        color: #cf1322;
      }

      .acc-time {
        font-size: 12px;
        color: #999;
        margin-top: 2px;
      }
    }
  }
</style>
