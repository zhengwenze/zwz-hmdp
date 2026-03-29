import { toAssetUrl } from "../stores/appState";

export function splitImages(rawValue) {
  if (!rawValue) {
    return [];
  }
  return rawValue
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => toAssetUrl(item));
}

export function firstImage(rawValue) {
  return splitImages(rawValue)[0] || "";
}

export function formatPrice(value) {
  if (value === undefined || value === null || value === "") {
    return "--";
  }
  return Number(value).toLocaleString("zh-CN");
}

export function formatDistance(value) {
  if (value === undefined || value === null || value === "") {
    return "未提供距离";
  }
  const distance = Number(value);
  if (!Number.isFinite(distance)) {
    return "未提供距离";
  }
  return distance < 1000
    ? `${distance.toFixed(0)}m`
    : `${(distance / 1000).toFixed(1)}km`;
}

export function formatDateTime(value) {
  if (!value) {
    return "未知时间";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatVoucherWindow(voucher) {
  if (!voucher?.beginTime || !voucher?.endTime) {
    return "常规券";
  }
  return `${formatDateTime(voucher.beginTime)} - ${formatDateTime(voucher.endTime)}`;
}

export function voucherState(voucher) {
  if (!voucher?.type) {
    return { label: "常规券", disabled: true };
  }
  const now = Date.now();
  const begin = new Date(voucher.beginTime).getTime();
  const end = new Date(voucher.endTime).getTime();

  if (begin > now) {
    return { label: "尚未开始", disabled: true };
  }
  if (end < now) {
    return { label: "已结束", disabled: true };
  }
  if ((voucher.stock ?? 0) < 1) {
    return { label: "库存不足", disabled: true };
  }
  return { label: "立即秒杀", disabled: false };
}
