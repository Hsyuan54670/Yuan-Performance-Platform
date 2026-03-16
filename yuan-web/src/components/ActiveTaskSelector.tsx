import { Select, Space, Tag, Typography } from "antd";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { TestTask } from "../types/test";

interface ActiveTaskSelectorProps {
  label: string;
  tasks: TestTask[];
  value?: number;
  loading?: boolean;
  onChange: (taskId: number) => void;
  width?: number;
}

const statusColorMap: Record<string, string> = {
  RUNNING: "green",
  SUCCESS: "blue",
  FAILED: "red",
  STOPPED: "default",
  PENDING: "gold"
};

function ActiveTaskSelector({ label, tasks, value, loading, onChange, width = 300 }: ActiveTaskSelectorProps) {
  const { t } = useTranslation();

  const currentTask = useMemo(() => tasks.find((task) => task.id === value), [tasks, value]);
  const options = useMemo(
    () =>
      tasks.map((task) => ({
        label: `#${task.id} ${task.planName}`,
        value: task.id
      })),
    [tasks]
  );

  const renderStatus = (status?: string) => {
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

  return (
    <Space direction="vertical" size={6}>
      <Typography.Text type="secondary">{label}</Typography.Text>
      <Select
        style={{ width, display: "block" }}
        loading={loading}
        value={value || undefined}
        options={options}
        onChange={onChange}
        placeholder={t("testTask.selectTaskFirst")}
        optionFilterProp="label"
        showSearch
      />
      {currentTask ? (
        <Space wrap>
          <Tag color="blue">#{currentTask.id}</Tag>
          <Tag>{currentTask.planName}</Tag>
          <Tag>{currentTask.sceneName || "--"}</Tag>
          <Tag color={statusColorMap[currentTask.status] ?? "default"}>{renderStatus(currentTask.status)}</Tag>
        </Space>
      ) : (
        <Typography.Text type="secondary">{t("common.noData")}</Typography.Text>
      )}
    </Space>
  );
}

export default ActiveTaskSelector;
