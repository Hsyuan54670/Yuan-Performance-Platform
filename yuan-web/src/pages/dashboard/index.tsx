import { ArrowRightOutlined, FireOutlined, RocketOutlined } from "@ant-design/icons";
import { Button, Card, Col, Empty, Row, Space, Statistic, Tag, Typography, message } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { getAnalysisReportByRunApi } from "../../api/analysis";
import { getRunSystemMetricSummaryApi } from "../../api/monitor";
import { listTaskRunsApi, listTasksApi, runMetricsApi } from "../../api/test";
import ActiveRunSelector from "../../components/ActiveRunSelector";
import ActiveTaskSelector from "../../components/ActiveTaskSelector";
import PerformanceChart from "../../components/PerformanceChart";
import { useAuth } from "../../hooks/useAuth";
import { useWebSocket } from "../../hooks/useWebSocket";
import { useAppStore } from "../../store/appStore";
import type { AnalysisReport } from "../../types/analysis";
import type { RunSystemMetricSummary } from "../../types/monitor";
import type { TaskStatusPushMessage, TestTask, TestTaskRun } from "../../types/test";
import { formatDateTime } from "../../utils/format";
import { PermissionCodes } from "../../utils/permissions";
import { getRequestErrorMessage } from "../../utils/request";
import { renderTaskStatus, taskStatusColorMap } from "../../utils/taskStatus";
import { resolveActiveRunId, resolveActiveTaskId } from "../../utils/taskSelection";

