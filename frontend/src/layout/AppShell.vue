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
        <div class="ue-stamp">浮世绘联调台</div>
        <p>每个页面只处理一个模块，左侧统一负责导航。</p>
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
