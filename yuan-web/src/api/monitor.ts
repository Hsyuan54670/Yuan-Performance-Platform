import type { AlertRecord, AlertRule, AlertRulePayload, MetricsSummary, SystemMetric } from "../types/monitor";
import { request } from "../utils/request";

export const getSystemMetricsApi = async (): Promise<SystemMetric> => {
  const response = await request.get("/monitor/system-metrics");
  return response.data.data;
};

export const getMetricsSummaryApi = async (): Promise<MetricsSummary> => {
  const response = await request.get("/monitor/metrics-summary");
  return response.data.data;
};

export const listAlertRulesApi = async (): Promise<AlertRule[]> => {
  const response = await request.get("/monitor/alert-rules");
  return response.data.data;
};

export const createAlertRuleApi = async (payload: AlertRulePayload): Promise<AlertRule> => {
  const response = await request.post("/monitor/alert-rules", payload);
  return response.data.data;
};

export const updateAlertRuleApi = async (ruleId: number, payload: AlertRulePayload): Promise<AlertRule> => {
  const response = await request.put(`/monitor/alert-rules/${ruleId}`, payload);
  return response.data.data;
};

export const switchAlertRuleApi = async (ruleId: number, enabled: boolean): Promise<boolean> => {
  await request.patch(`/monitor/alert-rules/${ruleId}/switch`, { enabled });
  return true;
};

export const deleteAlertRuleApi = async (ruleId: number): Promise<boolean> => {
  await request.delete(`/monitor/alert-rules/${ruleId}`);
  return true;
};

export const listAlertRecordsApi = async (): Promise<AlertRecord[]> => {
  const response = await request.get("/monitor/alert-records");
  return response.data.data;
};
