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

export interface RunSystemMetricSummary {
  runId: number;
  avgCpu: number;
  avgMemory: number;
  peakCpu: number;
  peakMemory: number;
  highCpuSeconds: number;
  highMemorySeconds: number;
  sampledSeconds: number;
  missingSeconds: number;
}

export interface RunSystemMetricPoint {
  ts: string;
  cpu: number | null;
  cpuMax: number | null;
  memory: number | null;
  memoryMax: number | null;
  sampleCount: number;
  missing: boolean;
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

export type AlertRulePayload = Omit<AlertRule, "id">;

export interface AlertRecord {
  id: number;
  taskId: number;
  runId: number;
  ruleName: string;
  level: "INFO" | "WARN" | "CRITICAL";
  eventType: "TRIGGER" | "RECOVER";
  currentValue: number;
  createdAt: string;
}

export interface ActiveAlert {
  ruleId: number;
  taskId: number;
  runId: number;
  ruleName: string;
  metric: AlertRule["metric"];
  op: AlertRule["op"];
  threshold: number;
  level: AlertRule["level"];
  latestValue: number;
  latestTriggeredAt: string;
}
