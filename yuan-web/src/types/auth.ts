export interface UserInfo {
  id: number;
  username: string;
  nickname: string;
  role: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: UserInfo;
}
