<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="盘点详情" :width="1000" :showOkBtn="false" cancelText="关闭">
    <div class="inv-detail" v-if="record">
      <a-descriptions :column="2" size="small" bordered class="detail-desc">
        <a-descriptions-item label="盘点单号">{{ record.pvNo }}</a-descriptions-item>
        <a-descriptions-item label="来源">{{ record.source }}</a-descriptions-item>
        <a-descriptions-item label="区域">{{ record.zoneCode || '全仓' }}</a-descriptions-item>
        <a-descriptions-item label="范围">{{ record.scope }}</a-descriptions-item>
        <a-descriptions-item label="执行人">{{ record.executor }}</a-descriptions-item>
        <a-descriptions-item label="识别方式">{{ record.identifyType }}</a-descriptions-item>
        <a-descriptions-item label="账面数">{{ record.bookQty }}</a-descriptions-item>
        <a-descriptions-item label="实盘数">
          <span :class="{ diff: hasDiff }">{{ record.realQty ?? '—' }}</span>
          <Tag v-if="hasDiff" color="error" style="margin-left: 8px">差异</Tag>
        </a-descriptions-item>
        <a-descriptions-item label="库存冻结">
          <Tag :color="record.frozenFlag === 1 ? 'error' : 'success'">{{ record.frozenFlag === 1 ? '已冻结' : '正常' }}</Tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ renderTime(record.createTime) }}</a-descriptions-item>
      </a-descriptions>
      <div class="detail-progress">
        <span class="progress-label">盘点进度</span>
        <a-progress :percent="record.progress ?? 0" :status="record.status === 4 ? 'exception' : record.status === 3 ? 'success' : 'active'" />
      </div>
      <a-steps :current="record.stage ?? 0" size="small" class="detail-steps">
        <a-step v-for="name in inventoryStages" :key="name" :title="name" />
      </a-steps>    </div>
  </BasicModal>
</template>
<script lang="ts" setup>
  import { ref, computed } from 'vue';
  import { Tag } from 'ant-design-vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { inventoryStages } from '../components/status';
  import { renderTime } from './inventory.data';

  const record = ref<any>(null);
  const hasDiff = computed(() => {
    const r = record.value;
    return !!(r && r.realQty !== null && r.realQty !== undefined && r.bookQty !== null && r.bookQty !== undefined && r.realQty !== r.bookQty);
  });

  const [registerModal] = useModalInner((data) => {
    record.value = data.record;
  });
</script>
<style lang="less" scoped>
  .inv-detail {
    // 两侧留白，内容不顶边
    padding: 4px 20px 8px;

    .detail-desc {
      margin-bottom: 16px;
    }

    .detail-progress {
      display: flex;
      gap: 12px;
      align-items: center;
      margin-bottom: 20px;

      .progress-label {
        flex-shrink: 0;
        font-weight: 600;
      }
    }

    .diff {
      color: #f5222d;
      font-weight: 700;
    }

    .detail-steps {
      padding-top: 4px;

      :deep(.ant-steps-item-title) {
        // 7 个阶段名窄屏也不截断，允许换行
        white-space: normal;
      }
    }
  }
</style>
