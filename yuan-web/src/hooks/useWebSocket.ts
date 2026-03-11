import { useEffect, useState } from "react";
import { metricsHistoryApi } from "../api/test";
import type { RealtimeMetricPoint } from "../types/test";

export const useWebSocket = (taskId: number) => {
  const [connected, setConnected] = useState(false);
  const [series, setSeries] = useState<RealtimeMetricPoint[]>([]);

  useEffect(() => {
    let disposed = false;
    let timer: ReturnType<typeof setInterval> | null = null;

    const loadMetrics = async () => {
      try {
        const resp = await metricsHistoryApi(taskId);
        if (disposed) return;
        setSeries(resp);
        setConnected(true);
      } catch {
        if (disposed) return;
        setConnected(false);
        setSeries([]);
      }
    };

    setConnected(false);
    setSeries([]);
    loadMetrics();
    timer = setInterval(loadMetrics, 3000);

    return () => {
      disposed = true;
      if (timer) {
        clearInterval(timer);
      }
      setConnected(false);
    };
  }, [taskId]);

  return { connected, series };
};
