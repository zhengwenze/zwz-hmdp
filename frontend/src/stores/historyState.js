import { ref, watch } from "vue";

function readList(key) {
  try {
    return JSON.parse(localStorage.getItem(key) || "[]");
  } catch {
    return [];
  }
}

function pushUnique(listRef, item, identity, limit = 8) {
  const identityValue = identity(item);
  listRef.value = [
    item,
    ...listRef.value.filter((entry) => identity(entry) !== identityValue),
  ].slice(0, limit);
}

export const historyState = {
  recentShops: ref(readList("hmdp-recent-shops")),
  recentBlogs: ref(readList("hmdp-recent-blogs")),
  recentSearches: ref(readList("hmdp-recent-searches")),
};

export function rememberShop(shop) {
  if (!shop?.id) {
    return;
  }
  pushUnique(historyState.recentShops, shop, (entry) => entry.id);
}

export function rememberBlog(blog) {
  if (!blog?.id) {
    return;
  }
  pushUnique(historyState.recentBlogs, blog, (entry) => entry.id);
}

export function rememberSearch(keyword) {
  const normalized = String(keyword || "").trim();
  if (!normalized) {
    return;
  }
  pushUnique(historyState.recentSearches, normalized, (entry) => entry, 10);
}

export function findRecentShop(shopId) {
  return historyState.recentShops.value.find((shop) => String(shop.id) === String(shopId)) || null;
}

watch(
  historyState.recentShops,
  (value) => localStorage.setItem("hmdp-recent-shops", JSON.stringify(value)),
  { deep: true },
);
watch(
  historyState.recentBlogs,
  (value) => localStorage.setItem("hmdp-recent-blogs", JSON.stringify(value)),
  { deep: true },
);
watch(historyState.recentSearches, (value) => {
  localStorage.setItem("hmdp-recent-searches", JSON.stringify(value));
});
