<script setup>
import { computed, onBeforeUnmount, onMounted, watch } from "vue";
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
const noticeIcon = computed(() => {
  const icons = {
    success: "✓",
    error: "×",
    warning: "!",
    info: "i",
  };

  return icons[appState.notice.type] || icons.info;
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

function clearNotice() {
  appState.notice.message = "";
}

let noticeTimer = null;

function clearNoticeTimer() {
  if (noticeTimer) {
    clearTimeout(noticeTimer);
    noticeTimer = null;
  }
}

watch(
  () => route.meta.title,
  (title) => {
    document.title = title ? `${title} - HMDP` : "HMDP";
  },
  { immediate: true },
);

watch(
  () => [appState.notice.message, appState.notice.type, appState.notice.version],
  ([message, type]) => {
    clearNoticeTimer();

    if (!message) {
      return;
    }

    const durationMap = {
      success: 2500,
      error: 4000,
      warning: 3500,
      info: 3000,
    };

    noticeTimer = setTimeout(() => {
      clearNotice();
    }, durationMap[type] || durationMap.info);
  },
  { immediate: true },
);

onMounted(syncCurrentUser);
onBeforeUnmount(clearNoticeTimer);
</script>

<template>
  <ElContainer class="app-layout">
    <Transition name="top-toast">
      <div
        v-if="appState.notice.message"
        class="app-toast"
        :class="`app-toast--${appState.notice.type || 'info'}`"
        role="status"
        aria-live="polite"
        @click="clearNotice"
      >
        <span class="app-toast__icon">{{ noticeIcon }}</span>
        <span class="app-toast__message">{{ appState.notice.message }}</span>
        <span class="app-toast__close">×</span>
      </div>
    </Transition>

    <ElAside width="140px" class="app-sidebar">
      <div class="app-sidebar__brand">
        <!-- 品牌logo -->
        <strong>ZWZ-HMDP</strong>
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
            退出
          </ElButton>
        </div>
      </ElHeader>

      <ElMain class="app-main">
        <div class="app-main__inner">
          <router-view />
        </div>
      </ElMain>
    </ElContainer>
  </ElContainer>
</template>

<style scoped>
.app-toast {
  position: fixed;
  top: max(16px, env(safe-area-inset-top));
  left: 50%;
  z-index: 9999;

  display: flex;
  align-items: center;
  gap: 8px;

  width: fit-content;
  min-width: 180px;
  max-width: 420px;
  padding: 12px 18px;

  border-radius: 8px;
  border-left: 3px solid #909399;
  background: #f4f4f5;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);

  color: #303133;
  font-size: 14px;
  line-height: 1.4;

  cursor: pointer;
  transform: translateX(-50%);
}

.app-toast__icon {
  flex: 0 0 auto;
  font-weight: 700;
}

.app-toast__message {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-toast__close {
  flex: 0 0 auto;
  margin-left: 4px;
  color: #909399;
  font-size: 16px;
  line-height: 1;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.app-toast:hover .app-toast__close {
  opacity: 1;
}

.app-toast--success {
  border-left-color: #67c23a;
  background: #f0f9eb;
}

.app-toast--error {
  border-left-color: #f56c6c;
  background: #fef0f0;
}

.app-toast--warning {
  border-left-color: #e6a23c;
  background: #fdf6ec;
}

.app-toast--info {
  border-left-color: #909399;
  background: #f4f4f5;
}

.top-toast-enter-active {
  transition: opacity 0.3s ease-out, transform 0.3s ease-out;
}

.top-toast-leave-active {
  transition: opacity 0.25s ease-in, transform 0.25s ease-in;
}

.top-toast-enter-from,
.top-toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -120%);
}

.top-toast-enter-to,
.top-toast-leave-from {
  opacity: 1;
  transform: translate(-50%, 0);
}

@media (max-width: 480px) {
  .app-toast {
    width: calc(100vw - 32px);
    max-width: none;
  }
}
</style>
