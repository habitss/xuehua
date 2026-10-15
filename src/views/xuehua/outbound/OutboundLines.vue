<template>
  <div class="outbound-lines" v-if="lines.length">
    <a-table :dataSource="lines" :pagination="false" size="small" rowKey="id">
      <a-table-column title="顺序" data-index="seq" :width="60" />
      <a-table-column title="拣配库位" data-index="binCode" :width="110">
        <template #default="{ text }">
          <span class="line-bin">{{ text }}</span>
        </template>
      </a-table-column>
      <a-table-column title="拣配数量" data-index="qty" :width="90" />
      <a-table-column title="行状态" dataIndex="status" :width="90">
        <template #default="{ text }">
          <a-tag v-if="text === 1" color="success">已拣配</a-tag>
          <a-tag v-else color="warning">待拣配</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="拣配人" data-index="pickBy" :width="100">
        <template #default="{ text }">{{ text || '—' }}</template>
      </a-table-column>
      <a-table-column title="拣配时间" dataIndex="pickTime" :width="150">
        <template #default="{ text }">{{ text ? dayjs(text).format('YYYY-MM-DD HH:mm') : '—' }}</template>
      </a-table-column>
    </a-table>
    <div class="line-hint">分配规则：库龄大的备件优先（该库位最早一次上架时间），数量不足时自动拆多库位</div>
  </div>
  <div v-else class="line-empty">该工单创建于自动分配上线前，无拣配明细</div>
</template>

<script lang="ts" name="xuehua-outbound-lines" setup>
  import { ref, onMounted } from 'vue';
  import dayjs from 'dayjs';
  import { getOutboundLines } from './outbound.api';

  const props = defineProps<{ woNo: string }>();
  const lines = ref<any[]>([]);

  onMounted(async () => {
    try {
      lines.value = (await getOutboundLines({ woNo: props.woNo })) || [];
    } catch (e) {
      lines.value = [];
    }
  });
</script>

<style lang="less" scoped>
  .outbound-lines {
    padding: 4px 8px;
    background: #fafafa;

    .line-bin {
      font-family: Consolas, monospace;
      font-weight: 600;
      color: #1e6091;
    }

    .line-hint {
      margin-top: 6px;
      font-size: 12px;
      color: #999;
    }
  }

  .line-empty {
    padding: 8px;
    color: #999;
    font-size: 12px;
  }
</style>
