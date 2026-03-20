import dayjs from "dayjs";

export const formatPercent = (value: number): string => `${value.toFixed(2)}%`;

export const formatMs = (value: number): string => `${Math.round(value)} ms`;

export const formatQps = (value: number): string => `${Math.round(value)} req/s`;

export const formatDateTime = (value?: string | null): string => {
  if (!value) {
    return "-";
  }
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format("YYYY-MM-DD HH:mm:ss") : "-";
};

export const randomInRange = (min: number, max: number): number =>
  Number((Math.random() * (max - min) + min).toFixed(2));
