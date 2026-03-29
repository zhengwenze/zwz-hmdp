import { defineStore } from "pinia";
import { ref, watch } from "vue";
import { setNotice } from "./appState";

export const useSessionStore = defineStore("session", () => {
  const token = ref(localStorage.getItem("hmdp-token") || "");
  const currentUser = ref(null);

  watch(token, (value) => {
    localStorage.setItem("hmdp-token", value);
  });

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

  return {
    token,
    currentUser,
    clearSession,
    isAuthenticated,
  };
});