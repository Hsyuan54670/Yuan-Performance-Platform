import { testPlans, testScenes, testTasks } from "../mock/data";
import type { RealtimeMetricPoint, TestPlan, TestScene, TestTask } from "../types/test";
import { withMockDelay } from "../utils/request";

export const listPlansApi = async (): Promise<TestPlan[]> => withMockDelay(testPlans);

export const listScenesApi = async (): Promise<TestScene[]> => withMockDelay(testScenes);

export const listTasksApi = async (): Promise<TestTask[]> => withMockDelay(testTasks);

export const startTaskApi = async (): Promise<{ success: boolean }> => withMockDelay({ success: true }, 380);

export const stopTaskApi = async (): Promise<{ success: boolean }> => withMockDelay({ success: true }, 280);

export const metricsHistoryApi = async (): Promise<RealtimeMetricPoint[]> => withMockDelay([], 100);
