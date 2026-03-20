import { useMemo } from "react";
import { useUserStore } from "../store/userStore";

export const useAuth = () => {
  const { loggedIn, user, refreshToken, login, logout, setUser } = useUserStore();
  const permissions = user?.permissions ?? [];

  return useMemo(
    () => ({
      loggedIn,
      user,
      permissions,
      refreshToken,
      login,
      logout,
      setUser,
      hasPermission: (permission: string) => permissions.includes(permission),
      hasAnyPermission: (requiredPermissions: string[]) => requiredPermissions.some((permission) => permissions.includes(permission))
    }),
    [loggedIn, user, permissions, refreshToken, login, logout, setUser]
  );
};
