import { defineStore } from "pinia";
import { ref, toRef } from "vue";
import { pinia } from "./pinia";

export const useHomeFeedStore = defineStore("homeFeed", () => {
  const shopTypes = ref([]);
  const hotBlogs = ref([]);
  const currentPage = ref(1);
  const hasMore = ref(true);
  const initialized = ref(false);

  return {
    shopTypes,
    hotBlogs,
    currentPage,
    hasMore,
    initialized,
  };
});

export function getHomeFeedStore() {
  return useHomeFeedStore(pinia);
}

const homeFeedStore = getHomeFeedStore();

export const homeFeedState = {
  shopTypes: toRef(homeFeedStore, "shopTypes"),
  hotBlogs: toRef(homeFeedStore, "hotBlogs"),
  currentPage: toRef(homeFeedStore, "currentPage"),
  hasMore: toRef(homeFeedStore, "hasMore"),
  initialized: toRef(homeFeedStore, "initialized"),
};
