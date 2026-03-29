import { createApiClient } from "../api";
import { setNotice, appState } from "../stores/appState";
import { clearSession, sessionState } from "../stores/session";
import {
  markTouched,
  rememberResponse,
  setLoading,
} from "../stores/labState";

function currentPathForRedirect() {
  const hash = window.location.hash.replace(/^#/, "");
  return hash || "/";
}

function redirectToLogin() {
  const currentPath = currentPathForRedirect();
  if (currentPath.startsWith("/login")) {
    return;
  }
  window.location.hash = `#${"/login?redirect=" + encodeURIComponent(currentPath)}`;
}

export const apiClient = createApiClient({
  getBaseUrl: () => appState.apiBaseUrl.value.trim(),
  getToken: () => sessionState.token.value.trim(),
  onUnauthorized: () => {
    clearSession();
    setNotice("error", "登录状态已失效，请重新登录。");
    redirectToLogin();
  },
});

export async function apiRequest(endpointKey, config, options = {}) {
  setLoading(endpointKey, true);
  const entry = await apiClient.request(config);
  rememberResponse(entry, endpointKey);
  setLoading(endpointKey, false);

  const body = entry.body;
  const businessSuccess =
    entry.ok
    && (typeof body !== "object"
      || body === null
      || !Object.prototype.hasOwnProperty.call(body, "success")
      || body.success !== false);

  if (businessSuccess) {
    markTouched(endpointKey);
    if (options.successMessage) {
      setNotice("success", options.successMessage);
    }
    if (options.onSuccess) {
      await options.onSuccess(body?.data, body, entry);
    }
  } else {
    const message = body?.errorMsg || entry.error || `HTTP ${entry.status}`;
    if (!options.silentError) {
      setNotice("error", options.errorMessage || `${endpointKey} 调用失败：${message}`);
    }
    if (options.onError) {
      await options.onError(body, entry);
    }
  }

  return {
    entry,
    body,
    data: body?.data,
    success: businessSuccess,
  };
}
