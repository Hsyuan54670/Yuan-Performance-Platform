import { App as AntdApp, ConfigProvider } from "antd";
import enUS from "antd/locale/en_US";
import zhCN from "antd/locale/zh_CN";
import dayjs from "dayjs";
import "dayjs/locale/en";
import "dayjs/locale/zh-cn";
import { useEffect, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { refreshTokenApi, userInfoApi } from "./api/auth";
import { AppRouter } from "./router";
import { useUserStore } from "./store/userStore";

function App() {
  const { i18n } = useTranslation();
  const { loggedIn, user, setUser, login, logout } = useUserStore();

  const antdLocale = useMemo(() => (i18n.resolvedLanguage === "en-US" ? enUS : zhCN), [i18n.resolvedLanguage]);

  useEffect(() => {
    dayjs.locale(i18n.resolvedLanguage === "en-US" ? "en" : "zh-cn");
  }, [i18n.resolvedLanguage]);

  useEffect(() => {
    if (!loggedIn || user) return;

    let active = true;
    void userInfoApi()
      .then((result) => {
        if (active) setUser(result);
      })
      .catch(async () => {
        try {
          const refreshed = await refreshTokenApi();
          if (active) login(refreshed.token, refreshed.refreshToken, refreshed.user);
        } catch {
          if (active) logout();
        }
      });

    return () => {
      active = false;
    };
  }, [loggedIn, user, setUser, login, logout]);

  return (
    <ConfigProvider
      locale={antdLocale}
      theme={{
        token: {
          colorPrimary: "#0b7285",
          borderRadius: 14,
          fontFamily: "Poppins, Segoe UI, Noto Sans SC, sans-serif"
        }
      }}
    >
      <AntdApp>
        <AppRouter />
      </AntdApp>
    </ConfigProvider>
  );
}

export default App;
