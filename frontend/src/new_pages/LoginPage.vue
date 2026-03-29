<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { userApi } from "../services/userApi";
import { clearSession, sessionState } from "../stores/session";
import { setNotice } from "../stores/appState";

const route = useRoute();
const router = useRouter();

const form = reactive({
  phone: "13800138000",
  code: "",
});
const countdown = ref(0);
const agreed = ref(true);
const lastDevCode = ref("");

const redirectTarget = computed(() => {
  const raw = Array.isArray(route.query.redirect)
    ? route.query.redirect[0]
    : route.query.redirect;

  if (
    typeof raw !== "string"
    || !raw.startsWith("/")
    || raw.startsWith("/login")
  ) {
    return "/me";
  }

  return raw;
});

const buttonText = computed(() =>
  countdown.value > 0 ? `${countdown.value}s 后可重发` : "发送验证码",
);

function startCountdown() {
  countdown.value = 60;
  const timer = window.setInterval(() => {
    countdown.value -= 1;
    if (countdown.value <= 0) {
      window.clearInterval(timer);
    }
  }, 1000);
}

async function loadCurrentSession() {
  if (!sessionState.token.value.trim()) {
    return;
  }
  const { data, success } = await userApi.fetchMe({ silentError: true });
  if (success) {
    sessionState.currentUser.value = data || null;
  }
}

async function sendCode() {
  if (!form.phone.trim()) {
    setNotice("error", "请输入手机号。");
    return;
  }

  const { data, success } = await userApi.sendCode(form.phone, {
    successMessage: "验证码请求已发送。",
  });
  if (!success) {
    return;
  }

  if (typeof data === "string" && data) {
    form.code = data;
    lastDevCode.value = data;
  } else {
    lastDevCode.value = "";
  }
  startCountdown();
}

async function login() {
  if (!agreed.value) {
    setNotice("error", "请先勾选用户协议。");
    return;
  }

  const { data, success } = await userApi.login(
    { phone: form.phone, code: form.code },
    { successMessage: "登录成功。" },
  );
  if (!success) {
    return;
  }

  sessionState.token.value = data || "";
  await loadCurrentSession();
  router.replace(redirectTarget.value);
}

onMounted(loadCurrentSession);
</script>

<template>
  <section class="app-page">
    <ElCard class="page-panel">
      <template #header>
        <div>
          <h1 class="login-card__title">登录 HMDP 工作台</h1>
          <p class="login-card__desc">
            登录后即可继续体验用户、关注、秒杀下单和发笔记能力。
          </p>
        </div>
      </template>

      <ElDescriptions :column="2" border>
        <ElDescriptionsItem label="登录状态">
          {{ sessionState.token.value.trim() ? "已登录" : "未登录" }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="当前用户">
          {{ sessionState.currentUser.value?.nickName || "--" }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="跳转目标">
          {{ redirectTarget }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="开发验证码">
          {{ lastDevCode || "--" }}
        </ElDescriptionsItem>
      </ElDescriptions>

      <ElDivider />

      <ElForm label-position="top">
        <ElFormItem label="手机号">
          <ElInput v-model="form.phone" placeholder="13800138000" />
        </ElFormItem>
        <ElFormItem label="验证码">
          <ElInput
            v-model="form.code"
            placeholder="请输入 6 位验证码"
            @keyup.enter="login"
          />
        </ElFormItem>
        <ElFormItem>
          <ElCheckbox v-model="agreed">
            我已阅读并同意用户协议与隐私政策
          </ElCheckbox>
        </ElFormItem>
      </ElForm>

      <div class="page-actions">
        <ElButton :disabled="countdown > 0" @click="sendCode">
          {{ buttonText }}
        </ElButton>
        <ElButton type="primary" @click="login">立即登录</ElButton>
        <ElButton
          v-if="sessionState.token.value.trim()"
          type="info"
          plain
          @click="clearSession('本地登录态已清空。')"
        >
          清空本地登录态
        </ElButton>
      </div>
    </ElCard>
  </section>
</template>
