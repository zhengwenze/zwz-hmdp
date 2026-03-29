import { reactive, ref, watch } from "vue";
import { resolveAssetUrl } from "../api";

export const appState = {
  apiBaseUrl: ref(
    localStorage.getItem("hmdp-api-base") || import.meta.env.VITE_API_BASE_URL || "",
  ),
  assetBaseUrl: ref(
    localStorage.getItem("hmdp-asset-base")
      || import.meta.env.VITE_ASSET_BASE_URL
      || import.meta.env.VITE_API_BASE_URL
      || "",
  ),
  notice: reactive({
    type: "info",
    message: "欢迎进入浮世绘版黑马点评主站。登录后可体验关注流、秒杀和发笔记。",
  }),
};

export function setNotice(type, message) {
  appState.notice.type = type;
  appState.notice.message = message;
}

export function toAssetUrl(path) {
  return resolveAssetUrl(
    appState.assetBaseUrl.value.trim() || appState.apiBaseUrl.value.trim(),
    path,
  );
}

watch(appState.apiBaseUrl, (value) => localStorage.setItem("hmdp-api-base", value));
watch(appState.assetBaseUrl, (value) => localStorage.setItem("hmdp-asset-base", value));
