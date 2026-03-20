import type { AnalysisReport } from "../types/analysis";
import type { ReportSummary } from "../types/report";
import { request, type ApiResponse } from "../utils/request";

export interface ReportQueryParams {
  taskId?: number;
  grade?: string;
  createdFrom?: string;
  createdTo?: string;
}

export const listReportsApi = async (params?: ReportQueryParams): Promise<ReportSummary[]> => {
  const { data } = await request.get<ApiResponse<ReportSummary[]>>("/report/reports", {
    params
  });
  return data.data ?? [];
};

export const getReportByRunApi = async (runId: number): Promise<AnalysisReport> => {
  const { data } = await request.get<ApiResponse<AnalysisReport>>(`/report/reports/runs/${runId}`);
  return data.data;
};

export const getReportHtmlApi = async (runId: number): Promise<string> => {
  const { data } = await request.get<string>(`/report/reports/runs/${runId}/html`, {
    responseType: "text"
  });
  return data;
};
