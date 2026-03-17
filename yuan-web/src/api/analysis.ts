import type { AnalysisReport, AnalysisRule, AnalysisRulePayload } from "../types/analysis";
import { request, type ApiResponse } from "../utils/request";

export const listRulesApi = async (): Promise<AnalysisRule[]> => {
  const { data } = await request.get<ApiResponse<AnalysisRule[]>>("/analysis/rules");
  return data.data;
};

export const createRuleApi = async (payload: AnalysisRulePayload): Promise<AnalysisRule> => {
  const { data } = await request.post<ApiResponse<AnalysisRule>>("/analysis/rules", payload);
  return data.data;
};

export const updateRuleApi = async (ruleId: number, payload: AnalysisRulePayload): Promise<AnalysisRule> => {
  const { data } = await request.put<ApiResponse<AnalysisRule>>(`/analysis/rules/${ruleId}`, payload);
  return data.data;
};

export const switchRuleApi = async (ruleId: number, enabled: boolean): Promise<void> => {
  await request.patch<ApiResponse<null>>(`/analysis/rules/${ruleId}/switch`, { enabled });
};

export const deleteRuleApi = async (ruleId: number): Promise<void> => {
  await request.delete<ApiResponse<null>>(`/analysis/rules/${ruleId}`);
};

export const getLatestAnalysisReportApi = async (taskId: number): Promise<AnalysisReport> => {
  const { data } = await request.get<ApiResponse<AnalysisReport>>(`/analysis/reports/tasks/${taskId}/latest`);
  return data.data;
};

export const getAnalysisReportByRunApi = async (runId: number): Promise<AnalysisReport> => {
  const { data } = await request.get<ApiResponse<AnalysisReport>>(`/analysis/reports/runs/${runId}`);
  return data.data;
};
