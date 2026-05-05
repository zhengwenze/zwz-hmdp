<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { userApi } from "../services/userApi";
import { sessionState } from "../stores/session";
import { isLoading } from "../stores/labState";

const route = useRoute();
const router = useRouter();

const form = reactive({
  phone: "13800138000",
  code: "",
});
const countdown = ref(0);
const formError = ref("");

const redirectTarget = computed(() => {
  const raw = Array.isArray(route.query.redirect)
    ? route.query.redirect[0]
    : route.query.redirect;

  if (
    typeof raw !== "string" ||
    !raw.startsWith("/") ||
    raw.startsWith("/login")
  ) {
    return "/me";
  }

  return raw;
});

const isLoginLoading = computed(() => isLoading("POST /user/login"));
const isCodeLoading = computed(() => isLoading("POST /user/code"));

const codeButtonText = computed(() => {
  if (isCodeLoading.value) {
    return "发送中";
  }
  return countdown.value > 0 ? `${countdown.value}s` : "发送";
});

async function loadCurrentSession() {
  if (!sessionState.token.value.trim()) {
    return;
  }
  const { data, success } = await userApi.fetchMe({ silentError: true });
  if (success) {
    sessionState.currentUser.value = data || null;
  }
}

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
  formError.value = "";

  if (!form.phone.trim()) {
    formError.value = "请输入手机号。";
    return;
  }

  const { data, success } = await userApi.sendCode(form.phone.trim(), {
    silentError: true,
    onError: (body) => {
      formError.value = body?.errorMsg || "发送失败。";
    },
  });

  if (!success) {
    return;
  }

  if (typeof data === "string" && data) {
    form.code = data;
  }
  startCountdown();
}

async function login() {
  formError.value = "";

  if (!form.phone.trim() || !form.code.trim()) {
    formError.value = "请填写完整。";
    return;
  }

  const { data, success } = await userApi.login(
    {
      phone: form.phone.trim(),
      code: form.code.trim(),
    },
    {
      silentError: true,
      onError: (body) => {
        formError.value = body?.errorMsg || "登录失败。";
      },
    },
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
  <section class="login-page">
    <form class="login-card" @submit.prevent="login">
      <label class="login-field">
        <span>手机号</span>
        <input
          v-model="form.phone"
          autocomplete="tel"
          inputmode="tel"
          placeholder="手机号"
          type="tel"
        />
      </label>

      <label class="login-field">
        <span>验证码</span>
        <div class="code-row">
          <input
            v-model="form.code"
            autocomplete="one-time-code"
            inputmode="numeric"
            placeholder="验证码"
            type="text"
          />
          <button
            class="code-button"
            :disabled="countdown > 0 || isCodeLoading"
            type="button"
            @click="sendCode"
          >
            {{ codeButtonText }}
          </button>
        </div>
      </label>

      <p v-if="formError" class="login-error">{{ formError }}</p>

      <button class="login-button" :disabled="isLoginLoading" type="submit">
        {{ isLoginLoading ? "登录中" : "登录" }}
      </button>

      <div class="login-links">
        <a href="#" @click.prevent="formError = '暂未开放。'">忘记密码</a>
        <a href="#" @click.prevent="formError = '暂未开放。'">注册</a>
      </div>
    </form>
  </section>
</template>

<style scoped>
:global(.blank-layout) {
  padding: 0;
  background: #fafafa;
}

:global(.blank-layout__content) {
  width: 100%;
  gap: 0;
}

.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
  background: #fafafa;
}

.login-card {
  width: 100%;
  max-width: 320px;
  padding: 24px;
  border: 1px solid #e4e4e7;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(24, 24, 27, 0.06);
}

.login-field {
  display: block;
}

.login-field + .login-field {
  margin-top: 14px;
}

.login-field span {
  display: block;
  margin-bottom: 6px;
  color: #52525b;
  font-size: 13px;
  line-height: 1.4;
}

.login-field input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d4d4d8;
  border-radius: 10px;
  outline: none;
  background: #ffffff;
  color: #18181b;
  font: inherit;
  font-size: 14px;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.login-field input::placeholder {
  color: #a1a1aa;
}

.login-field input:focus {
  border-color: #18181b;
  box-shadow: 0 0 0 3px rgba(24, 24, 27, 0.1);
}

.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 76px;
  gap: 8px;
}

.code-button {
  height: 40px;
  border: 1px solid #d4d4d8;
  border-radius: 10px;
  background: #ffffff;
  color: #3f3f46;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  transition:
    border-color 160ms ease,
    color 160ms ease,
    opacity 160ms ease;
}

.code-button:hover:not(:disabled) {
  border-color: #18181b;
  color: #18181b;
}

.code-button:disabled {
  cursor: default;
  opacity: 0.55;
}

.login-error {
  min-height: 18px;
  margin: 12px 0 0;
  color: #dc2626;
  font-size: 13px;
  line-height: 1.4;
}

.login-button {
  width: 100%;
  height: 40px;
  margin-top: 16px;
  border: 0;
  border-radius: 10px;
  background: #18181b;
  color: #ffffff;
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 500;
  transition:
    background 160ms ease,
    opacity 160ms ease;
}

.login-button:hover:not(:disabled) {
  background: #09090b;
}

.login-button:disabled {
  cursor: default;
  opacity: 0.65;
}

.login-links {
  display: flex;
  justify-content: space-between;
  margin-top: 14px;
  color: #71717a;
  font-size: 13px;
  line-height: 1.4;
}

.login-links a {
  color: inherit;
}

.login-links a:hover {
  color: #18181b;
}

@media (max-width: 420px) {
  .login-page {
    padding: 16px;
  }

  .login-card {
    max-width: 100%;
    padding: 20px;
  }
}
</style>
