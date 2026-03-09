const TOKEN_KEY = "yuan_token";
const REFRESH_TOKEN_KEY = "yuan_refresh_token";

export const getToken = (): string => localStorage.getItem(TOKEN_KEY) || "";

export const setToken = (token: string): void => localStorage.setItem(TOKEN_KEY, token);

export const getRefreshToken = (): string => localStorage.getItem(REFRESH_TOKEN_KEY) || "";

export const setRefreshToken = (token: string): void => localStorage.setItem(REFRESH_TOKEN_KEY, token);

export const clearToken = (): void => localStorage.removeItem(TOKEN_KEY);

export const clearRefreshToken = (): void => localStorage.removeItem(REFRESH_TOKEN_KEY);
