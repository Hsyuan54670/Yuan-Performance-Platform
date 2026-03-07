import { alertRecords, alertRules, systemMetric } from "../mock/data";
import type { AlertRecord, AlertRule, SystemMetric } from "../types/monitor";
import { withMockDelay } from "../utils/request";

export const getSystemMetricsApi = async (): Promise<SystemMetric> => withMockDelay(systemMetric, 120);

export const listAlertRulesApi = async (): Promise<AlertRule[]> => withMockDelay(alertRules, 180);

export const listAlertRecordsApi = async (): Promise<AlertRecord[]> => withMockDelay(alertRecords, 220);
