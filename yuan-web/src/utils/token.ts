const TOKEN_KEY = "yuan_token";

export const getToken = (): string => localStorage.getItem(TOKEN_KEY) || "";

export const setToken = (token: string): void => localStorage.setItem(TOKEN_KEY, token);

export const clearToken = (): void => localStorage.removeItem(TOKEN_KEY);
