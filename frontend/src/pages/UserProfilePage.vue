<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import { useRoute, useRouter } from "vue-router";
import { sessionState, isAuthenticated } from "../stores/session";
import { userApi } from "../services/userApi";
import { blogApi } from "../services/blogApi";
import { followApi } from "../services/followApi";
import BlogPreviewCard from "../components/BlogPreviewCard.vue";

const route = useRoute();
const router = useRouter();

const userSummary = ref(null);
const userInfo = ref(null);
const commonFollows = ref([]);
const followed = ref(false);
const activeTab = ref("notes");

const isSelf = computed(() => sessionState.currentUser.value?.id === userSummary.value?.id);

async function loadProfile() {
  const userId = route.params.id;

  const [summaryResult, infoResult, blogsResult] = await Promise.all([
    userApi.fetchUserSummary(userId, { silentError: true }),
    userApi.fetchUserInfo(userId, { silentError: true }),
    blogApi.fetchByUser(userId, 1, {
      silentError: true,
    }),
  ]);

  if (summaryResult.success) {
    userSummary.value = summaryResult.data || null;
  }
  if (infoResult.success) {
    userInfo.value = infoResult.data || null;
  }
  if (blogsResult.success) {
    const rows = Array.isArray(blogsResult.data) ? blogsResult.data : [];
    profileBlogs.value = rows;
  }

  if (isAuthenticated() && !isSelf.value) {
    const followResult = await followApi.check(userId, { silentError: true });
    if (followResult.success) {
      followed.value = Boolean(followResult.data);
    }
  }
}

const profileBlogs = ref([]);

async function loadCommonFollows() {
  const { data, success } = await followApi.common(route.params.id, { silentError: true });
  if (success) {
    commonFollows.value = Array.isArray(data) ? data : [];
  }
}

async function toggleFollow() {
  if (!isAuthenticated()) {
    router.push({
      path: "/login",
      query: { redirect: route.fullPath },
    });
    return;
  }
  const { success } = await followApi.toggle(route.params.id, !followed.value, {
    successMessage: followed.value ? "已取消关注。" : "已关注对方。",
  });
  if (success) {
    followed.value = !followed.value;
  }
}

watch(() => route.params.id, loadProfile);
onMounted(loadProfile);
</script>

<template>
  <section class="consumer-page">
    <article class="panel ue-washi ue-shadow">
      <div class="panel-head">
        <h2>{{ userSummary?.nickName || "用户主页" }}</h2>
        <button v-if="!isSelf" class="accent" @click="toggleFollow">
          {{ followed ? "取消关注" : "关注 TA" }}
        </button>
      </div>

      <div class="shop-detail-meta">
        <div>
          <span class="label">简介</span>
          <strong>{{ userInfo?.introduce || "这个人很懒，什么都没有留下。" }}</strong>
        </div>
        <div>
          <span class="label">粉丝</span>
          <strong>{{ userInfo?.fans ?? 0 }}</strong>
        </div>
        <div>
          <span class="label">关注</span>
          <strong>{{ userInfo?.followee ?? 0 }}</strong>
        </div>
      </div>
    </article>

    <article class="panel ue-washi ue-shadow">
      <div class="consumer-tab-strip">
        <button
          :class="activeTab === 'notes' ? 'accent' : 'secondary'"
          @click="activeTab = 'notes'"
        >
          TA 的笔记
        </button>
        <button
          :class="activeTab === 'common' ? 'accent' : 'secondary'"
          @click="activeTab = 'common'; loadCommonFollows()"
        >
          共同关注
        </button>
      </div>

      <div v-if="activeTab === 'notes'" class="blog-feed-grid">
        <BlogPreviewCard
          v-for="blog in profileBlogs"
          :key="blog.id"
          :blog="blog"
          :show-author="false"
        />
      </div>

      <div v-else class="simple-list">
        <div
          v-for="user in commonFollows"
          :key="user.id"
          class="consumer-list-button panel ue-washi"
        >
          <strong>{{ user.nickName }}</strong>
          <RouterLink :to="`/user/${user.id}`" class="link-button secondary-link">
            去主页看看
          </RouterLink>
        </div>
        <span v-if="!commonFollows.length" class="helper">暂无共同关注。</span>
      </div>
    </article>
  </section>
</template>
