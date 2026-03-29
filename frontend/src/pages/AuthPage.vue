<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import {
  sharedState,
  send,
  isLoading,
  setNotice,
} from "../stores/sharedState";

const route = useRoute();
const router = useRouter();

const forms = reactive({
  auth: {
    phone: "13800138000",
    code: "",
  },
});

const lastDevCode = ref("");

const redirectTarget = computed(() => {
  const raw = Array.isArray(route.query.redirect)
    ? route.query.redirect[0]
    : route.query.redirect;

  if (typeof raw !== "string" || !raw.startsWith("/") || raw.startsWith("/auth")) {
    return "/";
  }

  return raw;
});

onMounted(async () => {
  if (sharedState.token.value.trim()) {
    await fetchMe({ silent: true });
  }
});

async function fetchMe(options = {}) {
  return send(
    "GET /user/me",
    { method: "GET", path: "/user/me" },
    {
      successMessage: options.silent ? "认证页已同步当前登录用户。" : "已刷新当前登录用户。",
      onSuccess: (data) => {
        sharedState.currentUser.value = data || null;
      },
    },
  );
}

async function sendCode() {
  await send(
    "POST /user/code",
    {
      method: "POST",
      path: "/user/code",
      query: { phone: forms.auth.phone },
    },
    {
      successMessage: "验证码请求已发送。",
      onSuccess: (data) => {
        if (typeof data === "string" && data) {
          forms.auth.code = String(data);
          lastDevCode.value = data;
          setNotice("success", `开发环境验证码已自动回填：${data}`);
          return;
        }

        lastDevCode.value = "";
        setNotice("success", "验证码请求已发送，请查看短信或后端日志。");
      },
    },
  );
}

async function login() {
  await send(
    "POST /user/login",
    {
      method: "POST",
      path: "/user/login",
      body: {
        phone: forms.auth.phone,
        code: forms.auth.code,
      },
    },
    {
      successMessage: "登录成功。若手机号首次出现，后端已自动完成注册。",
      onSuccess: async (data) => {
        sharedState.token.value = data || "";
        await fetchMe({ silent: true });
        await router.replace(redirectTarget.value);
      },
    },
  );
}

function clearLocalToken() {
  sharedState.token.value = "";
  sharedState.currentUser.value = null;
  setNotice("info", "本地 token 已清空。后端会话记录会按自身 TTL 过期。");
}
</script>

<template>
  <section class="auth-page">
    <article class="auth-card ue-washi ue-shadow">
      <div class="auth-topbar">
        <RouterLink :to="redirectTarget" class="auth-back-link">返回来源页</RouterLink>
        <button
          v-if="sharedState.token.value.trim()"
          class="auth-ghost-button"
          @click="clearLocalToken"
        >
          退出登录
        </button>
      </div>

      <div class="auth-header">
        <p class="eyebrow">Auth</p>
        <h1>登录 / 注册</h1>
        <p class="auth-subtitle">验证码登录，首次成功会自动注册账号。</p>
      </div>

      <div class="auth-session">
        <span :class="sharedState.currentUser.value ? 'status-pill success' : 'status-pill muted'">
          {{ sharedState.currentUser.value ? "已登录" : "未登录" }}
        </span>
        <span class="auth-session-text">
          {{ sharedState.currentUser.value?.nickName || "请输入手机号并获取验证码" }}
        </span>
      </div>

      <div class="form-grid">
        <label>
          <span>手机号</span>
          <input v-model="forms.auth.phone" placeholder="13800138000" />
        </label>
        <label>
          <span>验证码</span>
          <input v-model="forms.auth.code" placeholder="6 位验证码" />
        </label>
      </div>

      <p v-if="lastDevCode" class="helper dev-code">
        开发环境验证码：{{ lastDevCode }}
      </p>

      <div class="auth-actions-row">
        <button :disabled="isLoading('POST /user/code')" @click="sendCode">发送验证码</button>
        <button :disabled="isLoading('POST /user/login')" class="accent" @click="login">
          登录
        </button>
      </div>

      <div v-if="sharedState.token.value.trim()" class="auth-footnote">
        <button
          :disabled="isLoading('GET /user/me')"
          class="auth-text-button"
          @click="fetchMe()"
        >
          刷新当前用户
        </button>
      </div>
    </article>
  </section>
</template>
