import type { RealtimeMetricPoint, TestPlan, TestScene, TestTask } from "../types/test";
import { request, type ApiResponse, withMockDelay } from "../utils/request";
import { testPlans, testScenes, testTasks } from "../mock/data";

export const listPlansApi = async (): Promise<TestPlan[]> => {
  const { data } = await request.get<ApiResponse<TestPlan[]>>("/test/plans");
  return data.data;
};

export const listScenesApi = async (): Promise<TestScene[]> => withMockDelay(testScenes);

export const listTasksApi = async (): Promise<TestTask[]> => withMockDelay(testTasks);

export const startTaskApi = async (): Promise<{ success: boolean }> => withMockDelay({ success: true }, 380);

export const stopTaskApi = async (): Promise<{ success: boolean }> => withMockDelay({ success: true }, 280);

export const metricsHistoryApi = async (): Promise<RealtimeMetricPoint[]> => withMockDelay([], 100);
