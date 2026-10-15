<template>
  <div :class="prefixCls" class="ems-login">
    <div class="ems-top-actions enter-x">
      <AppDarkModeToggle />
      <AppLocalePicker :showText="false" />
    </div>

    <!-- 左侧：深色科技舞台（能耗数据可视化） -->
    <div class="ems-login__stage">
      <div class="ems-stage-inner">
        <div class="ems-brand">
          <img :src="logoImg" class="ems-brand__logo" alt="卓越管理" />
          <div class="ems-brand__text">
            <h1>卓越管理平台</h1>
            <p>EXCELLENCE MANAGEMENT PLATFORM</p>
          </div>
        </div>


        <div class="ems-footer">
          <p>卓越运营 · 持续改善 · 精益管理</p>
          <p class="ems-footer__copy">© 2026 Excellence Management Platform</p>
        </div>
      </div>

      <div class="ems-particles">
        <span style="left: 8%; animation-duration: 11s; animation-delay: 0s"></span>
        <span style="left: 20%; animation-duration: 14s; animation-delay: 2s"></span>
        <span style="left: 32%; animation-duration: 9s; animation-delay: 4s"></span>
        <span style="left: 45%; animation-duration: 13s; animation-delay: 1s"></span>
        <span style="left: 58%; animation-duration: 10s; animation-delay: 5s"></span>
        <span style="left: 70%; animation-duration: 15s; animation-delay: 3s"></span>
        <span style="left: 82%; animation-duration: 12s; animation-delay: 6s"></span>
        <span style="left: 92%; animation-duration: 9s; animation-delay: 2.5s"></span>
      </div>
    </div>

    <!-- 右侧：登录卡片 -->
    <div class="ems-login__panel">
      <div class="ems-login__card" v-show="type === 'login'">
        <div class="ems-phone-logo">
          <img :src="logoImg" alt="卓越管理" />
        </div>
        <div class="aui-formBox">
          <div class="aui-formWell">
            <div class="aui-flex aui-form-nav investment_title">
              <div class="aui-flex-box" :class="activeIndex === 'accountLogin' ? 'activeNav on' : ''" @click="loginClick('accountLogin')">{{ t('sys.login.signInFormTitle') }}</div>
            </div>
            <div class="aui-form-box" style="min-height: 240px">
              <a-form ref="loginRef" :model="formData" layout="vertical" v-if="activeIndex === 'accountLogin'" @keyup.enter="loginHandleClick">
                <div class="aui-account">
                  <div class="aui-inputClear">
                    <a-form-item :label="'账号'">
                      <a-input class="fix-auto-fill" :placeholder="t('sys.login.accountPlaceholder')" v-model:value="formData.username" />
                    </a-form-item>
                  </div>
                  <div class="aui-inputClear">
                    <a-form-item :label="'密码'">
                      <a-input class="fix-auto-fill" type="password" :placeholder="t('sys.login.passwordPlaceholder')" v-model:value="formData.password" />
                    </a-form-item>
                  </div>
                  <div class="aui-inputClear">
                    <a-form-item :label="'验证码'">
                      <div class="ems-code-row">
                        <a-input class="fix-auto-fill" type="text" :placeholder="t('sys.login.inputCodePlaceholder')" v-model:value="formData.inputCode" />
                        <div class="aui-code">
                          <img v-if="randCodeData.requestCodeSuccess" :src="randCodeData.randCodeImage" @click="handleChangeCheckCode" />
                          <img v-else style="max-width: initial" :src="codeImg" @click="handleChangeCheckCode" />
                        </div>
                      </div>
                    </a-form-item>
                  </div>
                  <div class="aui-inputClear" v-if="showDepart">
                    <div class="JLoginSelectDept">
                      <a-select allow-clear style="width: 100%" :bordered="false" v-model:value="formData.loginOrgCode" :placeholder="t('sys.login.loginOrgCode')">
                        <template #suffixIcon>
                          <Icon icon="ant-design:gold-outline" />
                        </template>
                        <template v-for="depart in departList" :key="depart.orgCode">
                          <a-select-option :value="depart.orgCode">{{ getShortDeptName(depart.label) }}</a-select-option>
                        </template>
                      </a-select>
                    </div>
                  </div>
                </div>
              </a-form>
              <a-form v-else ref="phoneFormRef" :model="phoneFormData" @keyup.enter="loginHandleClick">
                <div class="aui-account phone">
                  <div class="aui-inputClear phoneClear">
                    <a-input class="fix-auto-fill" :placeholder="t('sys.login.mobile')" v-model:value="phoneFormData.mobile" />
                  </div>
                  <div class="aui-inputClear">
                    <a-input class="fix-auto-fill" :maxlength="6" :placeholder="t('sys.login.smsCode')" v-model:value="phoneFormData.smscode" />
                    <div v-if="showInterval" class="aui-code" @click="getLoginCode">
                      <a>{{ t('component.countdown.normalText') }}</a>
                    </div>
                    <div v-else class="aui-code">
                      <span class="aui-get-code code-shape">{{ t('component.countdown.sendText', [unref(timeRuning)]) }}</span>
                    </div>
                  </div>
                  <div class="aui-inputClear" v-if="showDepart">
                    <div class="JLoginSelectDept">
                      <a-select allow-clear style="width: 100%" :bordered="false" v-model:value="phoneFormData.loginOrgCode" :placeholder="t('sys.login.loginOrgCode')">
                        <template #suffixIcon>
                          <Icon icon="ant-design:gold-outline" />
                        </template>
                        <template v-for="depart in departList" :key="depart.orgCode">
                          <a-select-option :value="depart.orgCode">{{ getShortDeptName(depart.label) }}</a-select-option>
                        </template>
                      </a-select>
                    </div>
                  </div>
                </div>
              </a-form>
            </div>
            <div class="aui-formButton">
              <a-button :loading="loginLoading" class="aui-link-login" type="primary" @click="loginHandleClick">
                {{ t('sys.login.loginButton') }}
              </a-button>
            </div>
          </div>
        </div>
      </div>
      <div v-if="forgotLoaded" v-show="type === 'forgot'" :class="`${prefixCls}-form`">
        <MiniForgotpad ref="forgotRef" @go-back="goBack" @success="handleSuccess" />
      </div>
      <div v-if="registerLoaded" v-show="type === 'register'" :class="`${prefixCls}-form`">
        <MiniRegister ref="registerRef" @go-back="goBack" @success="handleSuccess" />
      </div>
      <div v-if="codeLoginLoaded" v-show="type === 'codeLogin'" :class="`${prefixCls}-form`">
        <MiniCodelogin ref="codeRef" @go-back="goBack" @success="handleSuccess" />
      </div>
      <!-- 第三方登录相关弹框 -->
      <ThirdModal ref="thirdModalRef"></ThirdModal>

      <!-- 图片验证码弹窗 -->
      <CaptchaModal @register="captchaRegisterModal" @ok="getLoginCode" />
    </div>
  </div>
