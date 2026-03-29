import { defineStore } from "pinia";
import { ref } from "vue";

export const useShopStore = defineStore("shop", () => {
  const currentShop = ref(null);
  const recentShops = ref([]);

  function addRecentShop(shop) {
    const existing = recentShops.value.find((s) => s.id === shop.id);
    if (existing) {
      return;
    }
    recentShops.value.unshift(shop);
    if (recentShops.value.length > 10) {
      recentShops.value = recentShops.value.slice(0, 10);
    }
  }

  return {
    currentShop,
    recentShops,
    addRecentShop,
  };
});