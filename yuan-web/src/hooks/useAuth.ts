import { useMemo } from "react";
import { useUserStore } from "../store/userStore";

export const useAuth = () => {
  const { loggedIn, user, login, logout } = useUserStore();

  return useMemo(
    () => ({
      loggedIn,
      user,
      login,
      logout
    }),
    [loggedIn, user, login, logout]
  );
};
