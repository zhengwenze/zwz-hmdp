import { defineStore } from "pinia";
import { ref, toRef, watch } from "vue";
import { pinia } from "./pinia";

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

export const useHistoryStore = defineStore("history", () => {
  const recentShops = ref(readList("hmdp-recent-shops"));
  const recentBlogs = ref(readList("hmdp-recent-blogs"));
  const recentSearches = ref(readList("hmdp-recent-searches"));

  function rememberShop(shop) {
    if (!shop?.id) {
      return;
    }
    pushUnique(recentShops, shop, (entry) => entry.id);
  }

  function rememberBlog(blog) {
    if (!blog?.id) {
      return;
    }
    pushUnique(recentBlogs, blog, (entry) => entry.id);
  }

  function rememberSearch(keyword) {
    const normalized = String(keyword || "").trim();
    if (!normalized) {
      return;
    }
    pushUnique(recentSearches, normalized, (entry) => entry, 10);
  }

  function findRecentShop(shopId) {
    return recentShops.value.find((shop) => String(shop.id) === String(shopId)) || null;
  }

  watch(
    recentShops,
    (value) => localStorage.setItem("hmdp-recent-shops", JSON.stringify(value)),
    { deep: true },
  );
  watch(
    recentBlogs,
    (value) => localStorage.setItem("hmdp-recent-blogs", JSON.stringify(value)),
    { deep: true },
  );
  watch(recentSearches, (value) => {
    localStorage.setItem("hmdp-recent-searches", JSON.stringify(value));
  });

  return {
    recentShops,
    recentBlogs,
    recentSearches,
    rememberShop,
    rememberBlog,
    rememberSearch,
    findRecentShop,
  };
});

export function getHistoryStore() {
  return useHistoryStore(pinia);
}

const historyStore = getHistoryStore();

export const historyState = {
  recentShops: toRef(historyStore, "recentShops"),
  recentBlogs: toRef(historyStore, "recentBlogs"),
  recentSearches: toRef(historyStore, "recentSearches"),
};

export function rememberShop(shop) {
  historyStore.rememberShop(shop);
}

export function rememberBlog(blog) {
  historyStore.rememberBlog(blog);
}

export function rememberSearch(keyword) {
  historyStore.rememberSearch(keyword);
}

export function findRecentShop(shopId) {
  return historyStore.findRecentShop(shopId);
}
