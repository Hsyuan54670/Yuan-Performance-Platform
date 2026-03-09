import { useMemo } from "react";
import { useUserStore } from "../store/userStore";

export const useAuth = () => {
  const { loggedIn, user, refreshToken, login, logout, setUser } = useUserStore();

  return useMemo(
    () => ({
      loggedIn,
      user,
      refreshToken,
      login,
      logout,
      setUser
    }),
    [loggedIn, user, refreshToken, login, logout, setUser]
  );
};
