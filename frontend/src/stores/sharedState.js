import { asNumber, cloneWithoutEmpty } from "../api";
import { apiClient, apiRequest as send } from "../services/httpClient";
import { appState, setNotice, toAssetUrl } from "./appState";
import { sessionState } from "./session";
import { homeFeedState } from "./homeFeed";
import { shopFlowState } from "./shopFlow";
import { blogFlowState } from "./blogFlow";
import {
  labState,
  isLoading,
  setLoading,
} from "./labState";

export const sharedState = {
  apiBaseUrl: appState.apiBaseUrl,
  assetBaseUrl: appState.assetBaseUrl,
  token: sessionState.token,
  currentUser: sessionState.currentUser,
  lastResponse: labState.lastResponse,
  requestLogs: labState.requestLogs,
  uploadedImages: blogFlowState.uploadedImages,
  notice: appState.notice,
  touchedEndpoints: labState.touchedEndpoints,
  loadingMap: labState.loadingMap,
  shopTypes: homeFeedState.shopTypes,
  selectedShop: shopFlowState.selectedShop,
};

export { apiClient, send, setNotice, setLoading, isLoading };

export function splitImages(rawValue) {
  if (!rawValue) {
    return [];
  }
  return rawValue
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
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

export { toAssetUrl };
export { prettify } from "../api";
