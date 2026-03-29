<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { followApi } from "../services/followApi";
import { blogApi } from "../services/blogApi";
import { buildRedirectPath, isAuthenticated } from "../stores/session";

const router = useRouter();
const forms = reactive({
  targetUserId: "1",
  checkUserId: "1",
  commonUserId: "1",
  lastId: String(Date.now()),
  offset: "0",
});

const followStatus = ref(null);
const commonFollows = ref([]);
const followFeed = ref([]);

function jumpToLogin() {
  router.push(buildRedirectPath("/follow"));
}

async function updateFollowState(isFollow) {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }
  await followApi.toggle(forms.targetUserId, isFollow, {
    successMessage: isFollow ? "已关注。" : "已取消关注。",
  });
}

async function checkFollowStatus() {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }
  const { data, success } = await followApi.check(forms.checkUserId, {
    silentError: true,
  });
  if (success) {
    followStatus.value = Boolean(data);
  }
}

async function fetchCommonFollows() {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }
  const { data, success } = await followApi.common(forms.commonUserId, {
    silentError: true,
  });
  if (success) {
    commonFollows.value = Array.isArray(data) ? data : [];
  }
}

async function fetchFollowFeed() {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }
  const { data, success } = await blogApi.fetchFollowFeed(
    forms.lastId,
    forms.offset,
    {
      silentError: true,
    },
  );
  if (success) {
    followFeed.value = Array.isArray(data?.list) ? data.list : [];
    forms.lastId = String(data?.minTime || forms.lastId);
    forms.offset = String(data?.offset ?? forms.offset);
  }
}
</script>

<template>
  <section class="app-page">
    <ElAlert
      :title="isAuthenticated() ? '当前已登录，可直接调用关注相关接口。' : '关注相关接口都需要登录，请先去登录页。'"
      :type="isAuthenticated() ? 'success' : 'warning'"
      :closable="false"
      show-icon
    />

    <div class="page-grid-3">
      <ElCard class="page-panel">
        <template #header>
          <div class="page-panel__header">
            <div>
              <h3 class="page-panel__title">关注 / 取关 / 状态</h3>
              <p class="page-panel__hint">关注关系操作统一收口到一张卡片。</p>
            </div>
          </div>
        </template>

        <ElForm label-position="top">
          <ElFormItem label="目标用户 ID">
            <ElInput v-model="forms.targetUserId" />
          </ElFormItem>
          <ElFormItem label="查询状态用户 ID">
            <ElInput v-model="forms.checkUserId" />
          </ElFormItem>
        </ElForm>

        <div class="filter-actions">
          <ElButton type="primary" @click="updateFollowState(true)">关注</ElButton>
          <ElButton @click="updateFollowState(false)">取关</ElButton>
          <ElButton type="info" plain @click="checkFollowStatus">查询状态</ElButton>
        </div>

        <ElDescriptions :column="1" border>
          <ElDescriptionsItem label="关注状态">
            {{
              followStatus === null ? "--" : followStatus ? "已关注" : "未关注"
            }}
          </ElDescriptionsItem>
        </ElDescriptions>
      </ElCard>

      <ElCard class="page-panel">
        <template #header>
          <div class="page-panel__header">
            <div>
              <h3 class="page-panel__title">共同关注</h3>
              <p class="page-panel__hint">用表格展示共同关注用户列表。</p>
            </div>
          </div>
        </template>

        <ElForm label-position="top">
          <ElFormItem label="对方用户 ID">
            <ElInput v-model="forms.commonUserId" />
          </ElFormItem>
        </ElForm>

        <div class="filter-actions">
          <ElButton type="primary" @click="fetchCommonFollows">查询共同关注</ElButton>
        </div>

        <ElTable :data="commonFollows" border stripe>
          <ElTableColumn prop="id" label="用户 ID" width="100" />
          <ElTableColumn prop="nickName" label="昵称" min-width="180" />
          <template #empty>
            <ElEmpty description="暂无共同关注数据" />
          </template>
        </ElTable>
      </ElCard>

      <ElCard class="page-panel">
        <template #header>
          <div class="page-panel__header">
            <div>
              <h3 class="page-panel__title">关注流</h3>
              <p class="page-panel__hint">保留游标查询，不伪造页码分页。</p>
            </div>
          </div>
        </template>

        <ElForm label-position="top">
          <ElFormItem label="lastId">
            <ElInput v-model="forms.lastId" />
          </ElFormItem>
          <ElFormItem label="offset">
            <ElInput v-model="forms.offset" />
          </ElFormItem>
        </ElForm>

        <div class="filter-actions">
          <ElButton type="primary" @click="fetchFollowFeed">拉取关注流</ElButton>
        </div>

        <ElTable :data="followFeed" border stripe>
          <ElTableColumn prop="id" label="笔记 ID" width="96" />
          <ElTableColumn prop="title" label="标题" min-width="180" />
          <ElTableColumn label="作者" min-width="140">
            <template #default="{ row }">
              {{ row.name || `用户 ${row.userId}` }}
            </template>
          </ElTableColumn>
          <ElTableColumn prop="liked" label="点赞数" width="100" />
          <template #empty>
            <ElEmpty description="暂无关注流数据" />
          </template>
        </ElTable>
      </ElCard>
    </div>
  </section>
</template>
