export interface ReportItem {
  id: number;
  taskId: number;
  title: string;
  createdAt: string;
  grade: "A" | "B" | "C" | "D" | "E";
  summary: string;
}

export interface ComparePoint {
  label: string;
  baseline: number;
  current: number;
}
