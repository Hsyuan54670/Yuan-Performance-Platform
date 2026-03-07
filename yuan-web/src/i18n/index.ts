import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import enUS from "../locales/en-US/common.json";
import zhCN from "../locales/zh-CN/common.json";

const LANG_KEY = "yuan_lang";

const getInitialLanguage = (): "zh-CN" | "en-US" => {
  if (typeof window === "undefined") return "zh-CN";
  const saved = window.localStorage.getItem(LANG_KEY);
  if (saved === "zh-CN" || saved === "en-US") return saved;

  const browserLang = window.navigator.language.toLowerCase();
  return browserLang.startsWith("zh") ? "zh-CN" : "en-US";
};

void i18n.use(initReactI18next).init({
  resources: {
    "zh-CN": { translation: zhCN },
    "en-US": { translation: enUS }
  },
  lng: getInitialLanguage(),
  fallbackLng: "zh-CN",
  interpolation: {
    escapeValue: false
  }
});

i18n.on("languageChanged", (lng) => {
  if (typeof window !== "undefined") {
    window.localStorage.setItem(LANG_KEY, lng);
  }
});

export default i18n;
