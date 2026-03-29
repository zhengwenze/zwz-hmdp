import { defineStore } from "pinia";
import { reactive, ref, toRef } from "vue";
import { pinia } from "./pinia";

export const useLabStore = defineStore("lab", () => {
  const lastResponse = ref(null);
  const requestLogs = ref([]);
  const touchedEndpoints = reactive({});
  const loadingMap = reactive({});

  function setLoading(key, value) {
    loadingMap[key] = value;
  }

  function isLoading(key) {
    return Boolean(loadingMap[key]);
  }

  function markTouched(key) {
    touchedEndpoints[key] = true;
  }

  function rememberResponse(entry, endpointKey) {
    const enriched = {
      ...entry,
      logId: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      endpointKey,
      displayTime: new Date().toLocaleTimeString("zh-CN", { hour12: false }),
    };
    lastResponse.value = enriched;
    requestLogs.value = [enriched, ...requestLogs.value].slice(0, 40);
  }

  return {
    lastResponse,
    requestLogs,
    touchedEndpoints,
    loadingMap,
    setLoading,
    isLoading,
    markTouched,
    rememberResponse,
  };
});

export function getLabStore() {
  return useLabStore(pinia);
}

const labStore = getLabStore();

export const labState = {
  lastResponse: toRef(labStore, "lastResponse"),
  requestLogs: toRef(labStore, "requestLogs"),
  touchedEndpoints: labStore.touchedEndpoints,
  loadingMap: labStore.loadingMap,
};

export function setLoading(key, value) {
  labStore.setLoading(key, value);
}

export function isLoading(key) {
  return labStore.isLoading(key);
}

export function markTouched(key) {
  labStore.markTouched(key);
}

export function rememberResponse(entry, endpointKey) {
  labStore.rememberResponse(entry, endpointKey);
}
