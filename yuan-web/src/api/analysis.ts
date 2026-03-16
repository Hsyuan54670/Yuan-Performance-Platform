import { analysisRules } from "../mock/data";
import type { AnalysisReport, AnalysisRule } from "../types/analysis";
import { request, type ApiResponse, withMockDelay } from "../utils/request";

// 规则管理后端还未接通，当前页面继续使用 mock 数据占位。
export const listRulesApi = async (): Promise<AnalysisRule[]> => withMockDelay(analysisRules, 200);

export const getLatestAnalysisReportApi = async (taskId: number): Promise<AnalysisReport> => {
  const { data } = await request.get<ApiResponse<AnalysisReport>>(`/analysis/reports/tasks/${taskId}/latest`);
  return data.data;
};

export const getAnalysisReportByRunApi = async (runId: number): Promise<AnalysisReport> => {
  const { data } = await request.get<ApiResponse<AnalysisReport>>(`/analysis/reports/runs/${runId}`);
  return data.data;
};
