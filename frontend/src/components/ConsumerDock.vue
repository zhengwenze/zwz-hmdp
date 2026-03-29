<script setup>
import { computed } from "vue";
import { RouterLink, useRoute } from "vue-router";

const route = useRoute();

const items = computed(() => [
  { label: "首页", to: "/" },
  { label: "发笔记", to: "/blog/new" },
  { label: "我的", to: "/me" },
]);
</script>

<template>
  <nav class="consumer-dock ue-shadow ue-washi">
    <RouterLink
      v-for="item in items"
      :key="item.to"
      :to="item.to"
      class="consumer-dock-link"
      :class="{ active: route.path === item.to }"
    >
      <span>{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>
