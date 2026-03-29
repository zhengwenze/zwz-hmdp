<script setup>
import { computed, onMounted } from "vue";
import { RouterLink, RouterView, useRoute } from "vue-router";
import { appState } from "../stores/appState";
import { sessionState, isAuthenticated, clearSession } from "../stores/session";
import { userApi } from "../services/userApi";

const route = useRoute();

const sessionLabel = computed(
  () =>
    sessionState.currentUser.value?.nickName ||
    (isAuthenticated() ? "已登录" : "未登录"),
);

async function handleLogout() {
  await userApi.logout({ silentError: true });
  clearSession("已退出登录。");
}
const navItems = [
  { label: "首页", to: "/" },
  { label: "登录", to: "/login" },
  { label: "个人", to: "/me" },
  { label: "商铺", to: "/shop" },
  { label: "笔记", to: "/blog" },
  { label: "关注", to: "/follow" },
  { label: "优惠", to: "/voucher" },
  { label: "上传", to: "/upload" },
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
        <span class="ue-stamp">页面路径 {{ route.path }}</span>
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
        <span v-if="isAuthenticated()" class="status-pill success">
          {{ sessionLabel }}
        </span>
        <RouterLink v-else to="/login" class="status-pill muted login-prompt">
          <span class="default-text">未登录</span>
          <span class="hover-text">去登录</span>
        </RouterLink>
        <button
          v-if="isAuthenticated()"
          class="logout-button"
          @click="handleLogout"
        >
          退出登录
        </button>
      </div>
    </header>
    <div class="notice-bar ue-shadow ue-washi" :class="appState.notice.type">
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
    <main class="consumer-main">
      <RouterView />
    </main>
  </div>
</template>