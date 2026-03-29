import { defineStore } from "pinia";
import { ref } from "vue";

export const useAppStore = defineStore("app", () => {
  const notices = ref([]);
  const apiBaseUrl = ref(import.meta.env.VITE_API_BASE_BASE_URL || "");

  function setNotice(type, message) {
    const id = Date.now();
    notices.value.push({ id, type, message });
    setTimeout(() => {
      notices.value = notices.value.filter((n) => n.id !== id);
    }, 3000);
  }

  return {
    notices,
    apiBaseUrl,
    setNotice,
  };
});