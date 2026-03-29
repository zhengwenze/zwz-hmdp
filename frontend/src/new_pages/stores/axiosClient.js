import axios from "axios";
import { setNotice } from "./appState";
import { useSessionStore } from "./session";

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use(
  (config) => {
    const session = useSessionStore();
    if (session.token) {
      config.headers.authorization = session.token;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

apiClient.interceptors.response.use(
  (response) => {
    const { success, errorMsg, data } = response.data;
    if (success === false) {
      setNotice("error", errorMsg || "请求失败");
      return Promise.reject(response.data);
    }
    return response.data;
  },
  (error) => {
    if (error.response?.status === 401) {
      const session = useSessionStore();
      session.clearSession("登录状态已失效，请重新登录。");
      window.location.hash = "#/login";
    }
    setNotice("error", error.message || "网络请求失败");
    return Promise.reject(error);
  }
);

export { apiClient };

export async function request(config) {
  try {
    const response = await apiClient({
      url: config.path,
      method: config.method || "GET",
      params: config.query,
      data: config.body,
    });
    return {
      success: true,
      data: response.data?.data,
      body: response.data,
    };
  } catch (error) {
    const response = error.response;
    if (response) {
      return {
        success: false,
        data: null,
        body: response.data,
        error: error.message,
      };
    }
    return {
      success: false,
      data: null,
      error: error.message,
    };
  }
}