export interface AuthorizedMenu {
  id: number;
  name: string;
  path: string;
  children?: AuthorizedMenu[];
}

export interface UserInfo {
  id: number;
  username: string;
  nickname: string;
  role: string;
  roles: string[];
  permissions: string[];
  menus: AuthorizedMenu[];
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
