<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";
import { firstImage, formatDistance, formatPrice } from "../utils/view";
import { rememberShop } from "../stores/historyState";

const props = defineProps({
  shop: {
    type: Object,
    required: true,
  },
});

const cover = computed(() => firstImage(props.shop.images));
</script>

<template>
  <article class="shop-preview-card panel ue-washi ue-shadow">
    <RouterLink :to="`/shop/${shop.id}`" class="shop-preview-media-link" @click="rememberShop(shop)">
      <img v-if="cover" :src="cover" :alt="shop.name" class="shop-preview-cover" />
      <div v-else class="shop-preview-cover empty">店铺无图</div>
    </RouterLink>
    <div class="shop-preview-content">
      <div class="shop-preview-head">
        <RouterLink :to="`/shop/${shop.id}`" class="shop-preview-title-link" @click="rememberShop(shop)">
          <h3>{{ shop.name }}</h3>
        </RouterLink>
        <span class="status-pill muted">评分 {{ ((shop.score || 0) / 10).toFixed(1) }}</span>
      </div>
      <p>{{ shop.area || "未知商圈" }} · {{ shop.openHours || "营业时间待补充" }}</p>
      <p>{{ formatDistance(shop.distance) }} · ￥{{ formatPrice(shop.avgPrice) }}/人</p>
      <p class="shop-preview-address">{{ shop.address || "暂无地址" }}</p>
      <div class="button-row tight">
        <RouterLink :to="`/shop/${shop.id}`" class="link-button secondary-link" @click="rememberShop(shop)">
          查看详情
        </RouterLink>
      </div>
    </div>
  </article>
</template>
