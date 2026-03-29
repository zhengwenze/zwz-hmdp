import { apiRequest } from "./httpClient";

export const voucherApi = {
  create(payload, options = {}) {
    return apiRequest(
      "POST /voucher",
      { method: "POST", path: "/voucher", body: payload },
      options,
    );
  },
  createSeckill(payload, options = {}) {
    return apiRequest(
      "POST /voucher/seckill",
      { method: "POST", path: "/voucher/seckill", body: payload },
      options,
    );
  },
  fetchList(shopId, options = {}) {
    return apiRequest(
      "GET /voucher/list/{shopId}",
      { method: "GET", path: `/voucher/list/${shopId}` },
      options,
    );
  },
  seckill(voucherId, options = {}) {
    return apiRequest(
      "POST /voucher-order/seckill/{id}",
      { method: "POST", path: `/voucher-order/seckill/${voucherId}` },
      options,
    );
  },
};
