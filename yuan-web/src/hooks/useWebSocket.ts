import { useEffect, useState } from "react";
import { metricsHistoryApi } from "../api/test";
import { getToken } from "../utils/token";
import type { RealtimeMetricPoint } from "../types/test";

export type RealtimeTransport = "websocket" | "polling" | "disconnected";

const MAX_SERIES_POINTS = 180;
const POLL_INTERVAL_MS = 3000;
const WS_FALLBACK_TIMEOUT_MS = 2000;

const padTime = (value: number) => String(value).padStart(2, "0");

const toTimeLabel = (date: Date) => {
  if (Number.isNaN(date.getTime())) {
    return "--:--:--";
  }

  return [date.getHours(), date.getMinutes(), date.getSeconds()].map(padTime).join(":");
};

const normalizeTime = (value: unknown) => {
  if (typeof value === "string") {
    if (/^\d{2}:\d{2}:\d{2}$/.test(value)) {
      return value;
    }
    return toTimeLabel(new Date(value));
  }

  if (typeof value === "number") {
    return toTimeLabel(new Date(value));
  }

  return toTimeLabel(new Date());
};

const toNumber = (value: unknown) => {
  const next = Number(value);
  return Number.isFinite(next) ? next : 0;
};

const trimSeries = (series: RealtimeMetricPoint[]) =>
  series.length <= MAX_SERIES_POINTS ? series : series.slice(-MAX_SERIES_POINTS);

const normalizePoint = (payload: unknown): RealtimeMetricPoint | null => {
  if (!payload || typeof payload !== "object") {
    return null;
  }

  const record = payload as Record<string, unknown>;
  if (!("qps" in record) && !("p99" in record) && !("errorRate" in record)) {
    return null;
  }

  return {
    time: normalizeTime(record.time ?? record.timestamp ?? record.ts ?? record.createdAt),
    qps: toNumber(record.qps),
    p50: toNumber(record.p50),
    p90: toNumber(record.p90),
    p99: toNumber(record.p99),
    errorRate: toNumber(record.errorRate)
  };
};

const extractPoints = (payload: unknown): RealtimeMetricPoint[] => {
  if (Array.isArray(payload)) {
    return payload.map(normalizePoint).filter((item): item is RealtimeMetricPoint => item !== null);
  }

  if (payload && typeof payload === "object" && "data" in (payload as Record<string, unknown>)) {
    return extractPoints((payload as Record<string, unknown>).data);
  }

  const point = normalizePoint(payload);
  return point ? [point] : [];
};

const mergeSeries = (current: RealtimeMetricPoint[], incoming: RealtimeMetricPoint[]) => {
  if (!incoming.length) {
    return current;
  }

  if (incoming.length > 1) {
    return trimSeries(incoming);
  }

  const next = [...current];
  const latest = incoming[0];
  const previous = next.at(-1);

  if (previous?.time === latest.time) {
    next[next.length - 1] = latest;
  } else {
    next.push(latest);
  }

  return trimSeries(next);
};

const parseSocketMessage = (payload: string | ArrayBuffer | Blob) => {
  if (typeof payload !== "string") {
    return null;
  }

  try {
    return JSON.parse(payload);
  } catch {
    return null;
  }
};

const getWebSocketUrl = (taskId: number) => {
  const explicitBase = import.meta.env.VITE_WS_BASE?.trim();
  const base =
    explicitBase ||
    (typeof window !== "undefined"
      ? `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}`
      : "");

  if (!base) {
    return "";
  }

  const token = getToken();
  const query = token ? `?token=${encodeURIComponent(token)}` : "";

  return `${base.replace(/\/$/, "")}/ws/monitor/${taskId}${query}`;
};

export const useWebSocket = (taskId: number, resetKey?: string | number | null) => {
  const [connected, setConnected] = useState(false);
  const [series, setSeries] = useState<RealtimeMetricPoint[]>([]);
  const [transport, setTransport] = useState<RealtimeTransport>("disconnected");

  useEffect(() => {
    if (!taskId) {
      setConnected(false);
      setSeries([]);
      setTransport("disconnected");
      return;
    }

    let disposed = false;
    let socket: WebSocket | null = null;
    let timer: ReturnType<typeof setTimeout> | null = null;
    let fallbackTimer: ReturnType<typeof setTimeout> | null = null;
    let activeTransport: RealtimeTransport = "disconnected";

    const clearPollTimer = () => {
      if (timer) {
        clearTimeout(timer);
        timer = null;
      }
    };

    const closeSocket = () => {
      if (!socket) {
        return;
      }

      socket.onopen = null;
      socket.onmessage = null;
      socket.onerror = null;
      socket.onclose = null;
      socket.close();
      socket = null;
    };

    const loadMetricsByPolling = async () => {
      try {
        const resp = await metricsHistoryApi(taskId);
        if (disposed) {
          return;
        }

        setSeries(trimSeries(resp));
        if (activeTransport === "polling") {
          setTransport("polling");
        }
      } catch {
        if (disposed) {
          return;
        }

        setConnected(false);
        if (activeTransport === "polling") {
          setTransport("disconnected");
        }
      } finally {
        if (!disposed && activeTransport === "polling") {
          timer = setTimeout(loadMetricsByPolling, POLL_INTERVAL_MS);
        }
      }
    };

    const startPollingFallback = (delay = 0) => {
      if (disposed || activeTransport === "polling") {
        return;
      }

      activeTransport = "polling";
      setConnected(false);
      setTransport("polling");
      closeSocket();
      clearPollTimer();
      timer = setTimeout(loadMetricsByPolling, delay);
    };

    const connectWebSocket = () => {
      const wsUrl = getWebSocketUrl(taskId);
      if (!wsUrl) {
        startPollingFallback();
        return;
      }

      try {
        socket = new WebSocket(wsUrl);
      } catch {
        startPollingFallback();
        return;
      }

      fallbackTimer = setTimeout(() => {
        if (!disposed && activeTransport !== "websocket") {
          startPollingFallback();
        }
      }, WS_FALLBACK_TIMEOUT_MS);

      socket.onopen = () => {
        if (disposed) {
          return;
        }

        activeTransport = "websocket";
        clearPollTimer();
        if (fallbackTimer) {
          clearTimeout(fallbackTimer);
          fallbackTimer = null;
        }
        setConnected(true);
        setTransport("websocket");
      };

      socket.onmessage = (event) => {
        const payload = parseSocketMessage(event.data);
        if (!payload || disposed) {
          return;
        }

        const incoming = extractPoints(payload);
        if (!incoming.length) {
          return;
        }

        setSeries((current) => mergeSeries(current, incoming));
      };

      socket.onerror = () => {
        if (!disposed) {
          startPollingFallback();
        }
      };

      socket.onclose = () => {
        if (disposed) {
          return;
        }

        setConnected(false);
        if (activeTransport === "websocket") {
          activeTransport = "disconnected";
        }
        startPollingFallback(1000);
      };
    };

    setConnected(false);
    setSeries([]);
    setTransport("disconnected");

    void metricsHistoryApi(taskId)
      .then((resp) => {
        if (!disposed) {
          setSeries(trimSeries(resp));
        }
      })
      .catch(() => {
        // Keep the last successful history snapshot.
      });

    connectWebSocket();

    return () => {
      disposed = true;
      clearPollTimer();
      if (fallbackTimer) {
        clearTimeout(fallbackTimer);
      }
      closeSocket();
      setConnected(false);
    };
  }, [taskId, resetKey]);

  return { connected, series, transport };
};
