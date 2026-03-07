import { useEffect, useState } from "react";
import { randomInRange } from "../utils/format";
import type { RealtimeMetricPoint } from "../types/test";

const nowLabel = (): string => new Date().toLocaleTimeString("zh-CN", { hour12: false });

export const useWebSocket = (taskId: number) => {
  const [connected, setConnected] = useState(false);
  const [series, setSeries] = useState<RealtimeMetricPoint[]>([]);

  useEffect(() => {
    setConnected(true);
    setSeries([]);

    const timer = setInterval(() => {
      setSeries((prev) => {
        const next: RealtimeMetricPoint = {
          time: nowLabel(),
          qps: randomInRange(980, 1460),
          p50: randomInRange(80, 220),
          p90: randomInRange(260, 700),
          p99: randomInRange(800, 1800),
          errorRate: randomInRange(0.1, 5.8)
        };
        return [...prev.slice(-39), next];
      });
    }, 1000);

    return () => {
      clearInterval(timer);
      setConnected(false);
    };
  }, [taskId]);

  return { connected, series };
};
