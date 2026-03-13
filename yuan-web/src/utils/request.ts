import axios from "axios";
import { getToken } from "./token";

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || "/api",
  timeout: 10000,
});

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
  (error) => {
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
