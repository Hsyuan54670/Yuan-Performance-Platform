export interface SystemMetric {
  cpu: number;
  memory: number;
  disk: number;
  networkIn: number;
  networkOut: number;
}

export interface MetricsSummary {
  taskId: number | null;
  runId: number | null;
  status: string;
  qps: number;
  p99: number;
  errorRate: number;
  timestamp: string | null;
}

export interface AlertRule {
  id: number;
  name: string;
  metric: "CPU" | "MEMORY" | "P99" | "ERROR_RATE";
  op: ">" | ">=";
  threshold: number;
  level: "INFO" | "WARN" | "CRITICAL";
  enabled: boolean;
}

export interface AlertRecord {
  id: number;
  taskId: number;
  ruleName: string;
  level: "INFO" | "WARN" | "CRITICAL";
  currentValue: number;
  createdAt: string;
}
