<script setup>
import { reactive } from "vue";
import {
  sharedState,
  send,
  isLoading,
  markTouched,
  endpointBadgeClass,
  prettify,
} from "../stores/sharedState";
import { cloneWithoutEmpty } from "../api";

const forms = reactive({
  user: {
    phone: "13800138000",
    code: "",
    password: "",
    infoId: "1",
    profileId: "1",
  },
});

async function fetchMe() {
  return send(
    "GET /user/me",
    { method: "GET", path: "/user/me" },
    {
      successMessage: "已刷新当前登录用户",
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
      query: { phone: forms.user.phone },
    },
    {
      successMessage: "验证码请求已发送。",
      onSuccess: (data) => {
        if (typeof data === "string" && data) {
          forms.user.code = String(data);
          sharedState.lastDevCode.value = data;
          sharedState.notice.type = "success";
          sharedState.notice.message = `验证码已自动回填到表单：${data}`;
        } else {
          sharedState.lastDevCode.value = "";
          sharedState.notice.message = "验证码请求已发送。当前环境未开启自动回填，请查看短信或后端日志。";
        }
      },
    },
  );
}

async function login() {
  const result = await send(
    "POST /user/login",
    {
      method: "POST",
      path: "/user/login",
      body: cloneWithoutEmpty({
        phone: forms.user.phone,
        code: forms.user.code,
        password: forms.user.password,
      }),
    },
    {
      successMessage: "登录成功，token 已写入本地存储。",
      onSuccess: async (data) => {
        sharedState.token.value = data || "";
        await fetchMe();
      },
    },
  );
  return result;
}

async function logout() {
  await send(
    "POST /user/logout",
    { method: "POST", path: "/user/logout" },
    { successMessage: "后端 logout 已调用。" },
  );
}

function clearLocalToken() {
  sharedState.token.value = "";
  sharedState.currentUser.value = null;
  sharedState.notice.type = "info";
  sharedState.notice.message = "本地 token 已清空。后端 logout 接口仍保留单独按钮供你验证。";
}

async function fetchUserInfo() {
  await send(
    "GET /user/info/{id}",
    { method: "GET", path: `/user/info/${forms.user.infoId}` },
    {
      successMessage: "用户详情已更新。",
      onSuccess: (data) => {
        sharedState.userDetail.value = data || null;
      },
    },
  );
}

async function fetchUserSummary() {
  await send(
    "GET /user/{id}",
    { method: "GET", path: `/user/${forms.user.profileId}` },
    {
      successMessage: "用户基础信息已更新。",
      onSuccess: (data) => {
        sharedState.userSummary.value = data || null;
      },
    },
  );
}

async function signToday() {
  await send(
    "POST /user/sign",
    { method: "POST", path: "/user/sign" },
    { successMessage: "签到成功。" },
  );
}

async function fetchSignCount() {
  await send(
    "GET /user/sign/count",
    { method: "GET", path: "/user/sign/count" },
    {
      successMessage: "已查询连续签到天数。",
      onSuccess: (data) => {
        sharedState.signCount.value = data ?? null;
      },
    },
  );
}
</script>

<template>
  <section id="user" class="module-section">
    <div class="section-title">
      <div>
        <p class="eyebrow">User APIs</p>
        <h2>用户模块</h2>
      </div>
      <span class="section-hint">8 个接口，登录后可验证受保护接口</span>
    </div>

    <div class="card-grid two">
      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card ukiyo-e-digital-animate-in">
        <div class="panel-head">
          <h3>验证码与登录</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['POST /user/code'])">/user/code</span>
        </div>
        <div class="form-grid">
          <label>
            <span>手机号</span>
            <input v-model="forms.user.phone" placeholder="13800138000" />
          </label>
          <label>
            <span>验证码</span>
            <input v-model="forms.user.code" placeholder="后端日志中的 6 位验证码" />
          </label>
          <p v-if="sharedState.lastDevCode.value" class="helper dev-code">
            开发环境验证码已回填：{{ sharedState.lastDevCode.value }}
          </p>
          <label>
            <span>密码</span>
            <input v-model="forms.user.password" placeholder="后端当前不会使用该字段" />
          </label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('POST /user/code')" @click="sendCode">发送验证码</button>
          <button :disabled="isLoading('POST /user/login')" class="accent" @click="login">验证码登录</button>
        </div>
        <p class="helper">登录成功后 token 会写入本地存储，并自动刷新 `/user/me`。</p>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card ukiyo-e-digital-animate-in">
        <div class="panel-head">
          <h3>会话与签到</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /user/me'])">受保护接口</span>
        </div>
        <div class="button-row wrap">
          <button :disabled="isLoading('GET /user/me')" @click="fetchMe">刷新当前用户</button>
          <button :disabled="isLoading('POST /user/logout')" @click="logout">调用 logout</button>
          <button class="secondary" @click="clearLocalToken">仅清空本地 token</button>
          <button :disabled="isLoading('POST /user/sign')" @click="signToday">今日签到</button>
          <button :disabled="isLoading('GET /user/sign/count')" @click="fetchSignCount">查询连续签到</button>
        </div>
        <div class="inline-stats">
          <div>
            <span class="label">当前用户</span>
            <strong>{{ sharedState.currentUser.value?.nickName || "--" }}</strong>
          </div>
          <div>
            <span class="label">连续签到</span>
            <strong>{{ sharedState.signCount.value ?? "--" }}</strong>
          </div>
        </div>
        <p class="helper">`/user/logout` 当前由后端固定返回"功能未完成"，保留该按钮用于接口验证。</p>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>查询用户详情</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /user/info/{id}'])">/user/info/{id}</span>
        </div>
        <div class="form-grid single">
          <label>
            <span>用户 ID</span>
            <input v-model="forms.user.infoId" />
          </label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('GET /user/info/{id}')" @click="fetchUserInfo">查询详情</button>
        </div>
        <pre class="json-box">{{ prettify(sharedState.userDetail.value || { message: "尚未查询" }) }}</pre>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>查询用户基础信息</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /user/{id}'])">/user/{id}</span>
        </div>
        <div class="form-grid single">
          <label>
            <span>用户 ID</span>
            <input v-model="forms.user.profileId" />
          </label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('GET /user/{id}')" @click="fetchUserSummary">查询基础信息</button>
        </div>
        <pre class="json-box">{{ prettify(sharedState.userSummary.value || { message: "尚未查询" }) }}</pre>
      </article>
    </div>
  </section>
</template>
