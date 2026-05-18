<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";
import { splitImages, excerpt } from "../utils/view";
import { rememberBlog } from "../stores/historyState";

const props = defineProps({
  blog: {
    type: Object,
    required: true,
  },
});

const cover = computed(() => splitImages(props.blog.images)[0] || "");
const authorName = computed(
  () => props.blog.name || props.blog.nickName || `用户 ${props.blog.userId || ""}`.trim(),
);
const authorInitial = computed(() => authorName.value.slice(0, 1) || "匿");
const summary = computed(() => excerpt(props.blog.content, 52));
const detailPath = computed(() => `/blog/detail/${props.blog.id}`);
</script>

<template>
  <article class="blog-preview-card">
    <RouterLink
      :to="detailPath"
      class="blog-preview-cover-link"
      @click="rememberBlog(blog)"
    >
      <img
        v-if="cover"
        :src="cover"
        :alt="blog.title"
        class="blog-preview-cover"
      />
      <div v-else class="blog-preview-cover blog-preview-cover--empty">
        暂无封面
      </div>
    </RouterLink>

    <div class="blog-preview-body">
      <RouterLink
        :to="detailPath"
        class="blog-preview-title"
        @click="rememberBlog(blog)"
      >
        {{ blog.title || "未命名笔记" }}
      </RouterLink>
      <p class="blog-preview-summary">{{ summary }}</p>

      <div class="blog-preview-meta">
        <div class="blog-preview-author">
          <ElAvatar :size="28" :src="blog.icon || ''">
            {{ authorInitial }}
          </ElAvatar>
          <span>{{ authorName }}</span>
        </div>
        <span class="blog-preview-like">赞 {{ blog.liked ?? 0 }}</span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.blog-preview-card {
  overflow: hidden;
  border: 1px solid rgba(215, 222, 232, 0.92);
  border-radius: 12px;
  background: var(--app-surface);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.07);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.blog-preview-card:hover {
  transform: translateY(-2px);
  border-color: rgba(37, 99, 235, 0.24);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.1);
}

.blog-preview-cover-link {
  display: block;
  background: #eef2f7;
}

.blog-preview-cover {
  display: block;
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
}

.blog-preview-cover--empty {
  display: grid;
  place-items: center;
  color: var(--app-text-muted);
  font-size: 13px;
}

.blog-preview-body {
  display: grid;
  gap: 10px;
  padding: 12px 13px 13px;
}

.blog-preview-title {
  display: -webkit-box;
  overflow: hidden;
  color: var(--app-text);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.blog-preview-summary {
  display: -webkit-box;
  overflow: hidden;
  min-height: 40px;
  margin: 0;
  color: var(--app-text-secondary);
  font-size: 13px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.blog-preview-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.blog-preview-author {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.blog-preview-author span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.blog-preview-like {
  flex: 0 0 auto;
}
</style>
