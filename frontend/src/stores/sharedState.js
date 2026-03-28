import { reactive, ref, watch } from "vue";
import {
  asNumber,
  cloneWithoutEmpty,
  createApiClient,
  prettify,
  resolveAssetUrl,
} from "../api";

export const sharedState = {
  apiBaseUrl: ref(
    localStorage.getItem("hmdp-api-base") ||
      import.meta.env.VITE_API_BASE_URL ||
      "",
  ),
  assetBaseUrl: ref(
    localStorage.getItem("hmdp-asset-base") ||
      import.meta.env.VITE_ASSET_BASE_URL ||
      import.meta.env.VITE_API_BASE_URL ||
      "",
  ),
  token: ref(localStorage.getItem("hmdp-token") || ""),
  currentUser: ref(null),
  lastResponse: ref(null),
  requestLogs: ref([]),
  uploadedImages: ref(
    JSON.parse(localStorage.getItem("hmdp-uploaded-images") || "[]"),
  ),

  notice: reactive({
    type: "info",
    message: "页面已接入全部后端接口。先发送验证码并登录，再验证受保护接口。",
  }),

  touchedEndpoints: reactive({}),
  loadingMap: reactive({}),

  shopTypes: ref([]),
  selectedShop: ref(null),
};

const apiClient = createApiClient({
  getBaseUrl: () => sharedState.apiBaseUrl.value.trim(),
  getToken: () => sharedState.token.value.trim(),
  onUnauthorized: () => {
    sharedState.currentUser.value = null;
    setNotice(
      "error",
      "后端返回 401。请重新登录，或检查 authorization token。",
    );
  },
});

export { apiClient };

export function setNotice(type, message) {
  sharedState.notice.type = type;
  sharedState.notice.message = message;
}

export function setLoading(key, value) {
  sharedState.loadingMap[key] = value;
}

export function isLoading(key) {
  return Boolean(sharedState.loadingMap[key]);
}

export function markTouched(key) {
  sharedState.touchedEndpoints[key] = true;
}

export function rememberResponse(entry, endpointKey) {
  const enriched = {
    ...entry,
    logId: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    endpointKey,
    displayTime: new Date().toLocaleTimeString("zh-CN", { hour12: false }),
  };
  sharedState.lastResponse.value = enriched;
  sharedState.requestLogs.value = [enriched, ...sharedState.requestLogs.value].slice(0, 40);
}

export async function send(endpointKey, config, options = {}) {
  setLoading(endpointKey, true);
  const entry = await apiClient.request(config);
  rememberResponse(entry, endpointKey);
  setLoading(endpointKey, false);

  const body = entry.body;
  const businessSuccess =
    entry.ok &&
    (typeof body !== "object" ||
    body === null ||
    !Object.prototype.hasOwnProperty.call(body, "success")
      ? true
      : body.success !== false);

  if (businessSuccess) {
    markTouched(endpointKey);
    setNotice("success", options.successMessage || `${endpointKey} 调用成功`);
    if (options.onSuccess) {
      await options.onSuccess(body?.data, body, entry);
    }
  } else {
    const message = body?.errorMsg || entry.error || `HTTP ${entry.status}`;
    setNotice("error", `${endpointKey} 调用失败：${message}`);
    if (options.onError) {
      await options.onError(body, entry);
    }
  }

  return { entry, body, data: body?.data, success: businessSuccess };
}

export function splitImages(rawValue) {
  if (!rawValue) {
    return [];
  }
  return rawValue
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

export function toAssetUrl(path) {
  return resolveAssetUrl(sharedState.assetBaseUrl.value.trim() || sharedState.apiBaseUrl.value.trim(), path);
}

export function buildShopPayload(source) {
  return cloneWithoutEmpty({
    id: asNumber(source.id),
    name: source.name,
    typeId: asNumber(source.typeId),
    images: source.images,
    area: source.area,
    address: source.address,
    x: asNumber(source.x),
    y: asNumber(source.y),
    avgPrice: asNumber(source.avgPrice),
    sold: asNumber(source.sold),
    comments: asNumber(source.comments),
    score: asNumber(source.score),
    openHours: source.openHours,
  });
}

export function buildVoucherPayload(source, includeSeckillFields = false) {
  const payload = cloneWithoutEmpty({
    shopId: asNumber(source.shopId),
    title: source.title,
    subTitle: source.subTitle,
    rules: source.rules,
    payValue: asNumber(source.payValue),
    actualValue: asNumber(source.actualValue),
    type: asNumber(source.type),
  });

  if (includeSeckillFields) {
    Object.assign(
      payload,
      cloneWithoutEmpty({
        stock: asNumber(source.stock),
        beginTime: normalizeDateTime(source.beginTime),
        endTime: normalizeDateTime(source.endTime),
      }),
    );
  }

  return payload;
}

function normalizeDateTime(value) {
  if (!value) {
    return undefined;
  }
  return value.length === 16 ? `${value}:00` : value;
}

export function buildBlogPayload(forms) {
  return cloneWithoutEmpty({
    shopId: asNumber(forms.blog.create.shopId),
    title: forms.blog.create.title,
    images: forms.blog.create.images,
    content: forms.blog.create.content,
  });
}

export function endpointBadgeClass(done) {
  return done ? "status-pill success" : "status-pill muted";
}

export { prettify } from "../api";

watch(sharedState.apiBaseUrl, (value) => localStorage.setItem("hmdp-api-base", value));
watch(sharedState.assetBaseUrl, (value) => localStorage.setItem("hmdp-asset-base", value));
watch(sharedState.token, (value) => localStorage.setItem("hmdp-token", value));
watch(
  sharedState.uploadedImages,
  (value) =>
    localStorage.setItem("hmdp-uploaded-images", JSON.stringify(value)),
  { deep: true },
);
