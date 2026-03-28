<script setup>
import { RouterLink } from "vue-router";

defineProps({
  items: {
    type: Array,
    required: true,
  },
});
</script>

<template>
  <nav class="sidebar-nav">
    <RouterLink
      v-for="item in items"
      :key="item.id"
      :to="item.path"
      custom
      v-slot="{ href, navigate, isExactActive }"
    >
      <a
        :href="href"
        class="nav-link"
        :class="{ active: isExactActive }"
        @click="navigate"
      >
        <span>{{ item.label }}</span>
        <small>{{ item.title }}</small>
      </a>
    </RouterLink>
  </nav>
</template>
