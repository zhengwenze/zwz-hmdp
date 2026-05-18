<script setup>
import { onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import BlogPreviewCard from "../components/BlogPreviewCard.vue";
import { blogApi } from "../services/blogApi";
import { userApi } from "../services/userApi";
import {
  blogFlowState,
  resetFollowCursor,
} from "../stores/blogFlow";
import { setNotice } from "../stores/appState";
import { buildRedirectPath, isAuthenticated } from "../stores/session";

const router = useRouter();

const activeTab = ref("square");
const squareBlogs = ref([]);
const squarePage = ref(1);
const squareLoading = ref(false);
const followLoading = ref(false);
const followLoaded = ref(false);

function openEditor() {
  router.push("/blog/editor");
}

function jumpToLogin() {
  router.push(buildRedirectPath("/blog"));
}

function normalizeBlogs(value) {
  return Array.isArray(value) ? value.filter((blog) => blog?.id) : [];
}

async function enrichAuthors(blogs) {
  const targets = blogs.filter(
    (blog) => blog?.userId && !blog.name && !blog.nickName,
  );
  const userIds = [...new Set(targets.map((blog) => blog.userId))];

  if (!userIds.length) {
    return blogs;
  }

  const entries = await Promise.all(
    userIds.map(async (userId) => {
      const { data, success } = await userApi.fetchUserSummary(userId, {
        silentError: true,
      });
      return success && data ? [String(userId), data] : null;
    }),
  );
  const userMap = new Map(entries.filter(Boolean));

  return blogs.map((blog) => {
    const user = userMap.get(String(blog.userId));
    return user
      ? {
          ...blog,
          name: blog.name || user.nickName || user.name,
          icon: blog.icon || user.icon,
        }
      : blog;
  });
}

async function fetchSquareBlogs(page = squarePage.value) {
  squareLoading.value = true;
  const { data, success } = await blogApi.fetchHot(page, {
    silentError: true,
  });
  if (success) {
    squareBlogs.value = await enrichAuthors(normalizeBlogs(data));
    squarePage.value = page;
  }
  squareLoading.value = false;
}

async function fetchFollowFeed({ reset = false } = {}) {
  if (!isAuthenticated()) {
    followLoaded.value = true;
    blogFlowState.followFeed.value = [];
    return;
  }

  if (reset) {
    resetFollowCursor();
    blogFlowState.followFeed.value = [];
  }

  if (!blogFlowState.followCursor.hasMore && !reset) {
    return;
  }

  followLoading.value = true;
  const { data, success } = await blogApi.fetchFollowFeed(
    blogFlowState.followCursor.minTime,
    blogFlowState.followCursor.offset,
    { silentError: true },
  );

  if (success) {
    const list = await enrichAuthors(normalizeBlogs(data?.list));
    blogFlowState.followFeed.value = reset
      ? list
      : [...blogFlowState.followFeed.value, ...list];
    blogFlowState.followCursor.minTime = String(data?.minTime || Date.now());
    blogFlowState.followCursor.offset = Number(data?.offset || 0);
    blogFlowState.followCursor.hasMore = list.length > 0;
    followLoaded.value = true;
  }

  followLoading.value = false;
}

function handleTabChange(name) {
  if (name === "publish") {
    activeTab.value = "square";
    openEditor();
    return;
  }

  if (name === "follow") {
    if (!isAuthenticated()) {
      jumpToLogin();
      return;
    }
    fetchFollowFeed({ reset: true });
  }
}

function handleTabClick(pane) {
  handleTabChange(pane?.props?.name || pane?.paneName);
}

async function refreshCurrentTab() {
  if (activeTab.value === "follow") {
    if (!isAuthenticated()) {
      jumpToLogin();
      return;
    }
    await fetchFollowFeed({ reset: true });
    setNotice("success", "关注流已刷新");
    return;
  }

  await fetchSquareBlogs(1);
  setNotice("success", "帖子广场已刷新");
}

function changeSquarePage(page) {
  fetchSquareBlogs(page);
}

onMounted(() => {
  fetchSquareBlogs(1);
});

watch(activeTab, handleTabChange);
</script>

<template>
  <section class="app-page blog-community-page">
    <div class="community-header">
      <div>
        <h2>社区笔记</h2>
        <p>发现用户分享的探店笔记和生活内容。</p>
      </div>
      <div class="community-header__actions">
        <ElButton @click="refreshCurrentTab">刷新</ElButton>
        <ElButton type="primary" @click="openEditor">发布笔记</ElButton>
      </div>
    </div>

    <ElTabs
      v-model="activeTab"
      class="community-tabs"
      @tab-click="handleTabClick"
    >
      <ElTabPane label="帖子广场" name="square">
        <ElSkeleton v-if="squareLoading && !squareBlogs.length" :rows="6" animated />
        <div v-else-if="squareBlogs.length" class="blog-masonry">
          <BlogPreviewCard
            v-for="blog in squareBlogs"
            :key="blog.id"
            :blog="blog"
          />
        </div>
        <ElEmpty v-else description="暂无帖子" />

        <div class="community-pager">
          <ElPagination
            :current-page="squarePage"
            :page-size="5"
            layout="prev, pager, next"
            :page-count="Math.max(squarePage + (squareBlogs.length === 5 ? 1 : 0), 1)"
            @current-change="changeSquarePage"
          />
        </div>
      </ElTabPane>

      <ElTabPane label="关注" name="follow">
        <ElAlert
          v-if="!isAuthenticated()"
          title="登录后可查看关注用户发布的笔记。"
          type="warning"
          :closable="false"
          show-icon
        />

        <ElSkeleton
          v-if="followLoading && !blogFlowState.followFeed.value.length"
          :rows="6"
          animated
        />
        <div
          v-else-if="blogFlowState.followFeed.value.length"
          class="blog-masonry"
        >
          <BlogPreviewCard
            v-for="blog in blogFlowState.followFeed.value"
            :key="blog.id"
            :blog="blog"
          />
        </div>
        <ElEmpty
          v-else
          description="暂无关注用户的新笔记"
        />

        <div v-if="isAuthenticated()" class="community-pager">
          <ElButton
            :loading="followLoading"
            :disabled="!blogFlowState.followCursor.hasMore"
            @click="fetchFollowFeed()"
          >
            {{ blogFlowState.followCursor.hasMore ? "加载更多" : "没有更多了" }}
          </ElButton>
        </div>
      </ElTabPane>

      <ElTabPane label="发布笔记" name="publish" />
    </ElTabs>
  </section>
</template>

<style scoped>
.blog-community-page {
  max-width: 1180px;
  width: 100%;
  margin: 0 auto;
}

.community-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 18px;
  padding: 8px 2px 4px;
}

.community-header h2 {
  margin: 0;
  font-size: 26px;
  line-height: 1.25;
}

.community-header p {
  margin: 8px 0 0;
  color: var(--app-text-muted);
  line-height: 1.6;
}

.community-header__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.community-tabs {
  padding: 16px 18px 20px;
  border: 1px solid var(--app-border);
  border-radius: 12px;
  background: var(--app-surface);
  box-shadow: var(--app-shadow-sm);
}

.blog-masonry {
  column-count: 3;
  column-gap: 18px;
}

.blog-masonry :deep(.blog-preview-card) {
  display: inline-block;
  width: 100%;
  margin: 0 0 18px;
  break-inside: avoid;
}

.community-pager {
  display: flex;
  justify-content: center;
  padding-top: 18px;
}

@media (max-width: 1100px) {
  .blog-masonry {
    column-count: 2;
  }
}

@media (max-width: 720px) {
  .community-header {
    align-items: stretch;
    flex-direction: column;
  }

  .community-header__actions {
    justify-content: flex-start;
  }

  .community-tabs {
    padding: 12px;
  }

  .blog-masonry {
    column-count: 1;
  }
}
</style>
