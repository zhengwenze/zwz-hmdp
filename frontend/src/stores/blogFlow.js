import { reactive, ref, watch } from "vue";

function readDraft() {
  try {
    return JSON.parse(localStorage.getItem("hmdp-blog-draft") || "{}");
  } catch {
    return {};
  }
}

const savedDraft = readDraft();

export const blogFlowState = {
  detailBlog: ref(null),
  blogLikes: ref([]),
  followFeed: ref([]),
  followCursor: reactive({
    minTime: String(Date.now()),
    offset: 0,
    hasMore: true,
  }),
  myBlogs: ref([]),
  userBlogs: ref([]),
  uploadedImages: ref(
    JSON.parse(localStorage.getItem("hmdp-uploaded-images") || "[]"),
  ),
  draft: reactive({
    title: savedDraft.title || "",
    content: savedDraft.content || "",
    shopId: savedDraft.shopId || "",
    shopName: savedDraft.shopName || "",
    images: Array.isArray(savedDraft.images) ? savedDraft.images : [],
  }),
};

watch(
  blogFlowState.uploadedImages,
  (value) => localStorage.setItem("hmdp-uploaded-images", JSON.stringify(value)),
  { deep: true },
);

watch(
  () => ({
    title: blogFlowState.draft.title,
    content: blogFlowState.draft.content,
    shopId: blogFlowState.draft.shopId,
    shopName: blogFlowState.draft.shopName,
    images: blogFlowState.draft.images,
  }),
  (value) => localStorage.setItem("hmdp-blog-draft", JSON.stringify(value)),
  { deep: true },
);
