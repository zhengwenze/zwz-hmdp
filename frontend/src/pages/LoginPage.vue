<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { sessionState, clearSession } from "../stores/session";
import { setNotice } from "../stores/appState";
import { userApi } from "../services/userApi";
import { isLoading } from "../stores/labState";

const route = useRoute();
const router = useRouter();

const form = reactive({
  phone: "13800138000",
  code: "",
});
const agreed = ref(true);
const countdown = ref(0);
const lastDevCode = ref("");

const redirectTarget = computed(() => {
  const raw = Array.isArray(route.query.redirect)
    ? route.query.redirect[0]
    : route.query.redirect;

  if (typeof raw !== "string" || !raw.startsWith("/") || raw.startsWith("/login")) {
    return "/";
  }

  return raw;
});

const codeButtonText = computed(() =>
  countdown.value > 0 ? `${countdown.value}s 后可重发` : "发送验证码",
);

onMounted(async () => {
  if (sessionState.token.value.trim()) {
    const { data, success } = await userApi.fetchMe({ silentError: true });
    if (success) {
      sessionState.currentUser.value = data || null;
    }
  }
});

function startCountdown() {
  countdown.value = 60;
  const timer = window.setInterval(() => {
    countdown.value -= 1;
    if (countdown.value <= 0) {
      window.clearInterval(timer);
    }
  }, 1000);
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
    form.code = String(data);
    lastDevCode.value = String(data);
    setNotice("success", `开发环境验证码已自动回填：${data}`);
  } else {
    lastDevCode.value = "";
  }

  startCountdown();
}

async function login() {
  if (!agreed.value) {
    setNotice("error", "请先确认用户协议。");
    return;
  }

  const { data, success } = await userApi.login(
    {
      phone: form.phone,
      code: form.code,
    },
    {
      successMessage: "登录成功。",
    },
  );

  if (!success) {
    return;
  }

  sessionState.token.value = data || "";
  const meResult = await userApi.fetchMe({ silentError: true });
  if (meResult.success) {
    sessionState.currentUser.value = meResult.data || null;
  }
  await router.replace(redirectTarget.value);
}
</script>

<template>
  <section class="auth-page consumer-auth-page">
    <article class="auth-card ue-washi ue-shadow">
      <div class="auth-topbar">
        <RouterLink :to="redirectTarget" class="auth-back-link">返回来源页</RouterLink>
        <button
          v-if="sessionState.token.value.trim()"
          class="auth-ghost-button"
          @click="clearSession('本地 token 已清空。')"
        >
          清空登录态
        </button>
      </div>

      <div class="auth-header">
        <p class="eyebrow">Login</p>
        <h1>手机号快捷登录</h1>
        <p class="auth-subtitle">
          沿用黑马点评原型的验证码登录路径，但视觉保持浮世绘数字化风格。
        </p>
      </div>

      <div class="auth-session">
        <span :class="sessionState.currentUser.value ? 'status-pill success' : 'status-pill muted'">
          {{ sessionState.currentUser.value ? "已登录" : "未登录" }}
        </span>
        <span class="auth-session-text">
          {{ sessionState.currentUser.value?.nickName || "首次登录会自动注册账号" }}
        </span>
      </div>

      <div class="form-grid">
        <label>
          <span>手机号</span>
          <input v-model="form.phone" placeholder="13800138000" />
        </label>
        <label>
          <span>验证码</span>
          <input v-model="form.code" placeholder="6 位验证码" />
        </label>
      </div>

      <p v-if="lastDevCode" class="helper dev-code">
        开发环境验证码：{{ lastDevCode }}
      </p>

      <label class="consumer-consent">
        <input v-model="agreed" type="checkbox" />
        <span>我已阅读并同意黑马点评用户协议与隐私政策</span>
      </label>

      <div class="auth-actions-row">
        <button
          :disabled="countdown > 0 || isLoading('POST /user/code')"
          @click="sendCode"
        >
          {{ codeButtonText }}
        </button>
        <button :disabled="isLoading('POST /user/login')" class="accent" @click="login">
          登录
        </button>
      </div>
    </article>
  </section>
</template>
