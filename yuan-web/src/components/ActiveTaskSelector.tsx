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
          <Tag>{currentTask.sceneName || "--"}</Tag>
        </Space>
      ) : (
        <Typography.Text type="secondary">{t("common.noData")}</Typography.Text>
      )}
    </Space>
  );
}

export default ActiveTaskSelector;
