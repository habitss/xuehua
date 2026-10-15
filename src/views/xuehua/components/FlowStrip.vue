<template>
  <div class="xh-flow-strip">
    <template v-for="(step, i) in steps" :key="step.key">
      <div
        class="xh-flow-card"
        :class="{ active: activeKey === step.key }"
        @click="$emit('select', step.key)"
      >
        <div class="xh-flow-title">{{ step.title }}</div>
        <div class="xh-flow-num">{{ step.count ?? 0 }}</div>
      </div>
      <div v-if="i < steps.length - 1" class="xh-flow-arrow">
        <Icon icon="ant-design:right-outlined" />
      </div>
    </template>
  </div>
</template>

<script lang="ts" setup>
  import { Icon } from '/@/components/Icon';

  interface FlowStep {
    key: string;
    title: string;
    count?: number;
  }

  defineProps({
    steps: { type: Array as () => FlowStep[], required: true },
    activeKey: { type: [String, Number] as PropType<string | number | null>, default: null },
  });

  defineEmits(['select']);
</script>

<style lang="less" scoped>
  .xh-flow-strip {
    display: flex;
    align-items: stretch;
    margin-bottom: 12px;
  }

  .xh-flow-card {
    flex: 1;
    min-width: 120px;
    padding: 10px 16px 12px;
    text-align: center;
    cursor: pointer;
    background: linear-gradient(135deg, #13273f 0%, #1d3a5f 100%);
    border: 1px solid rgba(0, 200, 255, 0.25);
    border-radius: 8px;
    transition: all 0.2s;

    &:hover {
      border-color: rgba(0, 200, 255, 0.6);
      box-shadow: 0 0 10px rgba(0, 200, 255, 0.25);
    }

    &.active {
      background: linear-gradient(135deg, #0f3557 0%, #1e6091 100%);
      border-color: rgba(0, 200, 255, 0.9);
      box-shadow: 0 0 14px rgba(0, 200, 255, 0.35);
    }
  }

  .xh-flow-title {
    color: #8fb8d4;
    font-size: 13px;
    letter-spacing: 1px;
  }

  .xh-flow-num {
    font-family: Consolas, monospace;
    font-size: 26px;
    font-weight: 700;
    line-height: 1.2;
    background: linear-gradient(180deg, #67e8f9, #38bdf8);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    text-shadow: 0 0 18px rgba(56, 189, 248, 0.45);
  }

  .xh-flow-arrow {
    display: flex;
    align-items: center;
    padding: 0 6px;
    color: #5b7a9d;
    font-size: 16px;
  }
</style>
