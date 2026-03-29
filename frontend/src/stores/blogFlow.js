import { defineStore } from "pinia";
import { reactive, ref, toRef, watch } from "vue";
import { pinia } from "./pinia";

function readDraft() {
  try {
    return JSON.parse(localStorage.getItem("hmdp-blog-draft") || "{}");
  } catch {
    return {};
  }
}

function readUploadedImages() {
  try {
    return JSON.parse(localStorage.getItem("hmdp-uploaded-images") || "[]");
  } catch {
    return [];
  }
}

export const useBlogFlowStore = defineStore("blogFlow", () => {
  const savedDraft = readDraft();
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
  const uploadedImages = ref(readUploadedImages());
  const draft = reactive({
    title: savedDraft.title || "",
    content: savedDraft.content || "",
    shopId: savedDraft.shopId || "",
    shopName: savedDraft.shopName || "",
    images: Array.isArray(savedDraft.images) ? savedDraft.images : [],
  });

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

  watch(
    uploadedImages,
    (value) => localStorage.setItem("hmdp-uploaded-images", JSON.stringify(value)),
    { deep: true },
  );

  watch(
    () => ({
      title: draft.title,
      content: draft.content,
      shopId: draft.shopId,
      shopName: draft.shopName,
      images: draft.images,
    }),
    (value) => localStorage.setItem("hmdp-blog-draft", JSON.stringify(value)),
    { deep: true },
  );

  return {
    detailBlog,
    blogLikes,
    followFeed,
    followCursor,
    myBlogs,
    userBlogs,
    uploadedImages,
    draft,
    resetFollowCursor,
    resetDraft,
  };
});

export function getBlogFlowStore() {
  return useBlogFlowStore(pinia);
}

const blogFlowStore = getBlogFlowStore();

export const blogFlowState = {
  detailBlog: toRef(blogFlowStore, "detailBlog"),
  blogLikes: toRef(blogFlowStore, "blogLikes"),
  followFeed: toRef(blogFlowStore, "followFeed"),
  followCursor: blogFlowStore.followCursor,
  myBlogs: toRef(blogFlowStore, "myBlogs"),
  userBlogs: toRef(blogFlowStore, "userBlogs"),
  uploadedImages: toRef(blogFlowStore, "uploadedImages"),
  draft: blogFlowStore.draft,
};

export function resetFollowCursor() {
  blogFlowStore.resetFollowCursor();
}

export function resetDraft() {
  blogFlowStore.resetDraft();
}
