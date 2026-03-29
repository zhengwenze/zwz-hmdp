<script setup>
import { computed } from "vue";
import { RouterLink, RouterView, useRoute } from "vue-router";
import SidebarNav from "../components/SidebarNav.vue";
import { sidebarModules } from "../config/moduleMeta";
import { sharedState } from "../stores/sharedState";

const route = useRoute();

const authStatus = computed(
  () =>
    sharedState.currentUser.value?.nickName ||
    (sharedState.token.value.trim() ? "本地 token 已保存" : "未登录"),
);

const authRoute = computed(() => ({
  path: "/login",
  query: {
    redirect: route.fullPath || "/",
  },
}));
</script>

<template>
  <div class="shell">
    <aside class="sidebar ue-shadow ue-washi">
      <div class="sidebar-head">
        <div class="ue-stamp">菜单</div>
        <RouterLink :to="authRoute" class="sidebar-auth-link">
          <span>{{ authStatus }}</span>
          <small>验证码登录</small>
        </RouterLink>
      </div>
      <SidebarNav :items="sidebarModules" />
    </aside>

    <main class="main page-main">
      <div class="notice-bar notice-shell" :class="sharedState.notice.type">
        <span class="ue-stamp">{{
          sharedState.notice.type === "error"
            ? "警示"
            : sharedState.notice.type === "success"
              ? "已响应"
              : "提示"
        }}</span>
        <span>{{ sharedState.notice.message }}</span>
      </div>
      <RouterView />
    </main>
  </div>
</template>
