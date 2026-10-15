<template>
  <div class="clearfix">
    <a-upload
      :listType="listType"
      accept="image/*"
      :multiple="multiple"
      :action="uploadUrl"
      :headers="headers"
      :data="{ biz: bizPath }"
      v-model:fileList="uploadFileList"
      :beforeUpload="beforeUpload"
      :disabled="disabled"
      @change="handleChange"
      @preview="handlePreview"
    >
      <div v-if="uploadVisible">
        <div v-if="listType == 'picture-card'">
          <LoadingOutlined v-if="loading" />
          <UploadOutlined v-else />
          <div class="ant-upload-text">{{ text }}</div>
        </div>
        <a-button v-if="listType == 'picture'" :disabled="disabled">
          <UploadOutlined></UploadOutlined>
          {{ text }}
        </a-button>
      </div>
    </a-upload>
    <Image
      :width="200"
      :style="{ display: 'none' }"
      :preview="{
        visible: previewVisible,
        onVisibleChange: (value): void => {previewVisible = value;}
      }"
      :src="previewImage"
    />
    <!-- <a-modal :open="previewVisible" :footer="null" @cancel="handleCancel()" width="auto" 
      wrapClassName="uploadv2-full-modal" :maskClosable="true">
      <div class="img-box" @click="handleCancel()">
        <img alt="example" :src="previewImage" />
      </div>
    </a-modal> -->
  </div>
