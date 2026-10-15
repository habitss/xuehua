<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="上架指引" :width="560" :okText="'已放置 · 确认上架'" @ok="handleConfirm">
    <div class="putaway-guide" v-if="record">
      <div class="guide-bin">
        <div class="guide-bin-label">目标库位</div>
        <div class="guide-bin-code">{{ record.binCode || '待分配' }}</div>
        <div class="guide-ptl">
          <span class="ptl-dot"></span>
           PTL 电子标签已点亮
        </div>
      </div>
      <a-descriptions :column="2" size="small" bordered class="guide-desc">
        <a-descriptions-item label="备件编号">{{ record.partCode }}</a-descriptions-item>
        <a-descriptions-item label="备件名称">{{ record.partName }}</a-descriptions-item>
        <a-descriptions-item label="数量">{{ record.qty }} {{ record.unit }}</a-descriptions-item>
        <a-descriptions-item label="入库单号">{{ record.inboundNo }}</a-descriptions-item>
      </a-descriptions>
      <a-steps :current="1" direction="vertical" size="small" class="guide-steps">
        <a-step title="前往货区" :description="`根据库位编码 ${record.binCode || '—'} 前往对应货区`" />
        <a-step title="定位货位" description="按层/列标识找到目标货位" />
        <a-step title="PTL 亮灯放置" description="确认电子标签亮起后放入备件" />
        <a-step title="按键确认上架" description="按下货位按键完成上架确认" />
      </a-steps>
    </div>
  </BasicModal>
</template>
<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { confirmPutaway } from './putaway.api';

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();
  const record = ref<any>(null);

  const [registerModal, { setModalProps, closeModal }] = useModalInner((data) => {
    record.value = data.record;
    setModalProps({ confirmLoading: false });
  });

  async function handleConfirm() {
    try {
      setModalProps({ confirmLoading: true });
      await confirmPutaway({ id: record.value.id });
      createMessage.success('上架成功，已反馈 WCS/Emis');
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
<style lang="less" scoped>
  .putaway-guide {
    .guide-bin {
      padding: 18px;
      margin-bottom: 16px;
      text-align: center;
      background: linear-gradient(135deg, #13273f 0%, #1d3a5f 100%);
      border: 1px solid rgba(0, 200, 255, 0.35);
      border-radius: 8px;
    }

    .guide-bin-label {
      color: #7fb6d9;
      font-size: 13px;
    }

    .guide-bin-code {
      font-family: Consolas, monospace;
      font-size: 40px;
      font-weight: 700;
      color: #67e8f9;
      text-shadow: 0 0 16px rgba(0, 200, 255, 0.55);
    }

    .guide-ptl {
      margin-top: 6px;
      color: #fbbf24;
      font-size: 13px;
    }

    .ptl-dot {
      display: inline-block;
      width: 8px;
      height: 8px;
      margin-right: 6px;
      background: #fbbf24;
      border-radius: 50%;
      box-shadow: 0 0 8px #fbbf24;
      animation: ptl-blink 1s infinite;
    }

    .guide-desc {
      margin-bottom: 16px;
    }

    .guide-steps {
      margin-top: 8px;
    }
  }

  @keyframes ptl-blink {
    50% {
      opacity: 0.2;
    }
  }
</style>
