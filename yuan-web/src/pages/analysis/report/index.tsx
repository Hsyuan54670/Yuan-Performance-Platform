import { ReloadOutlined } from "@ant-design/icons";
import { Card, Col, Empty, Row, Space, Spin, Statistic, Tag, Typography, message } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { getAnalysisReportByRunApi } from "../../../api/analysis";
import { listTaskRunsApi, listTasksApi } from "../../../api/test";
import ActiveRunSelector from "../../../components/ActiveRunSelector";
import ActiveTaskSelector from "../../../components/ActiveTaskSelector";
import BottleneckTimeline from "../../../components/BottleneckTimeline";
import OptimizationSuggestionList from "../../../components/OptimizationSuggestionList";
import { useAppStore } from "../../../store/appStore";
import type { AnalysisReport } from "../../../types/analysis";
import type { TestTask, TestTaskRun } from "../../../types/test";
import { getRequestErrorMessage } from "../../../utils/request";
import { renderTaskStatus, taskStatusColorMap } from "../../../utils/taskStatus";
import { resolveActiveRunId, resolveActiveTaskId } from "../../../utils/taskSelection";

function AnalysisReportPage() {
  const [report, setReport] = useState<AnalysisReport | null>(null);
  const [tasks, setTasks] = useState<TestTask[]>([]);
  const [runs, setRuns] = useState<TestTaskRun[]>([]);
  const [tasksLoading, setTasksLoading] = useState(true);
  const [runsLoading, setRunsLoading] = useState(false);
  const [reportLoading, setReportLoading] = useState(false);
  const [reportError, setReportError] = useState<string>();
  const { activeTaskId, activeRunId, setActiveTaskId, setActiveRunId } = useAppStore();
  const { t } = useTranslation();

  const selectedTask = useMemo(() => tasks.find((item) => item.id === activeTaskId), [tasks, activeTaskId]);
  const selectedRun = useMemo(() => runs.find((item) => item.id === activeRunId), [runs, activeRunId]);

  const loadTasks = useCallback(async () => {
    setTasksLoading(true);
    try {
      const taskList = await listTasksApi();
      setTasks(taskList);
      const resolvedTaskId = resolveActiveTaskId(taskList, activeTaskId);
      if (!resolvedTaskId) {
        setActiveTaskId(0);
        setRuns([]);
        return;
      }
      if (resolvedTaskId !== activeTaskId) {
        setActiveTaskId(resolvedTaskId);
      }
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.loadFailed")));
      setTasks([]);
    } finally {
      setTasksLoading(false);
    }
  }, [activeTaskId, setActiveTaskId, t]);

  const loadRuns = useCallback(
    async (taskId: number) => {
      setRunsLoading(true);
      try {
        const runList = await listTaskRunsApi(taskId);
        setRuns(runList);
        const resolvedRunId = resolveActiveRunId(runList, activeRunId);
        if (!resolvedRunId) {
          setActiveRunId(0);
          return;
        }
        if (resolvedRunId !== activeRunId) {
          setActiveRunId(resolvedRunId);
        }
      } catch (error) {
        setRuns([]);
        setActiveRunId(0);
        message.error(getRequestErrorMessage(error, t("common.loadFailed")));
      } finally {
        setRunsLoading(false);
      }
    },
    [activeRunId, setActiveRunId, t]
  );

  const loadReport = useCallback(
    async (runId: number, silent = false) => {
      setReportLoading(true);
      setReportError(undefined);
      try {
        const data = await getAnalysisReportByRunApi(runId);
        setReport(data);
        return true;
      } catch (error) {
        setReport(null);
        const msg = getRequestErrorMessage(error, t("common.loadFailed"));
        const noReport = msg === "Report not found";
        setReportError(noReport ? t("analysisReport.noReport") : msg);
        if (!silent && !noReport) {
          message.error(msg);
        }
        return false;
      } finally {
        setReportLoading(false);
      }
    },
    [t]
  );

  useEffect(() => {
    void loadTasks();
  }, [loadTasks]);

  useEffect(() => {
    if (!activeTaskId) {
      setRuns([]);
      setActiveRunId(0);
      return;
    }
    void loadRuns(activeTaskId);
  }, [activeTaskId, loadRuns, setActiveRunId]);

  useEffect(() => {
    if (!activeRunId) {
      setReport(null);
      setReportError(undefined);
      return;
    }
    void loadReport(activeRunId, true);
  }, [activeRunId, loadReport]);


  const formatDateTime = (value?: string) => {
    if (!value) {
      return "--";
    }
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return value;
    }
    return new Intl.DateTimeFormat(undefined, {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit"
    }).format(parsed);
  };

  const renderSource = (value?: string) => {
    if (value === "DATA_RULE") {
      return t("analysisReport.sourceDataRule");
    }
    return value || "--";
  };

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space direction="vertical" size={16} style={{ width: "100%" }}>
              <div>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("analysisReport.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("analysisReport.subtitle")}</Typography.Text>
              </div>
              <Space style={{ width: "100%", justifyContent: "space-between" }} wrap align="start">
                <Space wrap align="start" size={16}>
                  <ActiveTaskSelector
                    label={t("analysisReport.currentTask")}
                    tasks={tasks}
                    value={activeTaskId}
                    loading={tasksLoading}
                    onChange={setActiveTaskId}
                  />
                  <ActiveRunSelector
                    label={t("analysisReport.currentRun")}
                    runs={runs}
                    value={activeRunId}
                    loading={runsLoading}
                    onChange={setActiveRunId}
                  />
                </Space>

              </Space>
            </Space>
          </Card>
        </Col>

        <Col span={24}>
          <Card className="glass-card" title={t("analysisReport.detailTitle")} style={{ borderRadius: 18 }}>
            {reportLoading ? (
              <div style={{ minHeight: 260, display: "grid", placeItems: "center" }}>
                <Spin tip={t("analysisReport.loading")} />
              </div>
            ) : !activeTaskId ? (
              <Empty description={t("testTask.selectTaskFirst")} />
            ) : !activeRunId ? (
              <Empty description={t("common.selectRunFirst")} />
            ) : !report ? (
              <Empty description={selectedRun?.status === "RUNNING" ? t("analysisReport.pendingForRun") : reportError || t("analysisReport.noReport")} />
            ) : (
              <Space direction="vertical" size={18} style={{ width: "100%" }}>
                <div>
                  <Typography.Text type="secondary">{t("analysisReport.currentViewing")}</Typography.Text>
                  <Typography.Title level={4} style={{ margin: 0 }}>
                    {selectedTask?.planName || "--"}
                  </Typography.Title>
                  <Space wrap style={{ marginTop: 8 }}>
                    <Tag color="blue">{t("analysisReport.taskTag", { taskId: selectedTask?.id || report.taskId })}</Tag>
                    <Tag color="geekblue">run #{report.runId}</Tag>
                    <Tag>{selectedTask?.sceneName || "--"}</Tag>
                    <Tag color={taskStatusColorMap[report.status] ?? "default"}>{renderTaskStatus(t, report.status)}</Tag>
                  </Space>
                </div>

                <Row gutter={[12, 12]}>
                  <Col xs={12} md={6}>
                    <Statistic title={t("analysisReport.task")} value={selectedTask?.id || report.taskId} />
                  </Col>
                  <Col xs={12} md={6}>
                    <Statistic title={t("analysisReport.runId")} value={report.runId} />
                  </Col>
                  <Col xs={12} md={6}>
                    <Statistic title={t("analysisReport.grade")} value={report.grade} valueStyle={{ color: report.grade === "A" ? "#2b8a3e" : report.grade === "D" ? "#c92a2a" : "#e67700" }} />
                  </Col>
                  <Col xs={12} md={6}>
                    <Statistic title={t("analysisReport.score")} value={report.score} suffix="/100" />
                  </Col>
                </Row>

                <Space wrap>
                  <Tag>{selectedTask?.planName || "--"}</Tag>
                  <Tag color="processing">{renderSource(report.source)}</Tag>
                  <Tag>{`${t("analysisReport.createdAt")}: ${formatDateTime(report.createdAt)}`}</Tag>
                </Space>

                <Typography.Paragraph style={{ marginBottom: 0 }}>
                  {report.summary}
                </Typography.Paragraph>
              </Space>
            )}
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <BottleneckTimeline items={report?.bottlenecks ?? []} />
        </Col>
        <Col xs={24} lg={12}>
          <OptimizationSuggestionList items={report?.suggestions ?? []} />
        </Col>
      </Row>
    </div>
  );
}

export default AnalysisReportPage;


