<script setup>
import { computed, onMounted } from "vue";
import { RouterLink, RouterView, useRoute } from "vue-router";
import { appState } from "../stores/appState";
import { sessionState, isAuthenticated } from "../stores/session";
import { homeFeedState } from "../stores/homeFeed";
import { blogFlowState } from "../stores/blogFlow";
import { userApi } from "../services/userApi";
import ConsumerDock from "../components/ConsumerDock.vue";

const route = useRoute();

const showDock = computed(() => route.meta.showDock !== false);
const sessionLabel = computed(() =>
  sessionState.currentUser.value?.nickName || (isAuthenticated() ? "已登录" : "未登录"),
);

onMounted(async () => {
  if (isAuthenticated() && !sessionState.currentUser.value) {
    const { data, success } = await userApi.fetchMe({ silentError: true });
    if (success) {
      sessionState.currentUser.value = data || null;
    }
  }
});
</script>

<template>
  <div class="consumer-shell">
    <header class="consumer-topbar ue-shadow ue-washi">
      <RouterLink to="/" class="consumer-brand">
        <span class="ue-stamp">江户评</span>
        <div>
          <strong>黑马点评</strong>
          <small>Ukiyo-e Digital</small>
        </div>
      </RouterLink>
      <div class="consumer-topbar-meta">
        <span class="status-pill" :class="isAuthenticated() ? 'success' : 'muted'">
          {{ sessionLabel }}
        </span>
        <RouterLink to="/lab" class="consumer-lab-link">秘藏工房</RouterLink>
      </div>
    </header>

    <div class="notice-bar notice-shell" :class="appState.notice.type">
      <span class="ue-stamp">
        {{
          appState.notice.type === "error"
            ? "警示"
            : appState.notice.type === "success"
              ? "已响应"
              : "提示"
        }}
      </span>
      <span>{{ appState.notice.message }}</span>
    </div>

    <div class="consumer-stage">
      <main class="consumer-main">
        <RouterView />
      </main>

      <aside class="consumer-rail panel ue-washi ue-shadow">
        <div class="panel-head">
          <h3>浮世绘导览</h3>
          <span class="status-pill muted">桌面优先</span>
        </div>
        <div class="consumer-rail-block">
          <span class="label">当前会话</span>
          <strong>{{ sessionLabel }}</strong>
          <small>{{ isAuthenticated() ? "authorization 已自动注入" : "未登录时仅可访问公开链路" }}</small>
        </div>
        <div class="consumer-rail-block">
          <span class="label">首页状态</span>
          <strong>{{ homeFeedState.shopTypes.value.length }} 个分类</strong>
          <small>热门笔记 {{ homeFeedState.hotBlogs.value.length }} 条</small>
        </div>
        <div class="consumer-rail-block">
          <span class="label">发布缓存</span>
          <strong>{{ blogFlowState.uploadedImages.value.length }} 张</strong>
          <small>最近上传的图片保存在本地草稿中</small>
        </div>
        <RouterLink
          v-if="!isAuthenticated()"
          :to="{ path: '/login', query: { redirect: route.fullPath || '/' } }"
          class="link-button"
        >
          登录后体验完整链路
        </RouterLink>
      </aside>
    </div>

    <ConsumerDock v-if="showDock" />
  </div>
</template>
