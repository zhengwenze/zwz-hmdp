import { defineStore } from "pinia";
import { ref, reactive } from "vue";

export const useBlogStore = defineStore("blog", () => {
  const detailBlog = ref(null);
  const blogLikes = ref([]);
  const followFeed = ref([]);
  const followCursor = reactive({
    minTime: String(Date.now()),
    offset: 0,
    hasMore: true,
  });
  const myBlogs = ref([]);
  const userBlogs = ref([]);

  const draft = reactive({
    title: "",
    content: "",
    shopId: "",
    shopName: "",
    images: [],
  });

  function loadDraft() {
    try {
      const saved = JSON.parse(localStorage.getItem("hmdp-blog-draft") || "{}");
      draft.title = saved.title || "";
      draft.content = saved.content || "";
      draft.shopId = saved.shopId || "";
      draft.shopName = saved.shopName || "";
      draft.images = Array.isArray(saved.images) ? saved.images : [];
    } catch {
      // ignore
    }
  }

  function saveDraft() {
    localStorage.setItem(
      "hmdp-blog-draft",
      JSON.stringify({
        title: draft.title,
        content: draft.content,
        shopId: draft.shopId,
        shopName: draft.shopName,
        images: draft.images,
      })
    );
  }

  function resetFollowCursor() {
    followCursor.minTime = String(Date.now());
    followCursor.offset = 0;
    followCursor.hasMore = true;
  }

  function resetDraft() {
    draft.title = "";
    draft.content = "";
    draft.shopId = "";
    draft.shopName = "";
    draft.images = [];
    localStorage.removeItem("hmdp-blog-draft");
  }

  return {
    detailBlog,
    blogLikes,
    followFeed,
    followCursor,
    myBlogs,
    userBlogs,
    draft,
    loadDraft,
    saveDraft,
    resetFollowCursor,
    resetDraft,
  };
});