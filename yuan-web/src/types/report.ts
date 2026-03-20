import type { Grade } from "./analysis";

export interface ReportSummary {
  id: number;
  taskId: number;
  runId: number;
  createdAt: string;
  grade: Grade;
  score: number;
  status: string;
  source: string;
  summary: string;
}
