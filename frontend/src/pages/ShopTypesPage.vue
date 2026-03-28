<script setup>
import {
  sharedState,
  send,
  isLoading,
  endpointBadgeClass,
  toAssetUrl,
} from "../stores/sharedState";

async function fetchShopTypes() {
  await send(
    "GET /shop-type/list",
    { method: "GET", path: "/shop-type/list" },
    {
      successMessage: "分类列表已刷新。",
      onSuccess: (data) => {
        sharedState.shopTypes.value = Array.isArray(data) ? data : [];
      },
    },
  );
}
</script>

<template>
  <section id="shop-types" class="module-section">
    <div class="section-title">
      <div>
        <p class="eyebrow">Shop Type APIs</p>
        <h2>商铺分类</h2>
      </div>
      <span class="section-hint">公开接口，可作为商铺和博客的入口上下文</span>
    </div>

    <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
      <div class="panel-head">
        <h3>分类列表</h3>
        <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /shop-type/list'])">/shop-type/list</span>
      </div>
      <div class="button-row">
        <button :disabled="isLoading('GET /shop-type/list')" @click="fetchShopTypes">刷新分类</button>
      </div>
      <div class="type-grid">
        <button
          v-for="type in sharedState.shopTypes.value"
          :key="type.id"
          class="type-chip"
        >
          <img v-if="type.icon" :src="toAssetUrl(type.icon)" :alt="type.name" />
          <span>{{ type.name }}</span>
        </button>
      </div>
    </article>
  </section>
</template>
