<script setup>
import { computed, onMounted } from "vue";
import { RouterLink } from "vue-router";
import { moduleMeta } from "../config/moduleMeta";
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
const moduleLinks = computed(() => moduleMeta.filter((item) => item.id !== "hero"));

onMounted(async () => {
  if (!sharedState.shopTypes.value.length) {
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
  }
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
      <div class="info-card ukiyo-e-digital-card">
        <span class="label">分类缓存</span>
        <strong>{{ sharedState.shopTypes.value.length }}</strong>
        <small>供商铺和博客模块复用</small>
      </div>
      <div class="info-card ukiyo-e-digital-card">
        <span class="label">请求日志</span>
        <strong>{{ sharedState.requestLogs.value.length }}</strong>
        <small>最近 40 次调用都会保留在诊断页</small>
      </div>
      <div class="info-card ukiyo-e-digital-card">
        <span class="label">最近响应</span>
        <strong>{{ sharedState.lastResponse.value?.status ?? "--" }}</strong>
        <small>{{ sharedState.lastResponse.value?.endpointKey || "尚无请求" }}</small>
      </div>
      <div class="info-card ukiyo-e-digital-card">
        <span class="label">上传缓存</span>
        <strong>{{ sharedState.uploadedImages.value.length }}</strong>
        <small>最近上传的博客图片会保存在本地</small>
      </div>
    </div>

    <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
      <div class="panel-head">
        <h3>模块入口</h3>
        <span class="status-pill muted">按模块拆页</span>
      </div>
      <div class="module-link-grid">
        <RouterLink
          v-for="item in moduleLinks"
          :key="item.id"
          :to="item.path"
          class="module-link-card"
        >
          <strong>{{ item.label }}</strong>
          <p>{{ item.description }}</p>
        </RouterLink>
      </div>
    </article>
  </section>
</template>