</template>
<script lang="ts" setup name="login-mini">
  import { getCaptcha, getCodeInfo } from '/@/api/sys/user';
  import { computed, defineAsyncComponent, onMounted, reactive, ref, toRaw, unref, watch } from 'vue';
  import codeImg from '/@/assets/images/checkcode.png';
  import { useUserStore } from '/@/store/modules/user';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { SmsEnum } from '/@/views/sys/login/useLogin';
  import ThirdModal from '/@/views/sys/login/ThirdModal.vue';
  const MiniForgotpad = defineAsyncComponent(() => import('./MiniForgotpad.vue'));
  const MiniRegister = defineAsyncComponent(() => import('./MiniRegister.vue'));
  const MiniCodelogin = defineAsyncComponent(() => import('./MiniCodelogin.vue'));
  import logoImg from '/@/assets/loginmini/icon/logo.svg';
  import { AppLocalePicker, AppDarkModeToggle } from '/@/components/Application';
  import { createLocalStorage } from '/@/utils/cache';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { GithubFilled, WechatFilled, DingtalkCircleFilled } from '@ant-design/icons-vue';
  import '/@/utils/iconfont2';
  import CaptchaModal from '@/components/jeecg/captcha/CaptchaModal.vue';
  import { useModal } from '@/components/Modal';
  import { ExceptionEnum } from '@/enums/exceptionEnum';
  import { encryptAESCBC } from '/@/utils/cipher';
  import { defHttp } from '@/utils/http/axios';
  import { IconFont } from '/@/utils/iconfont2';

  const { prefixCls } = useDesign('mini-login');
  const { notification, createMessage } = useMessage();
  const userStore = useUserStore();
  const { t } = useI18n();
  const $ls = createLocalStorage();
  const randCodeData = reactive<any>({
    randCodeImage: '',
    requestCodeSuccess: false,
    checkKey: null,
  });
  // 记住用户名
  const rememberMe = ref<boolean>(false);
  const REMEMBER_USERNAME_KEY = 'LOGIN_REMEMBER_USERNAME';
  //手机号登录还是账号登录
  const activeIndex = ref<string>('accountLogin');
  const type = ref<string>('login');
  //账号登录表单字段
  const formData = reactive<any>({
    inputCode: '',
    username: '',
    password: '',
    loginOrgCode: '',
  });
  //手机登录表单字段
  const phoneFormData = reactive<any>({
    mobile: '',
    smscode: '',
    loginOrgCode: '',
  });
  const loginRef = ref();
  //第三方登录弹窗
  const thirdModalRef = ref();
  //扫码登录
  const codeRef = ref();
  //是否显示获取验证码
  const showInterval = ref<boolean>(true);
  //60s
  const timeRuning = ref<number>(60);
  //定时器
  const timer = ref<any>(null);
  //忘记密码
  const forgotRef = ref();
  //注册
  const registerRef = ref();
  const loginLoading = ref<boolean>(false);
  // 以下三个组件懒加载控制标志位，点击时才挂载
  const forgotLoaded = ref<boolean>(false);
  const registerLoaded = ref<boolean>(false);
  const codeLoginLoaded = ref<boolean>(false);
  const [captchaRegisterModal, { openModal: openCaptchaModal }] = useModal();
  defineProps({
    sessionTimeout: {
      type: Boolean,
    },
  });
  //**********************查询部门逻辑begin**********************************************
  //用户部门
  const departList = ref([]);
  //部门显示
  const showDepart = computed(() => {
    return departList.value.length > 1;
  });
  //获取部门缩写
  const getShortDeptName = computed(() => {
    return (deptName) => {
      if (!deptName) return '';
      if (deptName.length > 18) {
        return '...' + deptName.substring(deptName.length - 18, deptName.length);
      }
      return deptName;
    };
  });
  //监听验证码和输入框的修改
  watch(
    () => [formData.inputCode, phoneFormData.smscode],
    () => {
      if ((formData.inputCode && formData.inputCode.length == 4) || (phoneFormData.smscode && phoneFormData.smscode.length == 6)) {
        checkAccount();
      }
    }
  );
  /**
   * 监听账号变化，清除部门信息
   */
  watch(
    () => [formData.username, phoneFormData.mobile, activeIndex.value],
    () => {
      formData.loginOrgCode = null;
      phoneFormData.loginOrgCode = null;
      departList.value = [];
      if ((formData.inputCode && formData.inputCode.length == 4) || (phoneFormData.smscode && phoneFormData.smscode.length == 6)) {
        checkAccount();
      }
    }
  );

  //初始化数据
  let deptTimer;
  function checkAccount() {
    deptTimer && clearTimeout(deptTimer);
    deptTimer = setTimeout(async () => {
      let loginType = activeIndex.value === 'accountLogin' ? 'account' : 'phone';
      // 验证条件提取
      const isValidAccount = loginType === 'account' && formData.username && formData.password;
      const isValidPhone = loginType == 'phone' && phoneFormData.mobile && phoneFormData.smscode;
      let finalFormData = loginType == 'phone' ? { ...phoneFormData } : { ...formData };
      if (!isValidAccount && !isValidPhone) {
        return;
      }
      //查询部门信息前，优先进行账户校验
      if (departList.value && departList.value.length == 0) {
        let params = { ...finalFormData, loginType: activeIndex.value === 'accountLogin' ? 'account' : 'phone' };
        if (loginType == 'account') {
          params['password'] = encryptAESCBC(formData.password);
          params['checkKey'] = randCodeData.checkKey;
        }
        const res = await defHttp.post(
          {
            url: '/sys/loginGetUserDeparts',
            params: { ...params },
          },
          { isTransformResponse: false }
        );
        if (res.success && res.result) {
          let { departs, currentOrgCode } = res.result;
          // 判断当前部门是否在所属的部门列表中
          if (departs && departs.length > 0) {
            // 代码逻辑说明: JHHB-790 用户部门变更，会出现这个情况（因为之前设置的这里只切换部门，过滤了公司和岗位信息）
            const hasCurrentDepart = departs.some((item) => item.orgCode == currentOrgCode);
            formData.loginOrgCode = hasCurrentDepart ? currentOrgCode : null;
            phoneFormData.loginOrgCode = hasCurrentDepart ? currentOrgCode : null;
            departList.value = departs.map((item) => {
              return {
                label: item.departName,
                value: item.orgCode,
                orgCode: item.orgCode,
                departName: item.departName,
              };
            });
          }
        } else {
          //createMessage.warn(res.message);
        }
      }
    }, 500);
  }
  //**********************查询部门逻辑end*************************************************
  /**
   * 获取验证码
   */
  function handleChangeCheckCode() {
    formData.inputCode = '';
    // 代码逻辑说明: [QQYUN-10775]验证码可以复用 #7674------------
    randCodeData.checkKey = new Date().getTime() + Math.random().toString(36).slice(-4); // 1629428467008;
    getCodeInfo(randCodeData.checkKey).then((res) => {
      randCodeData.randCodeImage = res;
      randCodeData.requestCodeSuccess = true;
    });
  }

  /**
   * 切换登录方式
   */
  function loginClick(type) {
    activeIndex.value = type;
  }

  /**
   * 账号或者手机登录
   */
  async function loginHandleClick() {
    if (unref(activeIndex) === 'accountLogin') {
      accountLogin();
    } else {
      //手机号登录
      phoneLogin();
    }
  }

  async function accountLogin() {
    if (!formData.username) {
      createMessage.warn(t('sys.login.accountPlaceholder'));
      return;
    }
    if (!formData.password) {
      createMessage.warn(t('sys.login.passwordPlaceholder'));
      return;
    }
    try {
      loginLoading.value = true;

      // 密码使用AES加密传输
      const encryptedPassword = encryptAESCBC(formData.password);
      const { userInfo } = await userStore.login(
        toRaw({
          password: encryptedPassword,
          username: formData.username,
          loginOrgCode: formData.loginOrgCode,
          captcha: formData.inputCode,
          checkKey: randCodeData.checkKey,
          mode: 'none', //不要默认的错误提示
        })
      );
      if (userInfo) {
        notification.success({
          message: t('sys.login.loginSuccessTitle'),
          description: `${t('sys.login.loginSuccessDesc')}: ${userInfo.realname}`,
          duration: 3,
        });
        // 登录成功后处理记住用户名
        if (rememberMe.value && formData.username) {
          $ls.set(REMEMBER_USERNAME_KEY, formData.username);
        } else {
          $ls.remove(REMEMBER_USERNAME_KEY);
        }
      }
    } catch (error) {
      notification.error({
        message: t('sys.api.errorTip'),
        description: error.message || t('sys.login.networkExceptionMsg'),
        duration: 3,
      });
      handleChangeCheckCode();
    } finally {
      loginLoading.value = false;
    }
  }

  /**
   * 手机号登录
   */
  async function phoneLogin() {
    if (!phoneFormData.mobile) {
      createMessage.warn(t('sys.login.mobilePlaceholder'));
      return;
    }
    if (!phoneFormData.smscode) {
      createMessage.warn(t('sys.login.smsPlaceholder'));
      return;
    }
    try {
      loginLoading.value = true;
      const { userInfo }: any = await userStore.phoneLogin({
        mobile: phoneFormData.mobile,
        captcha: phoneFormData.smscode,
        loginOrgCode: phoneFormData.loginOrgCode,
        mode: 'none', //不要默认的错误提示
      });
      if (userInfo) {
        notification.success({
          message: t('sys.login.loginSuccessTitle'),
          description: `${t('sys.login.loginSuccessDesc')}: ${userInfo.realname}`,
          duration: 3,
        });
      }
    } catch (error) {
      notification.error({
        message: t('sys.api.errorTip'),
        description: error.message || t('sys.login.networkExceptionMsg'),
        duration: 3,
      });
    } finally {
      loginLoading.value = false;
    }
  }

  /**
   * 获取手机验证码
   */
  async function getLoginCode() {
    if (!phoneFormData.mobile) {
      createMessage.warn(t('sys.login.mobilePlaceholder'));
      return;
    }
    // 代码逻辑说明: 【issues/8567】严重：修改密码存在水平越权问题：登录应该用登录模板不应该用忘记密码的模板---
    const result = await getCaptcha({ mobile: phoneFormData.mobile, smsmode: SmsEnum.LOGIN }).catch((res) => {
      if (res.code === ExceptionEnum.PHONE_SMS_FAIL_CODE) {
        openCaptchaModal(true, {});
      }
    });
    if (result) {
      const TIME_COUNT = 60;
      if (!unref(timer)) {
        timeRuning.value = TIME_COUNT;
        showInterval.value = false;
        timer.value = setInterval(() => {
          if (unref(timeRuning) > 0 && unref(timeRuning) <= TIME_COUNT) {
            timeRuning.value = timeRuning.value - 1;
          } else {
            showInterval.value = true;
            clearInterval(unref(timer));
            timer.value = null;
          }
        }, 1000);
      }
    }
  }

  /**
   * 第三方登录
   * @param type
   */
  function onThirdLogin(type) {
    thirdModalRef.value.onThirdLogin(type);
  }

  /**
   * 忘记密码
   */
  function forgetHandelClick() {
    forgotLoaded.value = true;
    type.value = 'forgot';
    setTimeout(() => {
      forgotRef.value?.initForm();
    }, 300);
  }

  /**
   * 返回登录页面
   */
  function goBack() {
    activeIndex.value = 'accountLogin';
    type.value = 'login';
  }

  /**
   * 忘记密码/注册账号回调事件
   * @param value
   */
  function handleSuccess(value) {
    Object.assign(formData, value);
    Object.assign(phoneFormData, { mobile: '', smscode: '' });
    type.value = 'login';
    activeIndex.value = 'accountLogin';
    handleChangeCheckCode();
  }

  /**
   * 注册
   */
  function registerHandleClick() {
    registerLoaded.value = true;
    type.value = 'register';
    setTimeout(() => {
      registerRef.value?.initForm();
    }, 300);
  }

  /**
   * 注册
   */
  function codeHandleClick() {
    codeLoginLoaded.value = true;
    type.value = 'codeLogin';
    setTimeout(() => {
      codeRef.value?.initFrom();
    }, 300);
  }

  onMounted(() => {
    //加载验证码
    handleChangeCheckCode();
    // 恢复已记住的用户名
    const saved = $ls.get(REMEMBER_USERNAME_KEY);
    if (saved) {
      formData.username = saved;
      rememberMe.value = true;
    }
  });
</script>

<style lang="less" scoped>
  @import '/@/assets/loginmini/style/base.less';
  @import '/@/assets/loginmini/style/app-login.less';

  :deep(.ant-input:focus) {
    box-shadow: none;
  }

  // 浏览器自动填充色修正（深底下保持浅色文字）
  input.fix-auto-fill,
  .fix-auto-fill input {
    -webkit-text-fill-color: #e6edf3 !important;
    box-shadow: inset 0 0 0 1000px rgba(255, 255, 255, 0.04) !important;
  }

  .JLoginSelectDept {
    margin: 5px auto;
  }
</style>

<style lang="less">
  @prefix-cls: ~'@{namespace}-mini-login';

  .@{prefix-cls} {
    // 兼容暗色模式下的自动填充文字色
    input.fix-auto-fill,
    .fix-auto-fill input {
      -webkit-text-fill-color: #e6edf3 !important;
    }
  }
</style>
