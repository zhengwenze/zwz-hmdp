<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";
import { splitImages, formatDateTime } from "../utils/view";

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
</script>

<template>
  <RouterLink :to="`/blog/${blog.id}`" class="blog-preview-card panel ue-washi ue-shadow">
    <img v-if="cover" :src="cover" :alt="blog.title" class="blog-preview-cover" />
    <div class="blog-preview-content">
      <div class="blog-preview-meta">
        <span class="eyebrow">Hot Note</span>
        <span>{{ formatDateTime(blog.createTime) }}</span>
      </div>
      <h3>{{ blog.title }}</h3>
      <p class="blog-preview-text">{{ blog.content }}</p>
      <div class="blog-preview-foot">
        <span v-if="showAuthor">{{ blog.name || "匿名作者" }}</span>
        <span>点赞 {{ blog.liked ?? 0 }}</span>
        <span>评论 {{ blog.comments ?? 0 }}</span>
      </div>
    </div>
  </RouterLink>
</template>
