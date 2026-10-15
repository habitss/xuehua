<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose :title="title" :width="800" :minHeight="30" @ok="handleSubmit">
    <BasicForm @register="registerForm" name="MdmDictCategorizeForm"></BasicForm>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { categorizeFormSchema } from '../dict.data';
  import { categorizeSaveOrUpdate } from '../dict.api';

  // Emits声明
  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(true);
  const isDetail = ref(false);
  const expandedRowKeys = ref([]);
  // 当前编辑的数据
  let model: Nullable<Recordable> = null;
  // 表单配置
  const [registerForm, { setProps, resetFields, setFieldsValue, validate, scrollToField }] = useForm({
    labelWidth: 150,
    schemas: categorizeFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
  });
  // 表单赋值
  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    // 重置表单
    await resetFields();
    setModalProps({ confirmLoading: false, showCancelBtn: !!data?.showFooter, showOkBtn: !!data?.showFooter });
    isUpdate.value = !!data?.isUpdate;
    isDetail.value = !!data?.showFooter;
    if (data?.record) {
      model = data.record;
      // 表单赋值
      await setFieldsValue({
        ...data.record,
      });
    } else {
      model = null;
    }
    // 隐藏底部时禁用整个表单
    setProps({ disabled: !data?.showFooter });
  });
  // 设置标题
  const title = computed(() => (!unref(isUpdate) ? '新增' : !unref(isDetail) ? '详情' : '编辑'));
  // 表单提交事件
  async function handleSubmit() {
    try {
      let values = await validate();
      setModalProps({ confirmLoading: true });
      // 提交表单
      await categorizeSaveOrUpdate(values, isUpdate.value);
      // 关闭弹窗
      closeModal();
      // 刷新列表
      emit('success', {
        isUpdate: unref(isUpdate),
        values: { ...values },
        expandedArr: unref(expandedRowKeys).reverse(),
        // 是否更改了父级节点
        changeParent: model != null && model['parentId'] != values['parentId'],
      });
    } catch ({ errorFields }) {
      if (errorFields) {
        const firstField = errorFields[0];
        if (firstField) {
          scrollToField(firstField.name, { behavior: 'smooth', block: 'center' });
        }
      }
      return Promise.reject(errorFields);
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style lang="less" scoped>
  /** 时间和数字输入框样式 */
  :deep(.ant-input-number) {
    width: 100%;
  }

  :deep(.ant-calendar-picker) {
    width: 100%;
  }
</style>
