import { ref } from "vue";

export const homeFeedState = {
  shopTypes: ref([]),
  hotBlogs: ref([]),
  currentPage: ref(1),
  hasMore: ref(true),
  initialized: ref(false),
};
