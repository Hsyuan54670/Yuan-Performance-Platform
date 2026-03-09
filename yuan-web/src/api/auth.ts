import type { LoginRequest, LoginResponse, UserInfo } from "../types/auth";
import { getRefreshToken } from "../utils/token";
import { request, type ApiResponse } from "../utils/request";

export const loginApi = async (payload: LoginRequest): Promise<LoginResponse> => {
  const { data } = await request.post<ApiResponse<LoginResponse>>("/auth/login", payload);
  return data.data;
};

export const logoutApi = async (): Promise<boolean> => {
  await request.post<ApiResponse<null>>("/auth/logout", { refreshToken: getRefreshToken() });
  return true;
};

export const refreshTokenApi = async (): Promise<LoginResponse> => {
  const { data } = await request.post<ApiResponse<LoginResponse>>("/auth/refresh", {
    refreshToken: getRefreshToken()
  });
  return data.data;
};

export const userInfoApi = async (): Promise<UserInfo> => {
  const { data } = await request.get<ApiResponse<UserInfo>>("/auth/me");
  return data.data;
};
