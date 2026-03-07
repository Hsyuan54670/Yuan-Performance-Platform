import { App as AntdApp, ConfigProvider } from "antd";
import enUS from "antd/locale/en_US";
import zhCN from "antd/locale/zh_CN";
import dayjs from "dayjs";
import "dayjs/locale/en";
import "dayjs/locale/zh-cn";
import { useEffect, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { AppRouter } from "./router";

function App() {
  const { i18n } = useTranslation();

  const antdLocale = useMemo(() => (i18n.resolvedLanguage === "en-US" ? enUS : zhCN), [i18n.resolvedLanguage]);

  useEffect(() => {
    dayjs.locale(i18n.resolvedLanguage === "en-US" ? "en" : "zh-cn");
  }, [i18n.resolvedLanguage]);

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
