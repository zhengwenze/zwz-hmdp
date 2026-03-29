<script setup>
import { reactive, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";
import {
  sharedState,
  send,
  isLoading,
  endpointBadgeClass,
  prettify,
} from "../stores/sharedState";

const route = useRoute();

const forms = reactive({
  user: {
    infoId: "1",
    profileId: "1",
  },
});
const userDetail = ref(null);
const userSummary = ref(null);
const signCount = ref(null);

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

async function fetchUserInfo() {
  await send(
    "GET /user/info/{id}",
    { method: "GET", path: `/user/info/${forms.user.infoId}` },
    {
      successMessage: "用户详情已更新。",
      onSuccess: (data) => {
        userDetail.value = data || null;
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
        userSummary.value = data || null;
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
        signCount.value = data ?? null;
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
      <span class="section-hint">登录入口已迁移到独立认证页</span>
    </div>

    <div class="card-grid two">
      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card ukiyo-e-digital-animate-in">
        <div class="panel-head">
          <h3>认证入口</h3>
          <span class="status-pill muted">独立页面</span>
        </div>
        <p class="helper">
          登录 / 注册已经迁移到独立认证页。当前项目采用验证码登录，首次成功登录时后端会自动创建账号。
        </p>
        <div class="button-row wrap">
          <RouterLink
            :to="{ path: '/auth', query: { redirect: route.fullPath || '/user' } }"
            class="link-button"
          >
            打开登录 / 注册页
          </RouterLink>
          <button :disabled="isLoading('GET /user/me')" class="accent" @click="fetchMe">
            刷新当前用户
          </button>
        </div>
        <div class="inline-stats">
          <div>
            <span class="label">当前用户</span>
            <strong>{{ sharedState.currentUser.value?.nickName || "--" }}</strong>
          </div>
          <div>
            <span class="label">用户 ID</span>
            <strong>{{ sharedState.currentUser.value?.id ?? "--" }}</strong>
          </div>
        </div>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card ukiyo-e-digital-animate-in">
        <div class="panel-head">
          <h3>会话与签到</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /user/me'])">受保护接口</span>
        </div>
        <div class="button-row wrap">
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
            <strong>{{ signCount ?? "--" }}</strong>
          </div>
        </div>
        <p class="helper">如需重新登录或清空本地 token，请前往独立“登录 / 注册”页处理。</p>
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
        <pre class="json-box">{{ prettify(userDetail || { message: "尚未查询" }) }}</pre>
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
        <pre class="json-box">{{ prettify(userSummary || { message: "尚未查询" }) }}</pre>
      </article>
    </div>
  </section>
</template>
