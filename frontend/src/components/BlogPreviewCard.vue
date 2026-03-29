<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";
import { splitImages, formatDateTime, excerpt } from "../utils/view";
import { rememberBlog } from "../stores/historyState";

const props = defineProps({
  blog: {
    type: Object,
    required: true,
  },
  showAuthor: {
    type: Boolean,
    default: true,
  },
});

const cover = computed(() => splitImages(props.blog.images)[0] || "");
const summary = computed(() => excerpt(props.blog.content));
</script>

<template>
  <article class="blog-preview-card panel ue-washi ue-shadow">
    <RouterLink
      :to="`/blog/${blog.id}`"
      class="blog-preview-media-link"
      @click="rememberBlog(blog)"
    >
      <img
        v-if="cover"
        :src="cover"
        :alt="blog.title"
        class="blog-preview-cover"
      />
      <div v-else class="blog-preview-cover empty">浮世无图</div>
    </RouterLink>
    <div class="blog-preview-content">
      <div class="blog-preview-meta">
        <span class="eyebrow">Hot Note</span>
        <span>{{ formatDateTime(blog.createTime) }}</span>
      </div>
      <RouterLink
        :to="`/blog/${blog.id}`"
        class="blog-preview-title-link"
        @click="rememberBlog(blog)"
      >
        <h3>{{ blog.title }}</h3>
      </RouterLink>
      <p class="blog-preview-text">{{ summary }}</p>
      <div class="blog-preview-foot">
        <span v-if="showAuthor">{{ blog.name || "匿名作者" }}</span>
        <span>点赞 {{ blog.liked ?? 0 }}</span>
        <span>评论 {{ blog.comments ?? 0 }}</span>
      </div>
      <div class="button-row tight">
        <RouterLink
          :to="`/blog/${blog.id}`"
          class="link-button secondary-link"
          @click="rememberBlog(blog)"
        >
          查看详情
        </RouterLink>
        <RouterLink
          v-if="blog.shopId"
          :to="`/shop/${blog.shopId}`"
          class="link-button"
          @click="rememberBlog(blog)"
        >
          看店铺
        </RouterLink>
      </div>
    </div>
  </article>
</template>
