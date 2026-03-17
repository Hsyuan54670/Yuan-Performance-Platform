export type Grade = "A" | "B" | "C" | "D" | "E";
export type AnalysisRuleType = "ENGINE" | "AI";
export type AnalysisRulePriority = "P0" | "P1" | "P2";
export type AnalysisRuleSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

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
  ruleType: AnalysisRuleType;
  name: string;
  expression?: string;
  instruction?: string;
  bottleneckType: string;
  severity: AnalysisRuleSeverity;
  priority: AnalysisRulePriority;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AnalysisRulePayload {
  ruleType: AnalysisRuleType;
  name: string;
  expression?: string;
  instruction?: string;
  bottleneckType: string;
  severity: AnalysisRuleSeverity;
  priority: AnalysisRulePriority;
  enabled: boolean;
}
