import { create } from "zustand";
import type { UserInfo } from "../types/auth";
import {
  clearRefreshToken,
  clearToken,
  getRefreshToken,
  getToken,
  setRefreshToken,
  setToken
} from "../utils/token";

interface UserState {
  token: string;
  refreshToken: string;
  user: UserInfo | null;
  loggedIn: boolean;
  login: (token: string, refreshToken: string, user: UserInfo) => void;
  setUser: (user: UserInfo | null) => void;
  logout: () => void;
}

export const useUserStore = create<UserState>((set) => ({
  token: getToken(),
  refreshToken: getRefreshToken(),
  user: null,
  loggedIn: !!getToken(),
  login: (token, refreshToken, user) => {
    setToken(token);
    setRefreshToken(refreshToken);
    set({ token, refreshToken, user, loggedIn: true });
  },
  setUser: (user) => set({ user }),
  logout: () => {
    clearToken();
    clearRefreshToken();
    set({ token: "", refreshToken: "", user: null, loggedIn: false });
  }
}));
