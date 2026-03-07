import { mockLogin } from "../mock/data";
import type { LoginRequest, LoginResponse, UserInfo } from "../types/auth";
import { withMockDelay } from "../utils/request";

export const loginApi = async (payload: LoginRequest): Promise<LoginResponse> => {
  const user = { ...mockLogin.user, username: payload.username || mockLogin.user.username };
  return withMockDelay({ ...mockLogin, user });
};

export const logoutApi = async (): Promise<boolean> => withMockDelay(true, 140);

export const userInfoApi = async (): Promise<UserInfo> => withMockDelay(mockLogin.user, 100);
