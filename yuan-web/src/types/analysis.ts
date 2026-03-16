export type Grade = "A" | "B" | "C" | "D" | "E";

export interface BottleneckItem {
  time: string;
  type: string;
  reason: string;
  evidence?: string;
  severity: string;
}

export interface SuggestionItem {
  id: number;
  priority: "P0" | "P1" | "P2";
  title: string;
  detail: string;
}

export interface AnalysisReport {
  taskId: number;
  runId: number;
  grade: Grade;
  score: number;
  summary: string;
  status: string;
  source: string;
  createdAt: string;
  bottlenecks: BottleneckItem[];
  suggestions: SuggestionItem[];
}

export interface AnalysisRule {
  id: number;
  name: string;
  expression: string;
  bottleneckType: string;
  severity: string;
  enabled: boolean;
}
