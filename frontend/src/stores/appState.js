import { defineStore } from "pinia";
import { reactive, ref, toRef, watch } from "vue";
import { resolveAssetUrl } from "../api";
import { pinia } from "./pinia";

export const useAppStore = defineStore("app", () => {
  const apiBaseUrl = ref(
    localStorage.getItem("hmdp-api-base") || import.meta.env.VITE_API_BASE_URL || "",
  );
  const assetBaseUrl = ref(
    localStorage.getItem("hmdp-asset-base")
      || import.meta.env.VITE_ASSET_BASE_URL
      || import.meta.env.VITE_API_BASE_URL
      || "",
  );
  const notice = reactive({
    type: "info",
    message: "欢迎进入浮世绘版黑马点评主站。登录后可体验关注流、秒杀和发笔记。",
  });

  function setNotice(type, message) {
    notice.type = type;
    notice.message = message;
  }

  watch(apiBaseUrl, (value) => localStorage.setItem("hmdp-api-base", value));
  watch(assetBaseUrl, (value) => localStorage.setItem("hmdp-asset-base", value));

  return {
    apiBaseUrl,
    assetBaseUrl,
    notice,
    setNotice,
  };
});

export function getAppStore() {
  return useAppStore(pinia);
}

const appStore = getAppStore();

export const appState = {
  apiBaseUrl: toRef(appStore, "apiBaseUrl"),
  assetBaseUrl: toRef(appStore, "assetBaseUrl"),
  notice: appStore.notice,
};

export function setNotice(type, message) {
  appStore.setNotice(type, message);
}

export function toAssetUrl(path) {
  return resolveAssetUrl(
    appStore.assetBaseUrl.trim() || appStore.apiBaseUrl.trim(),
    path,
  );
}
