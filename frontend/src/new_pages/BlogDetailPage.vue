<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { blogApi } from "../services/blogApi";
import { followApi } from "../services/followApi";
import { buildRedirectPath, isAuthenticated, sessionState } from "../stores/session";
import { rememberBlog } from "../stores/historyState";
import {
  formatDateTime,
  renderRichText,
  splitImages,
} from "../utils/view";

const route = useRoute();
const router = useRouter();

const blog = ref(null);
const isLoading = ref(false);
const isFollowed = ref(false);
const followLoading = ref(false);

const images = computed(() => splitImages(blog.value?.images));
const authorName = computed(
  () => blog.value?.name || blog.value?.nickName || `用户 ${blog.value?.userId || ""}`.trim(),
);
const authorInitial = computed(() => authorName.value.slice(0, 1) || "匿");
const isSelf = computed(
  () =>
    blog.value?.userId &&
    sessionState.currentUser.value?.id &&
    String(blog.value.userId) === String(sessionState.currentUser.value.id),
);

function jumpToLogin() {
  router.push(buildRedirectPath(route.fullPath || "/blog"));
}

async function loadFollowState() {
  if (!isAuthenticated() || !blog.value?.userId || isSelf.value) {
    isFollowed.value = false;
    return;
  }

  const { data, success } = await followApi.check(blog.value.userId, {
    silentError: true,
  });
  if (success) {
    isFollowed.value = Boolean(data);
  }
}

async function loadBlogDetail() {
  const blogId = route.params.id;
  if (!blogId) {
    return;
  }

  isLoading.value = true;
  const { data, success } = await blogApi.fetchDetail(blogId, {
    silentError: true,
  });
  if (success) {
    blog.value = data || null;
    rememberBlog(data);
    await loadFollowState();
  } else {
    blog.value = null;
  }
  isLoading.value = false;
}

async function toggleFollow() {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }
  if (!blog.value?.userId || isSelf.value) {
    return;
  }

  const nextState = !isFollowed.value;
  followLoading.value = true;
  const { success } = await followApi.toggle(blog.value.userId, nextState, {
    successMessage: nextState ? "已关注" : "已取消关注",
  });
  if (success) {
    isFollowed.value = nextState;
  }
  followLoading.value = false;
}

function openShop() {
  if (blog.value?.shopId) {
    router.push(`/shop/detail/${blog.value.shopId}`);
  }
}

watch(() => route.params.id, loadBlogDetail);
onMounted(loadBlogDetail);
</script>

<template>
  <section class="app-page blog-detail-page">
    <div class="detail-nav">
      <ElButton @click="router.push('/blog')">返回广场</ElButton>
      <ElButton type="primary" plain @click="router.push('/blog/editor')">
        发布笔记
      </ElButton>
    </div>

    <ElSkeleton v-if="isLoading && !blog" :rows="10" animated />
    <ElEmpty v-else-if="!blog" description="笔记不存在或暂时无法访问" />

    <article v-else class="blog-detail-shell">
      <div class="blog-detail-media">
        <ElCarousel
          v-if="images.length"
          trigger="click"
          height="520px"
          indicator-position="outside"
        >
          <ElCarouselItem v-for="image in images" :key="image">
            <img :src="image" :alt="blog.title" class="blog-detail-image" />
          </ElCarouselItem>
        </ElCarousel>
        <div v-else class="blog-detail-image blog-detail-image--empty">
          暂无图片
        </div>
      </div>

      <aside class="blog-detail-content">
        <div class="blog-detail-author">
          <ElAvatar :size="42" :src="blog.icon || ''">
            {{ authorInitial }}
          </ElAvatar>
          <div>
            <strong>{{ authorName }}</strong>
            <span>{{ formatDateTime(blog.createTime) }}</span>
          </div>
          <ElButton
            v-if="!isSelf"
            class="follow-button"
            :type="isFollowed ? 'default' : 'primary'"
            :loading="followLoading"
            @click="toggleFollow"
          >
            {{ isFollowed ? "已关注" : "关注" }}
          </ElButton>
        </div>

        <h1>{{ blog.title || "未命名笔记" }}</h1>
        <div class="blog-detail-copy" v-html="renderRichText(blog.content)" />

        <div class="blog-detail-actions">
          <ElTag effect="plain">点赞 {{ blog.liked ?? 0 }}</ElTag>
          <ElButton
            v-if="blog.shopId"
            type="primary"
            plain
            @click="openShop"
          >
            查看关联店铺
          </ElButton>
        </div>
      </aside>
    </article>
  </section>
</template>

<style scoped>
.blog-detail-page {
  max-width: 1180px;
  width: 100%;
  margin: 0 auto;
}

.detail-nav {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.blog-detail-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  overflow: hidden;
  border: 1px solid var(--app-border);
  border-radius: 12px;
  background: var(--app-surface);
  box-shadow: var(--app-shadow-sm);
}

.blog-detail-media {
  min-width: 0;
  background: #eef2f7;
}

.blog-detail-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #f8fafc;
}

.blog-detail-image--empty {
  display: grid;
  min-height: 420px;
  place-items: center;
  color: var(--app-text-muted);
}

.blog-detail-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 24px;
}

.blog-detail-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.blog-detail-author > div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.blog-detail-author strong {
  overflow: hidden;
  color: var(--app-text);
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.blog-detail-author span {
  color: var(--app-text-muted);
  font-size: 12px;
}

.follow-button {
  margin-left: auto;
}

.blog-detail-content h1 {
  margin: 0;
  color: var(--app-text);
  font-size: 26px;
  line-height: 1.35;
}

.blog-detail-copy {
  color: var(--app-text-secondary);
  font-size: 15px;
  line-height: 1.9;
  word-break: break-word;
}

.blog-detail-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid var(--app-border);
}

@media (max-width: 960px) {
  .blog-detail-shell {
    grid-template-columns: 1fr;
  }

  .blog-detail-content {
    padding: 18px;
  }
}

@media (max-width: 640px) {
  .detail-nav {
    flex-direction: column;
  }

  .blog-detail-content h1 {
    font-size: 22px;
  }
}
</style>
