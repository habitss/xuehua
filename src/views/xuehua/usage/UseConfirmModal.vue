<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="备件使用确认" :width="560" :showOkBtn="false" cancelText="取消" @cancel="closeModal">
    <a-alert type="info" show-icon style="margin-bottom: 14px" message="确认实际使用数量，并上传旧件拆机、新件安装、安装细节照片。" />
    <a-descriptions :column="2" size="small" bordered style="margin-bottom: 14px" v-if="record">
      <a-descriptions-item label="备件">{{ record.partCode }} {{ record.partName || '' }}</a-descriptions-item>
      <a-descriptions-item label="领用数量">{{ record.qty }}</a-descriptions-item>
      <a-descriptions-item label="领用产线">{{ record.workshop || '—' }}</a-descriptions-item>
      <a-descriptions-item label="领用人">{{ record.receiver || '—' }}</a-descriptions-item>
    </a-descriptions>
    <BasicForm @register="registerForm" />
    <template #footer>
      <a-button @click="closeModal">取消</a-button>
      <a-button danger @click="submit(false)">登记未使用</a-button>
      <a-button type="primary" @click="submit(true)">提交确认</a-button>
    </template>
  </BasicModal>
</template>

<script lang="ts" name="xuehua-usage-confirm" setup>
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm, type FormSchema } from '/@/components/Form';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { confirmUsage, type UsageRow } from './usage.api';

  const { createMessage } = useMessage();
  const record = ref<UsageRow | null>(null);
  const emit = defineEmits(['success', 'register']);

  /** 表单：实际使用数量/照片/说明（JUpload 对齐入库三要素上传写法） */
  const schemas: FormSchema[] = [
    {
      label: '实际使用数量',
      field: 'usedQty',
      component: 'InputNumber',
      colProps: { span: 24 },
      componentProps: { min: 1, style: { width: '100%' } },
    },
    {
      label: '安装照片',
      field: 'photos',
      component: 'JUpload',
      colProps: { span: 24 },
      componentProps: { fileType: 'image', maxCount: 9, bizPath: 'xuehua/usage' },
      helpMessage: '旧件拆机状态 / 新件安装后 / 安装细节照片',
    },
    {
      label: '使用情况说明',
      field: 'purpose',
      component: 'InputTextArea',
      colProps: { span: 24 },
      componentProps: { rows: 3, placeholder: '请填写备件实际使用情况...' },
    },
  ];

  const [registerForm, { setFieldsValue, getFieldsValue, resetFields }] = useForm({
    schemas,
    showActionButtonGroup: false,
    labelWidth: 100,
  });

  const [registerModal, { closeModal, setModalProps }] = useModalInner(async (data) => {
    record.value = data.record as UsageRow;
    await resetFields();
    await setFieldsValue({ usedQty: record.value?.qty ?? 1, photos: '', purpose: '' });
  });

  async function submit(used: boolean) {
    if (!record.value) return;
    const v = getFieldsValue();
    if (used) {
      const qty = Number(v.usedQty);
      if (!qty || qty < 1) return createMessage.warning('请填写实际使用数量');
      if (record.value.qty && qty > record.value.qty) return createMessage.warning(`实际使用数量不能超过领用数量 ${record.value.qty}`);
    }
    try {
      setModalProps({ confirmLoading: true });
      const res: any = await confirmUsage({
        id: record.value.id,
        used,
        usedQty: used ? Number(v.usedQty) : 0,
        photos: v.photos || '',
        purpose: (v.purpose || '').trim(),
      });
      if (res?.success === false) return createMessage.error(res.message || '提交失败');
      createMessage.success(res?.message || (used ? '已确认使用 · 同步 Emis' : '已登记未使用'));
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
