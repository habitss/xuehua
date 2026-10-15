<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="isUpdate ? '编辑视频点位' : '新增视频点位'" @ok="submit" :width="640">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" name="xuehua-camera-modal" setup>
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { cameraFormSchema } from './camera.data';
  import { saveCamera } from './camera.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['success', 'register']);
  const isUpdate = ref(true);
  const record = ref<any>(null);

  const [registerForm, { setFieldsValue, validate, resetFields }] = useForm({
    schemas: cameraFormSchema,
    showActionButtonGroup: false,
    labelWidth: 90,
  });

  const [registerModal, { closeModal, setModalProps }] = useModalInner(async (data) => {
    isUpdate.value = !!data?.isUpdate;
    record.value = data?.record || null;
    await resetFields();
    if (record.value) {
      await setFieldsValue({ ...record.value });
    } else if (data?.preset) {
      // 从设备通道列表一键带入（设备类型/通道号/名称）
      await setFieldsValue({ status: 1, rtspPort: 554, ...data.preset });
    }
  });

  async function submit() {
    const v = await validate();
    // 绑定层级联动校验：货架/库位层级必须有对应编码
    if ((v.locationType === 1 || v.locationType === 2) && !v.rackCode) return createMessage.warning('绑定层级为货架/库位时必须填货架编码');
    if (v.locationType === 2 && !v.binCode) return createMessage.warning('绑定层级为库位时必须填库位编码');
    if (v.locationType === 0) {
      v.rackCode = '';
      v.binCode = '';
    } else if (v.locationType === 1) {
      v.binCode = '';
    }
    try {
      setModalProps({ confirmLoading: true });
      await saveCamera({ ...v, id: isUpdate.value ? record.value?.id : undefined }, isUpdate.value);
      createMessage.success(isUpdate.value ? '编辑成功' : '新增成功');
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
