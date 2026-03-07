import { create } from "zustand";
import type { UserInfo } from "../types/auth";
import { clearToken, getToken, setToken } from "../utils/token";

interface UserState {
  token: string;
  user: UserInfo | null;
  loggedIn: boolean;
  login: (token: string, user: UserInfo) => void;
  logout: () => void;
}

export const useUserStore = create<UserState>((set) => ({
  token: getToken(),
  user: null,
  loggedIn: !!getToken(),
  login: (token, user) => {
    setToken(token);
    set({ token, user, loggedIn: true });
  },
  logout: () => {
    clearToken();
    set({ token: "", user: null, loggedIn: false });
  }
}));
