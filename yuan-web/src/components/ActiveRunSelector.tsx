import { Select, Space, Tag, Typography } from "antd";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { TestTaskRun } from "../types/test";
import { formatDateTime } from "../utils/format";
import { renderTaskStatus, taskStatusColorMap } from "../utils/taskStatus";

interface ActiveRunSelectorProps {
  label: string;
  runs: TestTaskRun[];
  value?: number;
  loading?: boolean;
  onChange: (runId: number) => void;
  width?: number;
}

function ActiveRunSelector({ label, runs, value, loading, onChange, width = 260 }: ActiveRunSelectorProps) {
  const { t } = useTranslation();

  const currentRun = useMemo(() => runs.find((run) => run.id === value), [runs, value]);
  const options = useMemo(
    () =>
      runs.map((run) => ({
        label: `#${run.id} ${run.status} ${run.startTime ? formatDateTime(run.startTime) : ""}`.trim(),
        value: run.id
      })),
    [runs]
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
        placeholder={t("runSelector.placeholder")}
        optionFilterProp="label"
        showSearch
      />
      {currentRun ? (
        <Space wrap>
          <Tag color="geekblue">#{currentRun.id}</Tag>
          <Tag color={taskStatusColorMap[currentRun.status] ?? "default"}>{renderTaskStatus(t, currentRun.status)}</Tag>
          {currentRun.endTime ? <Tag>{`${t("runSelector.endedAt")}: ${formatDateTime(currentRun.endTime)}`}</Tag> : null}
        </Space>
      ) : (
        <Typography.Text type="secondary">{t("common.noData")}</Typography.Text>
      )}
    </Space>
  );
}

export default ActiveRunSelector;
