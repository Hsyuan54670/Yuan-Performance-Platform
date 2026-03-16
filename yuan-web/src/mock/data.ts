import type { LoginResponse } from "../types/auth";
import type { AnalysisReport, AnalysisRule } from "../types/analysis";
import type { AlertRecord, AlertRule, SystemMetric } from "../types/monitor";
import type { ReportItem, ComparePoint } from "../types/report";
import type { TestPlan, TestScene, TestTask } from "../types/test";

export const mockLogin: LoginResponse = {
  token: "mock-access-token",
  refreshToken: "mock-refresh-token",
  user: {
    id: 1,
    username: "admin",
    nickname: "Ops Lead",
    role: "ADMIN"
  }
};

export const testPlans: TestPlan[] = [
  {
    id: 101,
    name: "Checkout Peak Hour",
    targetUrl: "https://api.demo.local/checkout",
    concurrency: 200,
    duration: 600,
    rampType: "STAIR",
    createdAt: "2026-03-07T09:00:00"
  },
  {
    id: 102,
    name: "Search Latency Baseline",
    targetUrl: "https://api.demo.local/search",
    concurrency: 120,
    duration: 480,
    rampType: "LINEAR",
    createdAt: "2026-03-06T14:30:00"
  }
];

export const testScenes: TestScene[] = [
  {
    id: 201,
    name: "Purchase Flow",
    steps: [
      { id: 1, name: "Create Cart", method: "POST", path: "/cart", weight: 2 },
      { id: 2, name: "Set Address", method: "PUT", path: "/cart/address", weight: 1 },
      { id: 3, name: "Pay", method: "POST", path: "/order/pay", weight: 1 }
    ]
  },
  {
    id: 202,
    name: "Read-Heavy Search",
    steps: [
      { id: 4, name: "Search List", method: "GET", path: "/search", weight: 5 },
      { id: 5, name: "Detail", method: "GET", path: "/product/detail", weight: 2 }
    ]
  }
];

export const testTasks: TestTask[] = [
  {
    id: 3001,
    planName: "Checkout Peak Hour",
    sceneName: "Purchase Flow",
    status: "RUNNING",
    startTime: "2026-03-07T10:00:00",
    duration: 600,
    qps: 1320,
    p99: 1420,
    errorRate: 1.78
  },
  {
    id: 3000,
    planName: "Search Latency Baseline",
    sceneName: "Read-Heavy Search",
    status: "SUCCESS",
    startTime: "2026-03-06T18:00:00",
    duration: 480,
    qps: 910,
    p99: 980,
    errorRate: 0.35
  }
];

export const systemMetric: SystemMetric = {
  cpu: 72.8,
  memory: 67.5,
  disk: 58.9,
  networkIn: 18.2,
  networkOut: 14.6
};

export const alertRules: AlertRule[] = [
  { id: 401, name: "CPU High", metric: "CPU", op: ">=", threshold: 80, level: "WARN", enabled: true },
  { id: 402, name: "Error Burst", metric: "ERROR_RATE", op: ">", threshold: 5, level: "CRITICAL", enabled: true },
  { id: 403, name: "P99 Slow", metric: "P99", op: ">", threshold: 2000, level: "WARN", enabled: false }
];

export const alertRecords: AlertRecord[] = [
  {
    id: 501,
    taskId: 3001,
    runId: 9001,
    ruleName: "CPU High",
    level: "WARN",
    eventType: "TRIGGER",
    currentValue: 82.4,
    createdAt: "2026-03-07T10:03:11"
  },
  {
    id: 502,
    taskId: 3001,
    runId: 9001,
    ruleName: "Error Burst",
    level: "CRITICAL",
    eventType: "TRIGGER",
    currentValue: 6.9,
    createdAt: "2026-03-07T10:05:44"
  }
];

export const analysisRules: AnalysisRule[] = [
  {
    id: 601,
    name: "CPU bottleneck",
    expression: "avg_cpu_usage > 80 and p99_response_time > 2000",
    bottleneckType: "CPU_BOTTLENECK",
    severity: "HIGH",
    enabled: true
  },
  {
    id: 602,
    name: "Connection pool",
    expression: "active_connections >= max_connections * 0.9 and error_rate > 5",
    bottleneckType: "CONNECTION_POOL_EXHAUSTION",
    severity: "HIGH",
    enabled: true
  }
];

export const analysisReport: AnalysisReport = {
  taskId: 3001,
  grade: "B",
  score: 83,
  summary: "Throughput is healthy but response-time tail and occasional error spikes need tuning.",
  bottlenecks: [
    {
      time: "10:03:15",
      type: "CPU",
      reason: "CPU exceeded 82% while P99 climbed above 1500 ms.",
      severity: "HIGH"
    },
    {
      time: "10:05:40",
      type: "CONNECTION_POOL",
      reason: "Connection pool saturation caused transient 5xx errors.",
      severity: "CRITICAL"
    }
  ],
  suggestions: [
    {
      id: 1,
      priority: "P0",
      title: "Increase DB connection pool with back-pressure",
      detail: "Raise max active connections and add queue timeout to avoid immediate failure under burst traffic."
    },
    {
      id: 2,
      priority: "P1",
      title: "Optimize checkout hot query",
      detail: "Add composite index for order lookup and verify query plan to reduce P99 latency."
    },
    {
      id: 3,
      priority: "P2",
      title: "Tune JVM GC and CPU limits",
      detail: "Adjust heap sizing and CPU quota to avoid contention during peak stage."
    }
  ]
};

export const reports: ReportItem[] = [
  {
    id: 701,
    taskId: 3001,
    title: "Checkout peak benchmark",
    createdAt: "2026-03-07T10:12:00",
    grade: "B",
    summary: "Stable throughput with tail-latency risk."
  },
  {
    id: 702,
    taskId: 3000,
    title: "Search baseline benchmark",
    createdAt: "2026-03-06T18:10:00",
    grade: "A",
    summary: "Fast and consistent under configured load."
  }
];

export const comparePoints: ComparePoint[] = [
  { label: "Avg QPS", baseline: 910, current: 1320 },
  { label: "P99(ms)", baseline: 980, current: 1420 },
  { label: "Error Rate(%)", baseline: 0.35, current: 1.78 },
  { label: "CPU Avg(%)", baseline: 61.2, current: 72.8 }
];

export const users = [
  { id: 1, username: "admin", nickname: "Ops Lead", role: "ADMIN", status: "ACTIVE" },
  { id: 2, username: "dev_01", nickname: "Backend Dev", role: "DEVELOPER", status: "ACTIVE" },
  { id: 3, username: "qa_01", nickname: "QA Owner", role: "TESTER", status: "ACTIVE" }
];

export const roles = [
  { id: 1, name: "ADMIN", description: "Full system permission" },
  { id: 2, name: "DEVELOPER", description: "Plan and run performance tests" },
  { id: 3, name: "TESTER", description: "Manage tasks and reports" }
];

export const menuTree = [
  { id: 1, name: "Dashboard", path: "/dashboard" },
  { id: 2, name: "Test", path: "/test/plan" },
  { id: 3, name: "Monitor", path: "/monitor/realtime" },
  { id: 4, name: "Analysis", path: "/analysis/report" },
  { id: 5, name: "Report", path: "/report" },
  { id: 6, name: "System", path: "/system/user" }
];




