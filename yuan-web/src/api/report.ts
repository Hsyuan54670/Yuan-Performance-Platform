import { comparePoints, reports } from "../mock/data";
import type { ComparePoint, ReportItem } from "../types/report";
import { withMockDelay } from "../utils/request";

export const listReportsApi = async (): Promise<ReportItem[]> => withMockDelay(reports, 220);

export const exportReportApi = async (): Promise<{ success: boolean }> => withMockDelay({ success: true }, 420);

export const compareReportsApi = async (): Promise<ComparePoint[]> => withMockDelay(comparePoints, 260);
