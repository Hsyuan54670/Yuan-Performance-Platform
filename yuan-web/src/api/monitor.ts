import { alertRecords, alertRules } from "../mock/data";
import type { AlertRecord, AlertRule, MetricsSummary, SystemMetric } from "../types/monitor";
import { request, withMockDelay } from "../utils/request";

export const getSystemMetricsApi = async (): Promise<SystemMetric> => {
  const response = await request.get("/monitor/system-metrics");
  return response.data.data;
};

export const getMetricsSummaryApi = async (): Promise<MetricsSummary> => {
  const response = await request.get("/monitor/metrics-summary");
  return response.data.data;
};

export const listAlertRulesApi = async (): Promise<AlertRule[]> => withMockDelay(alertRules, 180);

export const listAlertRecordsApi = async (): Promise<AlertRecord[]> => withMockDelay(alertRecords, 220);
