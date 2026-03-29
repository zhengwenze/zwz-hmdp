import { defineStore } from "pinia";
import { ref } from "vue";

export const useHomeStore = defineStore("home", () => {
  const shopTypes = ref([]);
  const hotBlogs = ref([]);
  const currentPage = ref(1);
  const hasMore = ref(true);

  function reset() {
    currentPage.value = 1;
    hasMore.value = true;
    hotBlogs.value = [];
  }

  return {
    shopTypes,
    hotBlogs,
    currentPage,
    hasMore,
    reset,
  };
});