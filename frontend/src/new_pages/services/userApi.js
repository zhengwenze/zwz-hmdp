import { apiClient } from "./axiosClient";

export const userApi = {
  sendCode(phone) {
    return apiClient({
      method: "POST",
      path: "/user/code",
      params: { phone },
    });
  },

  login(data) {
    return apiClient({
      method: "POST",
      path: "/user/login",
      data,
    });
  },

  logout() {
    return apiClient({
      method: "POST",
      path: "/user/logout",
    });
  },

  fetchMe() {
    return apiClient({
      method: "GET",
      path: "/user/me",
    });
  },

  fetchUserInfo(userId) {
    return apiClient({
      method: "GET",
      path: `/user/info/${userId}`,
    });
  },

  fetchUserSummary(userId) {
    return apiClient({
      method: "GET",
      path: `/user/${userId}`,
    });
  },

  sign() {
    return apiClient({
      method: "POST",
      path: "/user/sign",
    });
  },

  fetchSignCount() {
    return apiClient({
      method: "GET",
      path: "/user/sign/count",
    });
  },

  updateNickName(nickName) {
    return apiClient({
      method: "PUT",
      path: "/user/nickname",
      params: { nickName },
    });
  },
};