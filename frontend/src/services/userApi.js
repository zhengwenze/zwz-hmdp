import { apiRequest } from "./httpClient";

export const userApi = {
  sendCode(phone, options = {}) {
    return apiRequest(
      "POST /user/code",
      { method: "POST", path: "/user/code", query: { phone } },
      options,
    );
  },
  login(payload, options = {}) {
    return apiRequest(
      "POST /user/login",
      { method: "POST", path: "/user/login", body: payload },
      options,
    );
  },
  logout(options = {}) {
    return apiRequest(
      "POST /user/logout",
      { method: "POST", path: "/user/logout" },
      options,
    );
  },
  fetchMe(options = {}) {
    return apiRequest(
      "GET /user/me",
      { method: "GET", path: "/user/me" },
      options,
    );
  },
  fetchUserInfo(userId, options = {}) {
    return apiRequest(
      "GET /user/info/{id}",
      { method: "GET", path: `/user/info/${userId}` },
      options,
    );
  },
  fetchUserSummary(userId, options = {}) {
    return apiRequest(
      "GET /user/{id}",
      { method: "GET", path: `/user/${userId}` },
      options,
    );
  },
  sign(options = {}) {
    return apiRequest(
      "POST /user/sign",
      { method: "POST", path: "/user/sign" },
      options,
    );
  },
  fetchSignCount(options = {}) {
    return apiRequest(
      "GET /user/sign/count",
      { method: "GET", path: "/user/sign/count" },
      options,
    );
  },
  updateNickName(nickName, options = {}) {
    return apiRequest(
      "PUT /user/nickname",
      { method: "PUT", path: "/user/nickname", query: { nickName } },
      options,
    );
  },
};
