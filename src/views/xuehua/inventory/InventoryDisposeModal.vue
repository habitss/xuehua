<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    title="差异处置"
    :width="460"
    okText="确认处置"
    @ok="handleDispose"
  >
    <div class="dispose-body" v-if="record">
      <p>
        盘点单 <b>{{ record.pvNo }}</b> 账面数 <b>{{ record.bookQty ?? 0 }}</b>，请录入实盘数后确认处置；
        确认后盘点完成、库存解冻并回传 Emis。
      </p>
      <a-form-item label="实盘数" :required="true">
        <a-input-number v-model:value="realQty" :min="0" :precision="0" style="width: 100%" placeholder="请输入实盘数量" />
      </a-form-item>
    </div>
  </BasicModal>
</template>
<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { disposeInventory } from './inventory.api';

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();
  const record = ref<any>(null);
  const realQty = ref<number | null>(null);

  const [registerModal, { setModalProps, closeModal }] = useModalInner((data) => {
    record.value = data.record;
    realQty.value = data.record?.realQty ?? null;
    setModalProps({ confirmLoading: false });
  });

  async function handleDispose() {
    if (realQty.value === null || realQty.value === undefined) {
      createMessage.warning('请录入实盘数');
      return;
    }
    try {
      setModalProps({ confirmLoading: true });
      await disposeInventory(record.value.id, realQty.value);
      createMessage.success('差异处置完成，已解冻并回传 Emis');
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
<style lang="less" scoped>
  .dispose-body {
    p {
      margin-bottom: 16px;
      color: #555;
    }
  }
</style>
