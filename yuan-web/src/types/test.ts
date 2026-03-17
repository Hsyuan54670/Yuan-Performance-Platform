export type TaskStatus = "RUNNING" | "PENDING" | "SUCCESS" | "FAILED" | "STOPPED";

export interface TestPlan {
  id: number;
  name: string;
  targetUrl: string;
  concurrency: number;
  duration: number;
  rampType: "STAIR" | "LINEAR";
  createdAt: string;
}

export interface SceneStep {
  id: number;
  name: string;
  method: "GET" | "POST" | "PUT" | "DELETE";
  path: string;
  weight: number;
}

export interface TestScene {
  id: number;
  name: string;
  steps: SceneStep[];
}

export interface TestTask {
  id: number;
  planName: string;
  sceneName: string;
  status: TaskStatus;
  startTime: string;
  duration: number;
  qps: number;
  p99: number;
  errorRate: number;
}

export interface TaskStatusPushMessage {
  messageType: "TASK_STATUS";
  taskId: number;
  runId?: number | null;
  status: TaskStatus;
  message?: string;
  timestamp?: string;
}

export interface RealtimeMetricPoint {
  time: string;
  qps: number;
  p50: number;
  p90: number;
  p99: number;
  errorRate: number;
}
