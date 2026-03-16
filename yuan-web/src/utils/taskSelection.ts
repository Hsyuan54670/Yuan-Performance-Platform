import type { TestTask } from "../types/test";

export const resolveActiveTaskId = (tasks: TestTask[], activeTaskId?: number) => {
  if (!tasks.length) {
    return undefined;
  }

  if (activeTaskId && tasks.some((task) => task.id === activeTaskId)) {
    return activeTaskId;
  }

  return tasks[0].id;
};
