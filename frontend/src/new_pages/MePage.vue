<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { userApi } from "../services/userApi";
import {
  buildRedirectPath,
  clearSession,
  isAuthenticated,
  sessionState,
} from "../stores/session";

const router = useRouter();
const userInfo = ref(null);
const signCount = ref("--");
const nicknameDialogVisible = ref(false);
const newNickname = ref("");

const userId = computed(() => sessionState.currentUser.value?.id);

async function loadMe() {
  if (!isAuthenticated()) {
    userInfo.value = null;
    signCount.value = "--";
    sessionState.currentUser.value = null;
    return;
  }

  const meResult = await userApi.fetchMe({ silentError: true });
  if (!meResult.success || !meResult.data?.id) {
    return;
  }

  sessionState.currentUser.value = meResult.data;

  await Promise.all([
    userApi.fetchUserInfo(meResult.data.id, {
      silentError: true,
      onSuccess: (data) => {
        userInfo.value = data || null;
      },
    }),
    userApi.fetchSignCount({
      silentError: true,
      onSuccess: (data) => {
        signCount.value = data ?? "--";
      },
    }),
  ]);
}

function jumpToLogin() {
  router.push(buildRedirectPath("/me"));
}

async function signToday() {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }
  const { success } = await userApi.sign({ successMessage: "签到成功。" });
  if (success) {
    await loadMe();
  }
}

function openNicknameEditor() {
  newNickname.value = sessionState.currentUser.value?.nickName || "";
  nicknameDialogVisible.value = true;
}

async function submitNicknameUpdate() {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }
  if (!newNickname.value.trim()) {
    return;
  }

  const { success } = await userApi.updateNickName(newNickname.value.trim(), {
    successMessage: "昵称修改成功。",
  });
  if (success) {
    sessionState.currentUser.value = {
      ...sessionState.currentUser.value,
      nickName: newNickname.value.trim(),
    };
    nicknameDialogVisible.value = false;
    await loadMe();
  }
}

async function handleLogout() {
  await userApi.logout({ silentError: true });
  clearSession("已退出登录。");
  userInfo.value = null;
  signCount.value = "--";
  router.push("/login");
}

onMounted(loadMe);
</script>

<template>
  <section class="app-page">
    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h2 class="page-panel__title">当前用户</h2>
            <p class="page-panel__hint">
              用户模块只负责当前会话、签到、昵称修改和退出登录。
            </p>
          </div>
          <div class="page-actions">
            <ElButton @click="loadMe">刷新</ElButton>
            <ElButton v-if="!isAuthenticated()" type="primary" @click="jumpToLogin">
              去登录
            </ElButton>
            <template v-else>
              <ElButton type="primary" @click="signToday">今日签到</ElButton>
              <ElButton type="info" plain @click="openNicknameEditor">
                修改昵称
              </ElButton>
              <ElButton type="info" plain @click="handleLogout">退出登录</ElButton>
            </template>
          </div>
        </div>
      </template>

      <ElEmpty
        v-if="!isAuthenticated()"
        description="当前未登录，请先登录后再查看用户信息。"
      />

      <ElDescriptions v-else :column="2" border>
        <ElDescriptionsItem label="用户 ID">
          {{ userId ?? "--" }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="昵称">
          {{ sessionState.currentUser.value?.nickName || "--" }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="手机号">
          {{ sessionState.currentUser.value?.phone || "--" }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="连续签到">
          {{ signCount }}
        </ElDescriptionsItem>
      </ElDescriptions>
    </ElCard>

    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h3 class="page-panel__title">用户详情</h3>
            <p class="page-panel__hint">来自 `/user/info/{id}` 的扩展信息。</p>
          </div>
        </div>
      </template>

      <ElDescriptions v-if="userInfo" :column="2" border>
        <ElDescriptionsItem label="简介">
          {{ userInfo?.introduce || "暂无简介" }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="性别">
          {{ userInfo?.gender ?? "未填写" }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="粉丝">
          {{ userInfo?.fans ?? 0 }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="关注">
          {{ userInfo?.followee ?? 0 }}
        </ElDescriptionsItem>
      </ElDescriptions>
      <ElEmpty v-else description="暂无用户详情数据。" />
    </ElCard>

    <ElDialog
      v-model="nicknameDialogVisible"
      title="修改昵称"
      width="420px"
    >
      <ElForm label-position="top">
        <ElFormItem label="新昵称">
          <ElInput
            v-model="newNickname"
            maxlength="32"
            placeholder="请输入新昵称"
            @keyup.enter="submitNicknameUpdate"
          />
        </ElFormItem>
      </ElForm>

      <template #footer>
        <div class="page-actions">
          <ElButton @click="nicknameDialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="submitNicknameUpdate">
            保存昵称
          </ElButton>
        </div>
      </template>
    </ElDialog>
  </section>
</template>
