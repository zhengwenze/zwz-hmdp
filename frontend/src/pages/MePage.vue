<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { sessionState, clearSession } from "../stores/session";
import { blogFlowState } from "../stores/blogFlow";
import { userApi } from "../services/userApi";
import { blogApi } from "../services/blogApi";
import { formatDateTime } from "../utils/view";
import BlogPreviewCard from "../components/BlogPreviewCard.vue";

const router = useRouter();
const activeTab = ref("notes");
const userInfo = ref(null);
const signCount = ref("--");

const followFeedCards = computed(() => blogFlowState.followFeed.value);

async function loadMe() {
  const meResult = await userApi.fetchMe({ silentError: true });
  if (meResult.success) {
    sessionState.currentUser.value = meResult.data || null;
  }

  if (!sessionState.currentUser.value?.id) {
    return;
  }

  await Promise.all([
    userApi.fetchUserInfo(sessionState.currentUser.value.id, {
      silentError: true,
      onSuccess: (data) => {
        userInfo.value = data || null;
      },
    }),
    blogApi.fetchMy(1, {
      silentError: true,
      onSuccess: (data) => {
        blogFlowState.myBlogs.value = Array.isArray(data) ? data : [];
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

async function loadFollowFeed(reset = false) {
  if (reset) {
    blogFlowState.followFeed.value = [];
    blogFlowState.followCursor.minTime = String(Date.now());
    blogFlowState.followCursor.offset = 0;
    blogFlowState.followCursor.hasMore = true;
  }

  const { data, success } = await blogApi.fetchFollowFeed(
    blogFlowState.followCursor.minTime,
    blogFlowState.followCursor.offset,
    { silentError: true },
  );

  if (!success || !data?.list?.length) {
    blogFlowState.followCursor.hasMore = false;
    return;
  }

  blogFlowState.followFeed.value = [...blogFlowState.followFeed.value, ...data.list];
  blogFlowState.followCursor.minTime = String(data.minTime);
  blogFlowState.followCursor.offset = Number(data.offset || 0);
}

async function signToday() {
  await userApi.sign({ successMessage: "签到成功。" });
  await loadMe();
}

function handleLogout() {
  clearSession("本地登录态已清空。");
  router.push("/");
}

onMounted(loadMe);
</script>

<template>
  <section class="consumer-page">
    <article class="panel ue-washi ue-shadow">
      <div class="panel-head">
        <h2>{{ sessionState.currentUser.value?.nickName || "我的主页" }}</h2>
        <button class="secondary" @click="handleLogout">退出本地登录</button>
      </div>

      <div class="shop-detail-meta">
        <div>
          <span class="label">简介</span>
          <strong>{{ userInfo?.introduce || "这个人很懒，还没有留下简介。" }}</strong>
        </div>
        <div>
          <span class="label">粉丝</span>
          <strong>{{ userInfo?.fans ?? 0 }}</strong>
        </div>
        <div>
          <span class="label">关注</span>
          <strong>{{ userInfo?.followee ?? 0 }}</strong>
        </div>
        <div>
          <span class="label">连续签到</span>
          <strong>{{ signCount }}</strong>
        </div>
      </div>

      <div class="button-row wrap">
        <button @click="signToday">今日签到</button>
        <RouterLink to="/blog/new" class="link-button">去发笔记</RouterLink>
      </div>
    </article>

    <article class="panel ue-washi ue-shadow">
      <div class="consumer-tab-strip">
        <button
          :class="activeTab === 'notes' ? 'accent' : 'secondary'"
          @click="activeTab = 'notes'"
        >
          我的笔记
        </button>
        <button
          :class="activeTab === 'feed' ? 'accent' : 'secondary'"
          @click="activeTab = 'feed'; loadFollowFeed(true)"
        >
          关注动态
        </button>
      </div>

      <div v-if="activeTab === 'notes'" class="blog-feed-grid">
        <BlogPreviewCard
          v-for="blog in blogFlowState.myBlogs.value"
          :key="blog.id"
          :blog="blog"
          :show-author="false"
        />
      </div>

      <div v-else class="blog-feed-grid">
        <BlogPreviewCard
          v-for="blog in followFeedCards"
          :key="`${blog.id}-${formatDateTime(blog.createTime)}`"
          :blog="blog"
        />
        <div class="button-row wrap">
          <button
            v-if="blogFlowState.followCursor.hasMore"
            class="secondary"
            @click="loadFollowFeed()"
          >
            继续拉取关注流
          </button>
          <span v-else class="helper">关注流已经到底了。</span>
        </div>
      </div>
    </article>
  </section>
</template>
