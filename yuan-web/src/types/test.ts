export type TaskStatus = "RUNNING" | "PENDING" | "SUCCESS" | "FAILED" | "STOPPED";

export interface TestPlan {
  id: number;
  name: string;
  targetUrl: string;
  concurrency: number;
  duration: number;
  rampType: "STAIR" | "LINEAR";
  createdAt: string;
  taskCount: number;
  relatedSceneNames?: string[];
}

export interface TestPlanPayload {
  name: string;
  targetUrl: string;
  concurrency: number;
  duration: number;
  rampType: "STAIR" | "LINEAR";
}

export interface SceneStep {
  id: number;
  name: string;
  method: "GET" | "POST" | "PUT" | "DELETE";
  path: string;
  weight: number;
}

export interface SceneStepPayload {
  name: string;
  method: "GET" | "POST" | "PUT" | "DELETE";
  path: string;
  weight: number;
}

export interface TestScene {
  id: number;
  name: string;
  createdAt: string;
  taskCount: number;
  relatedPlanNames?: string[];
  steps: SceneStep[];
}

export interface TestScenePayload {
  name: string;
  steps: SceneStepPayload[];
}

export interface TestTaskCreatePayload {
  planId: number;
  sceneId: number;
}

export interface TestTask {
  id: number;
  planName: string;
  sceneName: string;
  status: TaskStatus;
  startTime: string | null;
  duration: number;
  qps: number;
  p99: number;
  errorRate: number;
}

export interface TestTaskRun {
  id: number;
  taskId: number;
  status: TaskStatus;
  startTime: string | null;
  endTime: string | null;
  createdAt: string | null;
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
