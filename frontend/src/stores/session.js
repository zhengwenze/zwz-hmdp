import { defineStore } from "pinia";
import { ref, toRef, watch } from "vue";
import { setNotice } from "./appState";
import { pinia } from "./pinia";

export const useSessionStore = defineStore("session", () => {
  const token = ref(localStorage.getItem("hmdp-token") || "");
  const currentUser = ref(null);

  function clearSession(message) {
    token.value = "";
    currentUser.value = null;
    if (message) {
      setNotice("info", message);
    }
  }

  function isAuthenticated() {
    return Boolean(token.value.trim());
  }

  watch(token, (value) => localStorage.setItem("hmdp-token", value));

  return {
    token,
    currentUser,
    clearSession,
    isAuthenticated,
  };
});

export function getSessionStore() {
  return useSessionStore(pinia);
}

const sessionStore = getSessionStore();

export const sessionState = {
  token: toRef(sessionStore, "token"),
  currentUser: toRef(sessionStore, "currentUser"),
};

export function clearSession(message) {
  sessionStore.clearSession(message);
}

export function isAuthenticated() {
  return sessionStore.isAuthenticated();
}

export function buildRedirectPath(targetPath) {
  return `/login?redirect=${encodeURIComponent(targetPath || "/")}`;
}
