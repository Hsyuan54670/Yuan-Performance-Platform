import type { TestTask, TestTaskRun } from "../types/test";

export const resolveActiveTaskId = (tasks: TestTask[], activeTaskId?: number) => {
  if (!tasks.length) {
    return undefined;
  }

  if (activeTaskId && tasks.some((task) => task.id === activeTaskId)) {
    return activeTaskId;
  }

  return tasks[0].id;
};

export const resolveActiveRunId = (runs: TestTaskRun[], activeRunId?: number) => {
  if (!runs.length) {
    return undefined;
  }

  if (activeRunId && runs.some((run) => run.id === activeRunId)) {
    return activeRunId;
  }

  return runs[0].id;
};
