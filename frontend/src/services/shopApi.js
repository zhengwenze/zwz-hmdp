import { apiRequest } from "./httpClient";

export const shopApi = {
  fetchTypes(options = {}) {
    return apiRequest(
      "GET /shop-type/list",
      { method: "GET", path: "/shop-type/list" },
      options,
    );
  },
  fetchByType(query, options = {}) {
    return apiRequest(
      "GET /shop/of/type",
      { method: "GET", path: "/shop/of/type", query },
      options,
    );
  },
  fetchByName(query, options = {}) {
    return apiRequest(
      "GET /shop/of/name",
      { method: "GET", path: "/shop/of/name", query },
      options,
    );
  },
  fetchDetail(shopId, options = {}) {
    return apiRequest(
      "GET /shop/{id}",
      { method: "GET", path: `/shop/${shopId}` },
      options,
    );
  },
  create(payload, options = {}) {
    return apiRequest(
      "POST /shop",
      { method: "POST", path: "/shop", body: payload },
      options,
    );
  },
  update(payload, options = {}) {
    return apiRequest(
      "PUT /shop",
      { method: "PUT", path: "/shop", body: payload },
      options,
    );
  },
};
