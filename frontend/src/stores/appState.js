import { defineStore } from "pinia";
import { reactive, ref, toRef, watch } from "vue";
import { resolveAssetUrl } from "../api";
import { pinia } from "./pinia";

export const NOTICE_MESSAGES = {
  loginSuccess: "登录成功",
  logoutSuccess: "已退出",
  loginExpired: "登录状态已失效，请重新登录",
  operationSuccess: "操作成功",
  operationFailed: "操作失败，请稍后重试",
};

function normalizeNoticeMessage(type, message) {
  const text = String(message || "").trim();

  if (!text) {
    return "";
  }
  if (text.startsWith("登录成功")) {
    return NOTICE_MESSAGES.loginSuccess;
  }
  if (
    text.startsWith("已退出") ||
    text.startsWith("本地 token 已清空") ||
    text.startsWith("本地 token 已保存")
  ) {
    return NOTICE_MESSAGES.logoutSuccess;
  }
  if (text.startsWith("登录状态已失效")) {
    return NOTICE_MESSAGES.loginExpired;
  }
  if (type === "success") {
    return NOTICE_MESSAGES.operationSuccess;
  }
  if (type === "error" && /调用失败/.test(text)) {
    return NOTICE_MESSAGES.operationFailed;
  }

  return text.replace(/[。.]$/, "");
}

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
    message: "",
    version: 0,
  });

  function setNotice(type, message) {
    notice.type = type;
    notice.message = normalizeNoticeMessage(type, message);
    notice.version += 1;
  }

  function clearNotice() {
    notice.message = "";
    notice.version += 1;
  }

  watch(apiBaseUrl, (value) => localStorage.setItem("hmdp-api-base", value));
  watch(assetBaseUrl, (value) => localStorage.setItem("hmdp-asset-base", value));

  return {
    apiBaseUrl,
    assetBaseUrl,
    notice,
    clearNotice,
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

export function clearNotice() {
  appStore.clearNotice();
}

export function toAssetUrl(path) {
  return resolveAssetUrl(
    appStore.assetBaseUrl.trim() || appStore.apiBaseUrl.trim(),
    path,
  );
}
