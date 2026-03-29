import { ref, watch } from "vue";
import { setNotice } from "./appState";

export const sessionState = {
  token: ref(localStorage.getItem("hmdp-token") || ""),
  currentUser: ref(null),
};

export function clearSession(message) {
  sessionState.token.value = "";
  sessionState.currentUser.value = null;
  if (message) {
    setNotice("info", message);
  }
}

export function isAuthenticated() {
  return Boolean(sessionState.token.value.trim());
}

export function buildRedirectPath(targetPath) {
  return `/login?redirect=${encodeURIComponent(targetPath || "/")}`;
}

watch(sessionState.token, (value) => localStorage.setItem("hmdp-token", value));
