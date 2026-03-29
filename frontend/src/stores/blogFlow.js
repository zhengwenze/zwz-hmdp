import { reactive, ref, watch } from "vue";

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
    title: "",
    content: "",
    shopId: "",
    shopName: "",
    images: [],
  }),
};

watch(
  blogFlowState.uploadedImages,
  (value) => localStorage.setItem("hmdp-uploaded-images", JSON.stringify(value)),
  { deep: true },
);
