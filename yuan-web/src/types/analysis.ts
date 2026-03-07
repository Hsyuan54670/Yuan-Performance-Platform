export type Grade = "A" | "B" | "C" | "D" | "E";

export interface BottleneckItem {
  time: string;
  type: "CPU" | "MEMORY" | "DATABASE" | "NETWORK" | "CONNECTION_POOL";
  reason: string;
  severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
}

export interface SuggestionItem {
  id: number;
  priority: "P0" | "P1" | "P2";
  title: string;
  detail: string;
}

export interface AnalysisReport {
  taskId: number;
  grade: Grade;
  score: number;
  summary: string;
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
