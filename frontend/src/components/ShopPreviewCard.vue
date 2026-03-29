<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";
import { firstImage, formatDistance, formatPrice } from "../utils/view";

const props = defineProps({
  shop: {
    type: Object,
    required: true,
  },
});

const cover = computed(() => firstImage(props.shop.images));
</script>

<template>
  <RouterLink :to="`/shop/${shop.id}`" class="shop-preview-card panel ue-washi ue-shadow">
    <img v-if="cover" :src="cover" :alt="shop.name" class="shop-preview-cover" />
    <div class="shop-preview-content">
      <div class="shop-preview-head">
        <h3>{{ shop.name }}</h3>
        <span class="status-pill muted">评分 {{ ((shop.score || 0) / 10).toFixed(1) }}</span>
      </div>
      <p>{{ shop.area || "未知商圈" }} · {{ shop.openHours || "营业时间待补充" }}</p>
      <p>{{ formatDistance(shop.distance) }} · ￥{{ formatPrice(shop.avgPrice) }}/人</p>
      <p class="shop-preview-address">{{ shop.address || "暂无地址" }}</p>
    </div>
  </RouterLink>
</template>