</template>
<script lang="ts">
  import { defineComponent, ref, computed, watch, nextTick } from 'vue';
  import { LoadingOutlined, UploadOutlined } from '@ant-design/icons-vue';
  import { useRuleFormItem } from '/@/hooks/component/useFormItem';
  import { propTypes } from '/@/utils/propTypes';
  import { useAttrs } from '/@/hooks/core/useAttrs';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getFileAccessHttpUrl, getHeaders, getRandom } from '/@/utils/common/compUtils';
  import { uploadUrl } from '/@/api/common/api';
  import { Image } from 'ant-design-vue';
  import { useGlobSetting } from '/@/hooks/setting';
  const globSetting = useGlobSetting();

  const { createMessage } = useMessage();
  export default defineComponent({
    name: 'JImageUploadV2',
    components: { LoadingOutlined, UploadOutlined, Image },
    inheritAttrs: false,
    props: {
      //绑定值
      value: propTypes.oneOfType([propTypes.string, propTypes.array]),
      //按钮文本
      listType: {
        type: String,
        required: false,
        default: 'picture-card',
      },
      //按钮文本
      text: {
        type: String,
        required: false,
        default: '上传',
      },
      //这个属性用于控制文件上传的业务路径
      bizPath: {
        type: String,
        required: false,
        default: 'temp',
      },
      //是否禁用
      disabled: {
        type: Boolean,
        required: false,
        default: false,
      },
      //上传数量
      fileMax: {
        type: Number,
        required: false,
        default: 1,
      },
    },
    emits: ['options-change', 'change', 'update:value'],
    setup(props, { emit }) {
      const emitData = ref<any[]>([]);
      const attrs = useAttrs();
      const [state] = useRuleFormItem(props, 'value', 'change', emitData);
      //获取文件名
      const getFileName = (path) => {
        if (path.lastIndexOf('\\') >= 0) {
          let reg = new RegExp('\\\\', 'g');
          path = path.replace(reg, '/');
        }
        return path.substring(path.lastIndexOf('/') + 1);
      };
      //token
      const headers = getHeaders();
      //上传状态
      const loading = ref<boolean>(false);
      //是否是初始化加载
      const initTag = ref<boolean>(true);
      //文件列表
      let uploadFileList = ref<any[]>([]);
      //预览图
      const previewImage = ref<string | undefined>('');
      //预览框状态
      const previewVisible = ref<boolean>(false);

      //计算是否开启多图上传
      const multiple = computed(() => {
        return props['fileMax'] > 1 || props['fileMax'] === 0;
      });

      //计算是否可以继续上传
      const uploadVisible = computed(() => {
        if (props['fileMax'] === 0) {
          return true;
        }
        return uploadFileList.value.length < props['fileMax'];
      });

      /**
       * 监听value变化
       */
      watch(
        () => props.value,
        (val) => {
         //update-begin---author:liusq ---date:20230601  for：【issues/556】JImageUpload组件value赋初始值没显示图片------------
            if (val && val instanceof Array) {
            val = val.join(',');
          }
          if (initTag.value == true) {
            initFileList(val);
          }
        },
        { immediate: true }
        //update-end---author:liusq ---date:20230601  for：【issues/556】JImageUpload组件value赋初始值没显示图片------------
      );

      /**
       * 初始化文件列表
       */
      function initFileList(paths) {
        if (!paths || paths.length == 0) {
          uploadFileList.value = [];
          return;
        }
        let files = [] as any;
        let arr = paths.split(',');
        arr.forEach((value) => {
          let url = getFileAccessHttpUrl(value);
          files.push({
            uid: getRandom(10),
            name: getFileName(value),
            status: 'done',
            url: url,
            response: {
              status: 'history',
              message: value,
            },
          });
        });
        uploadFileList.value = files;
      }

      /**
       * 上传前校验
       */
      function beforeUpload(file) {
        let fileType = file.type;
        if (fileType.indexOf('image') < 0) {
          createMessage.info('请上传图片');
          return false;
        }
      }
      /**
       * 文件上传结果回调
       */
      function handleChange({ file, fileList }) {
        initTag.value = false;
        // update-begin--author:liaozhiyang---date:20231116---for：【issues/846】上传多个列表只显示一个
        // uploadFileList.value = fileList;
        if (file.status === 'error') {
          createMessage.error(`${file.name} 上传失败.`);
        }
        // update-begin--author:liaozhiyang---date:20240704---for：【TV360X-1640】上传图片大小超出限制显示优化
        if (file.status === 'done' && file.response.success === false) {
          const failIndex = uploadFileList.value.findIndex((item) => item.uid === file.uid);
          if (failIndex != -1) {
            uploadFileList.value.splice(failIndex, 1);
          }
          createMessage.warning(file.response.message);
          return;
        }
        // update-end--author:liaozhiyang---date:20240704---for：【TV360X-1640】上传图片大小超出限制显示优化
        let fileUrls = [] as any;
        let noUploadingFileCount = 0;
        if (file.status != 'uploading') {
          fileList.forEach((file) => {
            if (file.status === 'done') {
              fileUrls.push(file.response.message);
            }
            if (file.status != 'uploading') {
              noUploadingFileCount++;
            }
          });
          if (file.status === 'removed') {
            handleDelete(file);
          }
          if (noUploadingFileCount == fileList.length) {
            state.value = fileUrls.join(',');
            emit('update:value', fileUrls.join(','));
            // update-begin---author:wangshuai ---date:20221121  for：[issues/248]原生表单内使用图片组件,关闭弹窗图片组件值不会被清空------------
            nextTick(() => {
              initTag.value = true;
            });
            // update-end---author:wangshuai ---date:20221121  for：[issues/248]原生表单内使用图片组件,关闭弹窗图片组件值不会被清空------------
          }
        }
        // update-end--author:liaozhiyang---date:20231116---for：【issues/846】上传多个列表只显示一个
      }

      /**
       * 删除图片
       */
      function handleDelete(file) {
        //如有需要新增 删除逻辑
        console.log(file);
      }

      /**
       * 预览图片
       */
      function handlePreview(file) {
        previewImage.value = file.url || globSetting.uploadUrl+'/'+file.response.message;
        previewVisible.value = true;
      }


      function handleCancel() {
        previewVisible.value = false;
      }

      return {
        state,
        attrs,
        previewImage,
        previewVisible,
        uploadFileList,
        multiple,
        headers,
        loading,
        uploadUrl,
        beforeUpload,
        uploadVisible,
        handlePreview,
        handleCancel,
        handleChange,
      };
    },
  });
</script>
<style scoped>
  .ant-upload-select-picture-card i {
    font-size: 32px;
    color: #999;
  }

  .ant-upload-select-picture-card .ant-upload-text {
    margin-top: 8px;
    color: #666;
  }
</style>
<style>
.uploadv2-full-modal {
    .ant-modal {
      max-width: 100%;
      top: 60px;
      padding-bottom: 10;
      margin: 0;
    }
    .ant-modal-content {
      display: flex;
      flex-direction: column;
      height: calc(100vh);
      background-color: transparent;
    }
    .ant-modal-body {
      flex: 1;
    }
    .img-box{
      display: flex;
      justify-content: center; /* 水平居中 */
      align-items: center; /* 垂直居中 */
      height: 100vh; /* 视口高度，根据需要调整 */
    }
  }
</style>