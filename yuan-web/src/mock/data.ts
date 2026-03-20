import type { LoginResponse } from "../types/auth";
import type { AnalysisReport, AnalysisRule } from "../types/analysis";
import type { AlertRecord, AlertRule, SystemMetric } from "../types/monitor";
import type { TestPlan, TestScene, TestTask } from "../types/test";

export const mockLogin: LoginResponse = {
  token: "mock-access-token",
  refreshToken: "mock-refresh-token",
  user: {
    id: 1,
    username: "admin",
    nickname: "Ops Lead",
    role: "ADMIN",
    roles: ["ADMIN"],
    permissions: [],
    menus: []
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
    createdAt: "2026-03-07T09:00:00",
    taskCount: 2,
    relatedSceneNames: ["Purchase Flow", "Coupon Flow"]
  },
  {
    id: 102,
    name: "Search Latency Baseline",
    targetUrl: "https://api.demo.local/search",
    concurrency: 120,
    duration: 480,
    rampType: "LINEAR",
    createdAt: "2026-03-06T14:30:00",
    taskCount: 1,
    relatedSceneNames: ["Read-Heavy Search"]
  }
];

export const testScenes: TestScene[] = [
  {
    id: 201,
    name: "Purchase Flow",
    createdAt: "2026-03-07T09:10:00",
    taskCount: 2,
    relatedPlanNames: ["Checkout Peak Hour"],
    steps: [
      { id: 1, name: "Create Cart", method: "POST", path: "/cart", weight: 2 },
      { id: 2, name: "Set Address", method: "PUT", path: "/cart/address", weight: 1 },
      { id: 3, name: "Pay", method: "POST", path: "/order/pay", weight: 1 }
    ]
  },
  {
    id: 202,
    name: "Read-Heavy Search",
    createdAt: "2026-03-06T15:00:00",
    taskCount: 1,
    relatedPlanNames: ["Search Latency Baseline"],
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
    ruleType: "ENGINE",
    name: "高延迟风险",
    expression: "summary.p99 > 2000 && feature.highLatencySeconds >= 3",
    bottleneckType: "HIGH_LATENCY",
    severity: "HIGH",
    priority: "P1",
    enabled: true
  },
  {
    id: 602,
    ruleType: "AI",
    name: "数据库方向优先解释",
    instruction: "当延迟和错误率同时升高时，优先从数据库和下游依赖方向分析根因，并给出排查顺序。",
    bottleneckType: "ROOT_CAUSE",
    severity: "MEDIUM",
    priority: "P1",
    enabled: true
  }
];

export const analysisReport: AnalysisReport = {
  taskId: 3001,
  runId: 9001,
  grade: "B",
  score: 83,
  summary: "Throughput is healthy but response-time tail and occasional error spikes need tuning.",
  status: "SUCCESS",
  source: "DATA_RULE",
  createdAt: "2026-03-07T10:12:00",
  bottlenecks: [
    {
      time: "运行摘要",
      type: "HIGH_LATENCY",
      reason: "Tail latency increased significantly during the run.",
      evidence: "p99=1520ms, baselineP99=980ms, p99Change=55.10%, highLatencySeconds=6",
      severity: "HIGH"
    },
    {
      time: "运行摘要",
      type: "ERROR_RATE",
      reason: "Transient server-side failures were observed.",
      evidence: "errorRate=1.78%, baselineErrorRate=0.35%, errorChange=408.57%, errorSpikeSeconds=4",
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
