import { analysisReport, analysisRules } from "../mock/data";
import type { AnalysisReport, AnalysisRule } from "../types/analysis";
import { withMockDelay } from "../utils/request";

export const triggerAnalysisApi = async (taskId: number): Promise<{ taskId: number; triggered: boolean }> =>
  withMockDelay({ taskId, triggered: true }, 300);

export const getAnalysisReportApi = async (): Promise<AnalysisReport> => withMockDelay(analysisReport, 280);

export const listRulesApi = async (): Promise<AnalysisRule[]> => withMockDelay(analysisRules, 200);
