import type {
  RealtimeMetricPoint,
  TestPlan,
  TestPlanPayload,
  TestScene,
  TestScenePayload,
  TestTask,
  TestTaskCreatePayload,
  TestTaskRun
} from "../types/test";
import { request, type ApiResponse } from "../utils/request";

const normalizePlan = (plan: Partial<TestPlan>): TestPlan => ({
  id: Number(plan.id ?? 0),
  name: plan.name ?? "",
  targetUrl: plan.targetUrl ?? "",
  concurrency: Number(plan.concurrency ?? 0),
  duration: Number(plan.duration ?? 0),
  rampType: plan.rampType === "LINEAR" ? "LINEAR" : "STAIR",
  createdAt: plan.createdAt ?? "",
  taskCount: Number(plan.taskCount ?? 0),
  relatedSceneNames: Array.isArray(plan.relatedSceneNames) ? plan.relatedSceneNames : []
});

const normalizeScene = (scene: Partial<TestScene>): TestScene => ({
  id: Number(scene.id ?? 0),
  name: scene.name ?? "",
  createdAt: scene.createdAt ?? "",
  taskCount: Number(scene.taskCount ?? 0),
  relatedPlanNames: Array.isArray(scene.relatedPlanNames) ? scene.relatedPlanNames : [],
  steps: Array.isArray(scene.steps) ? scene.steps : []
});

export const listPlansApi = async (): Promise<TestPlan[]> => {
  const { data } = await request.get<ApiResponse<TestPlan[]>>("/test/plans");
  return (data.data ?? []).map((item) => normalizePlan(item));
};

export const createPlanApi = async (payload: TestPlanPayload): Promise<number> => {
  const { data } = await request.post<ApiResponse<number>>("/test/plans", payload);
  return data.data;
};

export const updatePlanApi = async (id: number, payload: TestPlanPayload): Promise<void> => {
  await request.put<ApiResponse<null>>(`/test/plans/${id}`, payload);
};

export const deletePlanApi = async (id: number): Promise<void> => {
  await request.delete<ApiResponse<null>>(`/test/plans/${id}`);
};

export const listScenesApi = async (): Promise<TestScene[]> => {
  const { data } = await request.get<ApiResponse<TestScene[]>>("/test/scenes");
  return (data.data ?? []).map((item) => normalizeScene(item));
};

export const createSceneApi = async (payload: TestScenePayload): Promise<number> => {
  const { data } = await request.post<ApiResponse<number>>("/test/scenes", payload);
  return data.data;
};

export const updateSceneApi = async (id: number, payload: TestScenePayload): Promise<void> => {
  await request.put<ApiResponse<null>>(`/test/scenes/${id}`, payload);
};

export const deleteSceneApi = async (id: number): Promise<void> => {
  await request.delete<ApiResponse<null>>(`/test/scenes/${id}`);
};

export const listTasksApi = async (): Promise<TestTask[]> => {
  const { data } = await request.get<ApiResponse<TestTask[]>>("/test/tasks");
  return data.data;
};

export const createTaskApi = async (payload: TestTaskCreatePayload): Promise<number> => {
  const { data } = await request.post<ApiResponse<number>>("/test/tasks", payload);
  return data.data;
};

export const listTaskRunsApi = async (taskId: number): Promise<TestTaskRun[]> => {
  const { data } = await request.get<ApiResponse<TestTaskRun[]>>(`/test/tasks/${taskId}/runs`);
  return data.data;
};

export const startTaskApi = async (id: number): Promise<void> => {
  await request.post<ApiResponse<null>>(`/test/tasks/${id}/start`);
};

export const stopTaskApi = async (id: number): Promise<void> => {
  await request.post<ApiResponse<null>>(`/test/tasks/${id}/stop`);
};

export const metricsHistoryApi = async (id: number): Promise<RealtimeMetricPoint[]> => {
  const { data } = await request.get<ApiResponse<RealtimeMetricPoint[]>>(`/test/tasks/${id}/metrics`);
  return data.data;
};

export const runMetricsApi = async (runId: number): Promise<RealtimeMetricPoint[]> => {
  const { data } = await request.get<ApiResponse<RealtimeMetricPoint[]>>(`/test/runs/${runId}/metrics`);
  return data.data;
};
