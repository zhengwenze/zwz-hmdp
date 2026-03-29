<script setup>
import { RouterView } from "vue-router";
import SidebarNav from "../components/SidebarNav.vue";
import { sidebarModules } from "../config/moduleMeta";
import { sharedState } from "../stores/sharedState";
</script>

<template>
  <div class="shell">
    <aside class="sidebar ue-shadow ue-washi">
      <div class="sidebar-head">
        <div class="ue-stamp">菜单</div>
        <p>登录 / 注册</p>
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
