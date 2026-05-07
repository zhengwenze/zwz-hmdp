import { createApiClient } from "../api";
import { NOTICE_MESSAGES, setNotice, appState } from "../stores/appState";
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
});

export async function apiRequest(endpointKey, config, options = {}) {
  setLoading(endpointKey, true);
  const requestToken = sessionState.token.value.trim();
  const entry = await apiClient.request(config);
  rememberResponse(entry, endpointKey);
  setLoading(endpointKey, false);

  const body = entry.body;

  if (entry.status === 401) {
    const currentToken = sessionState.token.value.trim();
    if (requestToken !== currentToken) {
      return {
        entry,
        body,
        data: body?.data,
        success: false,
      };
    }

    clearSession();
    if (!options.silentError) {
      setNotice("error", NOTICE_MESSAGES.loginExpired);
    }
    redirectToLogin();
    if (options.onError) {
      await options.onError(body, entry);
    }

    return {
      entry,
      body,
      data: body?.data,
      success: false,
    };
  }

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
    if (!options.silentError) {
      setNotice("error", options.errorMessage || NOTICE_MESSAGES.operationFailed);
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
