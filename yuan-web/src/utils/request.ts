import axios from "axios";
import type { AxiosRequestConfig, InternalAxiosRequestConfig } from "axios";
import type { LoginResponse } from "../types/auth";
import { useUserStore } from "../store/userStore";
import { getRefreshToken, getToken } from "./token";

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

interface RetryRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE || "/api";

export const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
});

const refreshRequest = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
});

let refreshPromise: Promise<string | null> | null = null;

const isRefreshEndpoint = (url?: string): boolean => {
  if (!url) {
    return false;
  }
  return url.includes("/auth/refresh");
};

const isLoginEndpoint = (url?: string): boolean => {
  if (!url) {
    return false;
  }
  return url.includes("/auth/login");
};

const redirectToLogin = () => {
  if (typeof window === "undefined") {
    return;
  }
  if (window.location.pathname !== "/login") {
    window.location.replace("/login");
  }
};

const logoutLocally = () => {
  useUserStore.getState().logout();
  redirectToLogin();
};

const refreshSession = async (): Promise<string | null> => {
  const currentRefreshToken = getRefreshToken();
  if (!currentRefreshToken) {
    logoutLocally();
    return null;
  }

  if (!refreshPromise) {
    refreshPromise = refreshRequest
      .post<ApiResponse<LoginResponse>>("/auth/refresh", { refreshToken: currentRefreshToken })
      .then(({ data }) => {
        if (!data || typeof data.code !== "number" || data.code !== 200 || !data.data) {
          throw new Error(data?.message || "Refresh failed");
        }

        const { token, refreshToken, user } = data.data;
        useUserStore.getState().login(token, refreshToken, user);
        return token;
      })
      .catch(() => {
        logoutLocally();
        return null;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
};

request.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (resp) => {
    const payload = resp.data as ApiResponse<unknown> | undefined;
    if (payload && typeof payload.code === "number" && payload.code !== 200) {
      return Promise.reject(new Error(payload.message || "Request failed"));
    }
    return resp;
  },
  async (error) => {
    const originalRequest = error?.config as RetryRequestConfig | undefined;
    const status = error?.response?.status;

    if (
      status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !isRefreshEndpoint(originalRequest.url) &&
      !isLoginEndpoint(originalRequest.url)
    ) {
      originalRequest._retry = true;

      const newToken = await refreshSession();
      if (newToken) {
        originalRequest.headers = originalRequest.headers ?? {};
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return request(originalRequest as AxiosRequestConfig);
      }

      return Promise.reject(new Error("登录已过期，请重新登录"));
    }

    if (status === 401 && isRefreshEndpoint(originalRequest?.url)) {
      logoutLocally();
    }

    const message = error?.response?.data?.message || error?.message || "Request failed";
    return Promise.reject(new Error(message));
  }
);

export const getRequestErrorMessage = (error: unknown, fallback: string): string => {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
};

export const withMockDelay = async <T>(data: T, wait = 260): Promise<T> => {
  await new Promise((resolve) => setTimeout(resolve, wait));
  return data;
};
