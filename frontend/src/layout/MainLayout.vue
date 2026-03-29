<script setup>
import { computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { appState } from "../stores/appState";
import { clearSession, isAuthenticated, sessionState } from "../stores/session";
import { userApi } from "../services/userApi";

const route = useRoute();
const router = useRouter();

const menuItems = computed(() => {
  const rootRoute = router.options.routes.find((item) => item.path === "/");
  return (rootRoute?.children || [])
    .filter((item) => item.meta?.menu)
    .map((item) => ({
      path: item.path ? `/${item.path}` : "/",
      title: item.meta.title,
    }));
});

const currentTitle = computed(() => route.meta.title || "工作台");
const currentDescription = computed(() => route.meta.description || "");
const noticeTitle = computed(() => {
  if (appState.notice.type === "error") {
    return "错误提示";
  }
  if (appState.notice.type === "success") {
    return "操作成功";
  }
  return "系统提示";
});
const currentUserLabel = computed(
  () =>
    sessionState.currentUser.value?.nickName ||
    (isAuthenticated() ? "已登录" : "未登录"),
);

async function syncCurrentUser() {
  if (!isAuthenticated() || sessionState.currentUser.value) {
    return;
  }
  const { data, success } = await userApi.fetchMe({ silentError: true });
  if (success) {
    sessionState.currentUser.value = data || null;
  }
}

async function handleLogout() {
  await userApi.logout({ silentError: true });
  clearSession("已退出登录。");
  router.push("/login");
}

watch(
  () => route.meta.title,
  (title) => {
    document.title = title ? `${title} - HMDP` : "HMDP";
  },
  { immediate: true },
);

onMounted(syncCurrentUser);
</script>

<template>
  <ElContainer class="app-layout">
    <ElAside width="220px" class="app-sidebar">
      <div class="app-sidebar__brand">
        <span class="app-sidebar__eyebrow">HMDP Console</span>
        <strong>new_pages 工作台</strong>
        <span class="app-sidebar__hint">统一布局、统一结构、统一视觉</span>
      </div>
      <ElScrollbar class="app-sidebar__scroll">
        <ElMenu :default-active="route.path" router class="app-menu">
          <ElMenuItem
            v-for="item in menuItems"
            :key="item.path"
            :index="item.path"
          >
            {{ item.title }}
          </ElMenuItem>
        </ElMenu>
      </ElScrollbar>
    </ElAside>

    <ElContainer>
      <ElHeader class="app-header">
        <div>
          <h1 class="app-header__title">{{ currentTitle }}</h1>
          <p v-if="currentDescription" class="app-header__description">
            {{ currentDescription }}
          </p>
        </div>

        <div class="app-header__actions">
          <ElTag :type="isAuthenticated() ? 'primary' : 'info'" effect="plain">
            {{ currentUserLabel }}
          </ElTag>
          <ElButton v-if="!isAuthenticated()" @click="router.push('/login')">
            去登录
          </ElButton>
          <ElButton v-else type="primary" plain @click="handleLogout">
            退出登录
          </ElButton>
        </div>
      </ElHeader>

      <ElMain class="app-main">
        <div class="app-main__inner">
          <ElAlert
            v-if="appState.notice.message"
            :title="noticeTitle"
            :description="appState.notice.message"
            :type="
              appState.notice.type === 'error'
                ? 'error'
                : appState.notice.type === 'success'
                  ? 'success'
                  : 'info'
            "
            :closable="false"
            show-icon
          />
          <router-view />
        </div>
      </ElMain>
    </ElContainer>
  </ElContainer>
</template>
