<script setup>
import { computed, onMounted } from "vue";
import { sharedState, send } from "../stores/sharedState";

const effectiveApiBase = computed(
  () => sharedState.apiBaseUrl.value.trim() || "同源代理（Vite dev proxy）",
);
const effectiveAssetBase = computed(
  () => sharedState.assetBaseUrl.value.trim() || sharedState.apiBaseUrl.value.trim(),
);
const isLoggedIn = computed(() => Boolean(sharedState.token.value.trim()));
const shortToken = computed(() => {
  if (!sharedState.token.value) {
    return "未登录";
  }
  if (sharedState.token.value.length < 16) {
    return sharedState.token.value;
  }
  return `${sharedState.token.value.slice(0, 8)}...${sharedState.token.value.slice(-6)}`;
});

onMounted(async () => {
  await send(
    "GET /shop-type/list",
    {
      method: "GET",
      path: "/shop-type/list",
    },
    {
      successMessage: "分类列表已刷新。",
      onSuccess: (data) => {
        sharedState.shopTypes.value = Array.isArray(data) ? data : [];
      },
    },
  );
  await send(
    "GET /blog/hot",
    {
      method: "GET",
      path: "/blog/hot",
      query: { current: "1" },
    },
    {
      successMessage: "热门博客已刷新。",
      onSuccess: (data) => {
        sharedState.hotBlogs.value = Array.isArray(data) ? data : [];
      },
    },
  );
  if (sharedState.token.value) {
    await send(
      "GET /user/me",
      {
        method: "GET",
        path: "/user/me",
      },
      {
        successMessage: "已刷新当前登录用户",
        onSuccess: (data) => {
          sharedState.currentUser.value = data || null;
        },
      },
    );
  }
});
</script>

<template>
  <section
    id="hero"
    class="hero-card ue-wave-bg ue-washi ue-shadow ukiyo-e-digital-accent-corner"
  >
    <div class="hero-copy">
      <p class="eyebrow">HMDP Frontend Console</p>
      <h1>Ukiyo-e Digital 全接口前端</h1>
      <p class="hero-text">
        以和纸底、靛蓝边、朱印强调组织一个可直接联调的前端工作台。接口状态、请求日志、业务数据和错误信息全部在页面内可见。
      </p>
    </div>
    <div class="hero-grid">
      <div class="info-card ukiyo-e-digital-card">
        <span class="label">API Base URL</span>
        <input v-model="sharedState.apiBaseUrl.value" placeholder="留空则使用同源代理" />
        <small>当前：{{ effectiveApiBase }}</small>
      </div>
      <div class="info-card ukiyo-e-digital-card">
        <span class="label">静态资源基址</span>
        <input v-model="sharedState.assetBaseUrl.value" placeholder="图片预览基址，可留空" />
        <small>上传图片预览时使用</small>
      </div>
      <div class="info-card ukiyo-e-digital-card">
        <span class="label">Token 状态</span>
        <strong>{{ shortToken }}</strong>
        <small>{{
          isLoggedIn
            ? "已注入 authorization 头"
            : "未登录，仅可调用公开接口"
        }}</small>
      </div>
      <div class="info-card ukiyo-e-digital-card">
        <span class="label">当前用户</span>
        <strong>{{ sharedState.currentUser.value?.nickName || "匿名访客" }}</strong>
        <small>ID：{{ sharedState.currentUser.value?.id ?? "--" }}</small>
      </div>
    </div>
    <div class="notice-bar" :class="sharedState.notice.type">
      <span class="ue-stamp">{{
        sharedState.notice.type === "error"
          ? "警示"
          : sharedState.notice.type === "success"
            ? "已响应"
            : "提示"
      }}</span>
      <span>{{ sharedState.notice.message }}</span>
    </div>
  </section>
</template>