function DashboardPage() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [tasks, setTasks] = useState<TestTask[]>([]);
  const [runs, setRuns] = useState<TestTaskRun[]>([]);
  const [tasksLoading, setTasksLoading] = useState(true);
  const [runsLoading, setRunsLoading] = useState(false);
  const [report, setReport] = useState<AnalysisReport | null>(null);
  const [resourceSummary, setResourceSummary] = useState<RunSystemMetricSummary | null>(null);
  const [briefLoading, setBriefLoading] = useState(false);
  const { activeTaskId, activeRunId, setActiveTaskId, setActiveRunId } = useAppStore();
  const { hasPermission } = useAuth();
  const canOpenTaskConsole = hasPermission(PermissionCodes.TEST_TASK_READ);
  const canOpenAnalysisReport = hasPermission(PermissionCodes.ANALYSIS_REPORT_VIEW);

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
      setTasks([]);
      message.error(getRequestErrorMessage(error, t("common.loadFailed")));
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

  const handleTaskStatus = useCallback(
    (statusMessage: TaskStatusPushMessage) => {
      setTasks((current) =>
        current.map((task) => {
          if (task.id !== statusMessage.taskId) {
            return task;
          }

          return {
            ...task,
            status: statusMessage.status,
            startTime: statusMessage.status === "RUNNING" && statusMessage.timestamp ? statusMessage.timestamp : task.startTime
          };
        })
      );

      if (!statusMessage.runId || statusMessage.taskId !== activeTaskId) {
        return;
      }

      const runId = statusMessage.runId;

      setRuns((current) => {
        const existing = current.find((run) => run.id === runId);
        const nextRun: TestTaskRun = {
          id: runId,
          taskId: statusMessage.taskId,
          status: statusMessage.status,
          startTime:
            statusMessage.status === "RUNNING"
              ? statusMessage.timestamp ?? existing?.startTime ?? existing?.createdAt ?? null
              : existing?.startTime ?? existing?.createdAt ?? statusMessage.timestamp ?? null,
          endTime: statusMessage.status === "RUNNING" ? null : statusMessage.timestamp ?? existing?.endTime ?? null,
          createdAt: existing?.createdAt ?? statusMessage.timestamp ?? null
        };

        if (!existing) {
          return [nextRun, ...current];
        }

        return current.map((run) => (run.id === runId ? { ...run, ...nextRun } : run));
      });

      if (statusMessage.status === "RUNNING") {
        setActiveRunId(runId);
      }
    },
    [activeTaskId, setActiveRunId]
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
      setResourceSummary(null);
      return;
    }

    let disposed = false;
    setBriefLoading(true);

    void Promise.allSettled([getAnalysisReportByRunApi(activeRunId), getRunSystemMetricSummaryApi(activeRunId)])
      .then((results) => {
        if (disposed) {
          return;
        }

        const [reportResult, resourceResult] = results;
        setReport(reportResult.status === "fulfilled" ? reportResult.value : null);
        setResourceSummary(resourceResult.status === "fulfilled" ? resourceResult.value : null);
      })
      .finally(() => {
        if (!disposed) {
          setBriefLoading(false);
        }
      });

    return () => {
      disposed = true;
    };
  }, [activeRunId]);

  const currentTask = useMemo(() => tasks.find((task) => task.id === activeTaskId), [tasks, activeTaskId]);
  const currentRun = useMemo(() => runs.find((run) => run.id === activeRunId), [runs, activeRunId]);
  const runningTaskCount = useMemo(() => tasks.filter((task) => task.status === "RUNNING").length, [tasks]);
  const historyLoader = useCallback(() => (activeRunId ? runMetricsApi(activeRunId) : Promise.resolve([])), [activeRunId]);
  const { series, transport } = useWebSocket({
    taskId: activeTaskId,
    resetKey: `${activeTaskId}-${activeRunId}`,
    onTaskStatus: handleTaskStatus,
    historyLoader,
    realtimeEnabled: currentRun?.status === "RUNNING"
  });

  const xAxis = useMemo(() => series.map((item) => item.time), [series]);
  const realtimeTagColor = transport === "websocket" ? "green" : transport === "polling" ? "gold" : "default";
  const realtimeLabel =
    transport === "websocket"
      ? t("dashboard.realtimeConnected")
      : transport === "polling"
        ? t("dashboard.realtimeFallback")
        : t("dashboard.realtimeDisconnected");

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space direction="vertical" size={16} style={{ width: "100%" }}>
              <div>
                <Typography.Title level={2} style={{ margin: 0 }}>
                  {t("dashboard.title")}
                </Typography.Title>
                <Typography.Text type="secondary">{t("dashboard.subtitle")}</Typography.Text>
              </div>
              <Space wrap style={{ width: "100%", justifyContent: "space-between" }} align="start">
                <Space wrap align="start" size={16}>
                  <ActiveTaskSelector
                    label={t("dashboard.currentTask")}
                    tasks={tasks}
                    value={activeTaskId}
                    loading={tasksLoading}
                    onChange={setActiveTaskId}
                  />
                  <ActiveRunSelector
                    label={t("dashboard.currentRun")}
                    runs={runs}
                    value={activeRunId}
                    loading={runsLoading}
                    onChange={setActiveRunId}
                  />
                </Space>
                <Space wrap>
                  {currentRun?.status === "RUNNING" ? <Tag color={realtimeTagColor}>{realtimeLabel}</Tag> : null}
                  {canOpenTaskConsole ? (
                    <Button type="primary" icon={<RocketOutlined />} onClick={() => navigate("/test/task")}>
                      {t("dashboard.runTask")}
                    </Button>
                  ) : null}
                  {canOpenAnalysisReport ? (
                    <Button icon={<ArrowRightOutlined />} onClick={() => navigate("/analysis/report")}>
                      {t("dashboard.openAiReport")}
                    </Button>
                  ) : null}
                </Space>
              </Space>
            </Space>
          </Card>
        </Col>

        <Col xs={24} md={6}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("dashboard.activeTasks")} value={runningTaskCount} prefix={<FireOutlined />} />
            <Typography.Text type="secondary">{t("dashboard.activeTasksDesc")}</Typography.Text>
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("dashboard.latestQps")} value={series.at(-1)?.qps ?? 0} suffix="req/s" precision={0} />
            <Typography.Text type="secondary">{currentRun ? `#${currentRun.id}` : t("dashboard.noRunSelected")}</Typography.Text>
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("dashboard.latestP99")} value={series.at(-1)?.p99 ?? 0} suffix="ms" precision={0} />
            <Typography.Text type="secondary">{currentTask?.planName || t("common.noData")}</Typography.Text>
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("dashboard.analysisScore")} value={report?.score} suffix={report ? "/100" : undefined} formatter={(value) => value ?? "--"} />
            <Typography.Text type="secondary">{report ? t("dashboard.analysisScoreDesc") : t("dashboard.noReport")}</Typography.Text>
          </Card>
        </Col>

        <Col xs={24} xl={16}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <PerformanceChart
              title={t("dashboard.chartTitle")}
              xAxis={xAxis}
              area
              yAxisName={t("dashboard.seriesQps")}
              secondaryYAxisName="ms / %"
              series={[
                { name: t("dashboard.seriesQps"), color: "#0b7285", data: series.map((item) => item.qps), yAxisIndex: 0 },
                { name: t("dashboard.seriesP99"), color: "#e8590c", data: series.map((item) => item.p99), yAxisIndex: 1 },
                { name: t("dashboard.seriesError"), color: "#c92a2a", data: series.map((item) => item.errorRate), yAxisIndex: 1 }
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} xl={8}>
          <Card className="glass-card" style={{ borderRadius: 18, minHeight: 420 }}>
            {!currentRun ? (
              <Empty description={t("dashboard.noRunSelected")} />
            ) : (
              <Space direction="vertical" size={16} style={{ width: "100%" }}>
                <div>
                  <Typography.Title level={4} style={{ margin: 0 }}>{t("dashboard.analysisSummary")}</Typography.Title>
                  <Typography.Text type="secondary">{currentTask?.planName || currentTask?.sceneName || "--"}</Typography.Text>
                </div>
                <Space wrap>
                  <Tag color="blue">#{currentTask?.id ?? "--"}</Tag>
                  <Tag color="geekblue">run #{currentRun.id}</Tag>
                  <Tag color={taskStatusColorMap[currentRun.status] ?? "default"}>{renderTaskStatus(t, currentRun.status)}</Tag>
                </Space>
                <div>
                  <Typography.Text type="secondary">{t("analysisReport.createdAt")}</Typography.Text>
                  <div>{currentRun.startTime ? formatDateTime(currentRun.startTime) : "--"}</div>
                </div>
                {briefLoading ? (
                  <Typography.Text type="secondary">{t("common.loading")}</Typography.Text>
                ) : report ? (
                  <Typography.Paragraph style={{ marginBottom: 0 }}>{report.summary}</Typography.Paragraph>
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={currentRun.status === "RUNNING" ? t("analysisReport.pendingForRun") : t("dashboard.noReport")} />
                )}
                <div>
                  <Typography.Title level={5} style={{ marginBottom: 12 }}>{t("dashboard.resourceSummary")}</Typography.Title>
                  <Row gutter={[12, 12]}>
                    <Col span={12}>
                      <Statistic title={t("dashboard.avgCpu")} value={resourceSummary?.avgCpu} suffix="%" precision={1} formatter={(value) => value ?? "--"} />
                    </Col>
                    <Col span={12}>
                      <Statistic title={t("dashboard.peakCpu")} value={resourceSummary?.peakCpu} suffix="%" precision={1} formatter={(value) => value ?? "--"} />
                    </Col>
                    <Col span={12}>
                      <Statistic title={t("dashboard.avgMemory")} value={resourceSummary?.avgMemory} suffix="%" precision={1} formatter={(value) => value ?? "--"} />
                    </Col>
                    <Col span={12}>
                      <Statistic title={t("dashboard.peakMemory")} value={resourceSummary?.peakMemory} suffix="%" precision={1} formatter={(value) => value ?? "--"} />
                    </Col>
                    <Col span={12}>
                      <Statistic title={t("dashboard.sampledSeconds")} value={resourceSummary?.sampledSeconds} formatter={(value) => value ?? "--"} />
                    </Col>
                    <Col span={12}>
                      <Statistic title={t("dashboard.missingSeconds")} value={resourceSummary?.missingSeconds} formatter={(value) => value ?? "--"} />
                    </Col>
                  </Row>
                </div>
              </Space>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default DashboardPage;
