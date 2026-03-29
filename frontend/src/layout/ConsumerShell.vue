<script setup>
import { computed, onMounted } from "vue";
import { RouterLink, RouterView, useRoute } from "vue-router";
import { appState } from "../stores/appState";
import { sessionState, isAuthenticated } from "../stores/session";
import { userApi } from "../services/userApi";
import ConsumerDock from "../components/ConsumerDock.vue";

const route = useRoute();

const showDock = computed(() => route.meta.showDock !== false);
const sessionLabel = computed(
  () =>
    sessionState.currentUser.value?.nickName ||
    (isAuthenticated() ? "已登录" : "未登录"),
);
const navItems = [
  { label: "首页", to: "/" },
  { label: "发笔记", to: "/blog/new" },
  { label: "我的", to: "/me" },
];

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
      <nav class="consumer-topnav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="consumer-topnav-link"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="consumer-topbar-meta">
        <span
          class="status-pill"
          :class="isAuthenticated() ? 'success' : 'muted'"
        >
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
    </div>

    <ConsumerDock v-if="showDock" />
  </div>
</template>
