import { reactive, ref } from "vue";

export const labState = {
  lastResponse: ref(null),
  requestLogs: ref([]),
  touchedEndpoints: reactive({}),
  loadingMap: reactive({}),
};

export function setLoading(key, value) {
  labState.loadingMap[key] = value;
}

export function isLoading(key) {
  return Boolean(labState.loadingMap[key]);
}

export function markTouched(key) {
  labState.touchedEndpoints[key] = true;
}

export function rememberResponse(entry, endpointKey) {
  const enriched = {
    ...entry,
    logId: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    endpointKey,
    displayTime: new Date().toLocaleTimeString("zh-CN", { hour12: false }),
  };
  labState.lastResponse.value = enriched;
  labState.requestLogs.value = [enriched, ...labState.requestLogs.value].slice(0, 40);
}
