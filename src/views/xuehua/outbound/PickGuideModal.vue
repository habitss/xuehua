<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="拣配指引" :width="560" :okText="okText" @ok="handleConfirm">
    <div class="pick-guide" v-if="record">
      <!-- 当前待拣行：大字库位 + 数量（多库位单按 seq 逐行拣） -->
      <div class="guide-bin" v-if="currentLine">
        <div class="guide-bin-label">当前待拣 · 第 {{ currentLine.seq }} 站（共 {{ lines.length }} 站）</div>
        <div class="guide-bin-code">{{ currentLine.binCode }}</div>
        <div class="guide-ptl">
          <span class="ptl-dot"></span>
          PTL 电子标签已点亮 · 取 {{ currentLine.qty }} 件
        </div>
      </div>
      <div class="guide-bin guide-done" v-else>
        <div class="guide-bin-code done-text">全部拣配完成</div>
      </div>
      <a-descriptions :column="2" size="small" bordered class="guide-desc">
        <a-descriptions-item label="工单号">{{ record.woNo }}</a-descriptions-item>
        <a-descriptions-item label="备件名称">{{ record.partName }}</a-descriptions-item>
        <a-descriptions-item label="总数">{{ record.qty }}</a-descriptions-item>
        <a-descriptions-item label="领用人">{{ record.receiveUser || record.applyUser }}</a-descriptions-item>
      </a-descriptions>
      <!-- 拣配路线：自动分配的逐库位任务（库龄优先 FIFO） -->
      <div class="guide-lines" v-if="lines.length">
        <div class="guide-lines-title">拣配路线（库龄优先自动分配）</div>
        <div v-for="l in lines" :key="l.id" class="guide-line" :class="{ done: l.status === 1, current: currentLine && currentLine.id === l.id }">
          <span class="gl-seq">{{ l.seq }}</span>
          <span class="gl-bin">{{ l.binCode }}</span>
          <span class="gl-qty">×{{ l.qty }}</span>
          <span class="gl-status">{{ l.status === 1 ? `已拣 · ${l.pickBy || ''}` : '待拣配' }}</span>
        </div>
      </div>
      <a-steps :current="1" direction="vertical" size="small" class="guide-steps" v-if="currentLine">
        <a-step title="前往货区" :description="`根据库位编码 ${currentLine.binCode} 前往对应货区`" />
        <a-step title="PTL 亮灯取货" :description="`按标签亮灯取出 ${currentLine.qty} 件，按 OKKey 确认`" />
        <a-step
          title="确认本站拣配"
          :description="lines.length > 1 ? '确认后自动点亮下一站库位标签；全部站点完成工单出库' : '确认后工单出库、库位台账自动扣减'"
        />
      </a-steps>
    </div>
  </BasicModal>
</template>
<script lang="ts" setup>
  import { ref, computed } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getOutboundLines, confirmPickLine } from './outbound.api';

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();
  const record = ref<any>(null);
  const lines = ref<any[]>([]);

  // 当前待拣行 = seq 最小的未拣行（与后端确认顺序、PTL 链式一致）
  const currentLine = computed(() => lines.value.find((l) => l.status === 0) || null);
  const okText = computed(() => (currentLine.value ? `确认本站 · ${currentLine.value.binCode}` : '关闭'));

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    record.value = data.record;
    setModalProps({ confirmLoading: false });
    await loadLines();
  });

  async function loadLines() {
    try {
      lines.value = (await getOutboundLines({ woNo: record.value.woNo })) || [];
    } catch (e) {
      lines.value = [];
    }
  }

  async function handleConfirm() {
    if (!currentLine.value) {
      closeModal();
      return;
    }
    try {
      setModalProps({ confirmLoading: true });
      await confirmPickLine({ lineId: currentLine.value.id });
      if (lines.value.filter((l) => l.status === 0).length <= 1) {
        // 刚确认的是最后一站：工单出库完成
        createMessage.success('全部拣配完成，工单已出库');
        closeModal();
        emit('success');
      } else {
        createMessage.success(`第 ${currentLine.value.seq} 站确认成功，请前往下一站`);
        await loadLines();
      }
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
<style lang="less" scoped>
  .pick-guide {
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

    .done-text {
      font-size: 26px;
      color: #6ee7a0;
      text-shadow: 0 0 16px rgba(110, 231, 160, 0.5);
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

    .guide-lines {
      margin-bottom: 14px;

      .guide-lines-title {
        margin-bottom: 6px;
        font-size: 12.5px;
        color: #666;
      }

      .guide-line {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 7px 12px;
        margin-bottom: 4px;
        font-size: 13px;
        border: 1px solid #e8e8e8;
        border-radius: 6px;

        &.current {
          background: #e6f7ff;
          border-color: #40a9ff;
        }

        &.done {
          color: #999;
          background: #fafafa;

          .gl-bin {
            text-decoration: line-through;
          }
        }

        .gl-seq {
          width: 20px;
          height: 20px;
          font-weight: 700;
          line-height: 20px;
          text-align: center;
          background: #f0f0f0;
          border-radius: 50%;
        }

        .gl-bin {
          font-family: Consolas, monospace;
          font-weight: 700;
          color: #1e6091;
        }

        .gl-status {
          margin-left: auto;
          font-size: 12px;
          color: #888;
        }
      }
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
