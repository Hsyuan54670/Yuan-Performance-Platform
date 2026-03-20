import type { TFunction } from "i18next";
import type { TaskStatus } from "../types/test";

export const taskStatusColorMap: Record<string, string> = {
  RUNNING: "green",
  SUCCESS: "blue",
  FAILED: "red",
  STOPPED: "default",
  PENDING: "gold"
};

export const renderTaskStatus = (t: TFunction, status?: TaskStatus | string | null) => {
  if (!status) {
    return "--";
  }
  if (status === "RUNNING") {
    return t("common.statusRunning");
  }
  if (status === "PENDING") {
    return t("common.statusPending");
  }
  if (status === "SUCCESS") {
    return t("common.statusSuccess");
  }
  if (status === "FAILED") {
    return t("common.statusFailed");
  }
  if (status === "STOPPED") {
    return t("common.statusStopped");
  }
  return status;
};
