<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit" :width="700">
    <div v-if="!isUpdate" class="scan-bar">
      <a-input
        v-model:value="scanVal"
        placeholder="扫码枪扫描备件条码 / 送货单条码，回车自动识别填充"
        allow-clear
        @pressEnter="handleScan"
      />
      <a-button :loading="scanning" @click="handleScan">扫码识别</a-button>
    </div>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { formSchema } from './inbound.data';
  import { saveOrUpdateInbound, getInboundById, scanCode } from './inbound.api';
  // 声明Emits
  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();
  const isUpdate = ref(true);
  const scanVal = ref('');
  const scanning = ref(false);
  //表单配置
  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: formSchema,
    showActionButtonGroup: false,
  });
  //表单赋值
  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    //重置表单
    await resetFields();
    setModalProps({ confirmLoading: false });
    scanVal.value = '';
    isUpdate.value = !!data?.isUpdate;
    if (unref(isUpdate)) {
      //获取详情
      data.record = await getInboundById({ id: data.record.id });
      //表单赋值
      await setFieldsValue({
        ...data.record,
      });
    }
  });
  //设置标题
  const getTitle = computed(() => (!unref(isUpdate) ? '新增入库登记' : '编辑入库单'));
  //扫码预填：备件条码回填主数据；送货单号提示已登记
  async function handleScan() {
    const code = scanVal.value?.trim();
    if (!code) {
      createMessage.warning('请先扫描或输入条码');
      return;
    }
    scanning.value = true;
    try {
      const res = await scanCode({ code });
      if (res?.type === 'part') {
        await setFieldsValue({
          partCode: res.partCode,
          partName: res.partName,
          model: res.model,
          unit: res.unit,
        });
        createMessage.success(`已识别备件：${res.partName}（${res.partCode}），请补全数量与三要素`);
      } else if (res?.type === 'delivery') {
        createMessage.warning(`该送货单已登记过，原入库单号：${res.inboundNo}，请勿重复登记`);
      }
    } catch (e) {
      // 错误提示由 http 拦截器统一弹出
    } finally {
      scanning.value = false;
    }
  }
  //表单提交事件
  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      //提交表单
      await saveOrUpdateInbound(values, isUpdate.value);
      //关闭弹窗
      closeModal();
      //刷新列表
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
<style scoped>
  .scan-bar {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;
    padding: 10px 12px;
    background: rgba(22, 119, 255, 0.06);
    border: 1px dashed rgba(22, 119, 255, 0.35);
    border-radius: 6px;
  }
</style>
