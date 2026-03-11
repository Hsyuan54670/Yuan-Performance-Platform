import type { RealtimeMetricPoint, TestPlan, TestScene, TestTask } from "../types/test";
import { request, type ApiResponse } from "../utils/request";

export const listPlansApi = async (): Promise<TestPlan[]> => {
  const { data } = await request.get<ApiResponse<TestPlan[]>>("/test/plans");
  return data.data;
};

export const listScenesApi = async (): Promise<TestScene[]> => {
  const { data } = await request.get<ApiResponse<TestScene[]>>("/test/scenes");
  return data.data;
};

export const listTasksApi = async (): Promise<TestTask[]> => {
  const { data } = await request.get<ApiResponse<TestTask[]>>("/test/tasks");
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
