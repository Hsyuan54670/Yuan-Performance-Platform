import axios from "axios";
import { getToken } from "./token";

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || "/api",
  timeout: 10000
});

request.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (resp) => resp,
  (error) => Promise.reject(error)
);

export const withMockDelay = async <T>(data: T, wait = 260): Promise<T> => {
  await new Promise((resolve) => setTimeout(resolve, wait));
  return data;
};
