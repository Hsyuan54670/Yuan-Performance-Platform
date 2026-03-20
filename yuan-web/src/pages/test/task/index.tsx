import { PauseCircleOutlined, PlayCircleOutlined, PlusOutlined, WifiOutlined } from "@ant-design/icons";
import { Button, Card, Col, Empty, Row, Space, Statistic, Tag, Typography, message } from "antd";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { getAnalysisReportByRunApi } from "../../../api/analysis";
import { getSystemMetricsApi } from "../../../api/monitor";
import { listTaskRunsApi, listTasksApi, runMetricsApi, startTaskApi, stopTaskApi } from "../../../api/test";
import ActiveRunSelector from "../../../components/ActiveRunSelector";
import ActiveTaskSelector from "../../../components/ActiveTaskSelector";
import PerformanceChart from "../../../components/PerformanceChart";
import RealtimeMetricsPanel from "../../../components/RealtimeMetricsPanel";
import { useAuth } from "../../../hooks/useAuth";
import { useWebSocket } from "../../../hooks/useWebSocket";
import { useAppStore } from "../../../store/appStore";
import type { AnalysisReport } from "../../../types/analysis";
import type { SystemMetric } from "../../../types/monitor";
import type { TaskStatusPushMessage, TestTask, TestTaskRun } from "../../../types/test";
import { formatDateTime } from "../../../utils/format";
import { PermissionCodes } from "../../../utils/permissions";
import { getRequestErrorMessage } from "../../../utils/request";
import { renderTaskStatus, taskStatusColorMap } from "../../../utils/taskStatus";
import { resolveActiveRunId, resolveActiveTaskId } from "../../../utils/taskSelection";

function TestTaskPage() {
  const navigate = useNavigate();
  const [tasks, setTasks] = useState<TestTask[]>([]);
  const [runs, setRuns] = useState<TestTaskRun[]>([]);
  const [tasksLoading, setTasksLoading] = useState(true);
  const [runsLoading, setRunsLoading] = useState(false);
  const [report, setReport] = useState<AnalysisReport | null>(null);
  const [sysMetric, setSysMetric] = useState<SystemMetric>({
    cpu: 0,
    memory: 0,
    disk: 0,
    networkIn: 0,
    networkOut: 0
  });
  const { activeTaskId, activeRunId, setActiveTaskId, setActiveRunId } = useAppStore();
  const previousTransportRef = useRef<"websocket" | "polling" | "disconnected">("disconnected");
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const canCreateTask = hasPermission(PermissionCodes.TEST_TASK_CREATE);
  const canStartTask = hasPermission(PermissionCodes.TEST_TASK_START);
  const canStopTask = hasPermission(PermissionCodes.TEST_TASK_STOP);
  const canViewAnalysisReport = hasPermission(PermissionCodes.ANALYSIS_REPORT_VIEW);

  const loadTasks = useCallback(
    async (silent = false) => {
      if (!silent) {
        setTasksLoading(true);
      }
      try {
        const resp = await listTasksApi();
        setTasks(resp);
        const resolvedTaskId = resolveActiveTaskId(resp, activeTaskId);
        if (!resolvedTaskId) {
          setActiveTaskId(0);
          setRuns([]);
          return;
        }
        if (resolvedTaskId !== activeTaskId) {
          setActiveTaskId(resolvedTaskId);
        }
      } catch (error) {
        if (!silent) {
          message.error(getRequestErrorMessage(error, t("common.loadFailed")));
        }
      } finally {
        if (!silent) {
          setTasksLoading(false);
        }
      }
    },
    [activeTaskId, setActiveTaskId, t]
  );

  const loadRuns = useCallback(
    async (taskId: number, silent = false) => {
      if (!silent) {
        setRunsLoading(true);
      }
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
        if (!silent) {
          message.error(getRequestErrorMessage(error, t("common.loadFailed")));
        }
        setRuns([]);
        setActiveRunId(0);
      } finally {
        if (!silent) {
          setRunsLoading(false);
        }
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

  const currentTask = useMemo(() => tasks.find((item) => item.id === activeTaskId), [tasks, activeTaskId]);
  const currentRun = useMemo(() => runs.find((item) => item.id === activeRunId), [runs, activeRunId]);
  const historyLoader = useCallback(() => (activeRunId ? runMetricsApi(activeRunId) : Promise.resolve([])), [activeRunId]);
  const { transport, series } = useWebSocket({
    taskId: activeTaskId,
    resetKey: `${activeTaskId}-${activeRunId}-${currentRun?.startTime ?? "idle"}`,
    onTaskStatus: handleTaskStatus,
    historyLoader,
    realtimeEnabled: currentRun?.status === "RUNNING"
  });

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
      return;
    }

    let disposed = false;
    void getAnalysisReportByRunApi(activeRunId)
      .then((result) => {
        if (!disposed) {
          setReport(result);
        }
      })
      .catch(() => {
        if (!disposed) {
          setReport(null);
        }
      });

    return () => {
      disposed = true;
    };
  }, [activeRunId]);

  useEffect(() => {
    const handleFocusRefresh = () => {
      void loadTasks(true);
      if (activeTaskId) {
        void loadRuns(activeTaskId, true);
      }
    };

    const handleVisibilityRefresh = () => {
      if (document.visibilityState === "visible") {
        handleFocusRefresh();
      }
    };

    window.addEventListener("focus", handleFocusRefresh);
    document.addEventListener("visibilitychange", handleVisibilityRefresh);

    return () => {
      window.removeEventListener("focus", handleFocusRefresh);
      document.removeEventListener("visibilitychange", handleVisibilityRefresh);
    };
  }, [activeTaskId, loadRuns, loadTasks]);

  useEffect(() => {
    const previousTransport = previousTransportRef.current;
    if (previousTransport === "websocket" && transport !== "websocket") {
      void loadTasks(true);
      if (activeTaskId) {
        void loadRuns(activeTaskId, true);
      }
    }
    previousTransportRef.current = transport;
  }, [activeTaskId, loadRuns, loadTasks, transport]);

  useEffect(() => {
    let disposed = false;
    let timer: ReturnType<typeof setTimeout> | null = null;

    const pollSystemMetrics = async () => {
      try {
        const metrics = await getSystemMetricsApi();
        if (!disposed) {
          setSysMetric(metrics);
        }
      } catch {
        // Keep the last successful metrics sample.
      } finally {
        if (!disposed) {
          timer = setTimeout(pollSystemMetrics, 3000);
        }
      }
    };

    void pollSystemMetrics();

    return () => {
      disposed = true;
      if (timer) {
        clearTimeout(timer);
      }
    };
  }, []);

  const xAxis = useMemo(() => series.map((s) => s.time), [series]);
  const connectionTagColor = transport === "websocket" ? "green" : transport === "polling" ? "gold" : "default";
  const connectionLabel =
    transport === "websocket"
      ? t("testTask.wsOnline")
      : transport === "polling"
        ? t("testTask.wsFallback")
        : t("testTask.wsOffline");

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card task-console-hero" style={{ borderRadius: 18 }}>
            <div className="task-console-hero__content">
              <div>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("testTask.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("testTask.subtitle")}</Typography.Text>
              </div>
              <Space wrap align="start" size={16}>
                <ActiveTaskSelector
                  label={t("testTask.currentTask")}
                  tasks={tasks}
                  value={activeTaskId}
                  loading={tasksLoading}
                  onChange={setActiveTaskId}
                  width={300}
                />
                <ActiveRunSelector
                  label={t("testTask.currentRun")}
                  runs={runs}
                  value={activeRunId}
                  loading={runsLoading}
                  onChange={setActiveRunId}
                  width={300}
                />
                <Space wrap>
                  {canCreateTask ? (
                    <Button icon={<PlusOutlined />} onClick={() => navigate("/test/task/create")}>
                      {t("testTask.createTask")}
                    </Button>
                  ) : null}
                  {currentRun?.status === "RUNNING" ? (
                    <Tag color={connectionTagColor} icon={<WifiOutlined />}>
                      {connectionLabel}
                    </Tag>
                  ) : null}
                  {canStartTask ? (
                    <Button
                      type="primary"
                      icon={<PlayCircleOutlined />}
                      onClick={async () => {
                        const taskId = activeTaskId || tasks[0]?.id;
                        if (!taskId) {
                          message.warning(t("testTask.selectTaskFirst"));
                          return;
                        }
                        try {
                          await startTaskApi(taskId);
                          await loadTasks();
                          await loadRuns(taskId, true);
                          message.success(t("testTask.startSuccess"));
                        } catch (error) {
                          message.error(getRequestErrorMessage(error, t("common.loadFailed")));
                        }
                      }}
                    >
                      {t("testTask.startTask")}
                    </Button>
                  ) : null}
                  {canStopTask ? (
                    <Button
                      danger
                      icon={<PauseCircleOutlined />}
                      onClick={async () => {
                        const taskId = activeTaskId || tasks[0]?.id;
                        if (!taskId) {
                          message.warning(t("testTask.selectTaskFirst"));
                          return;
                        }
                        try {
                          await stopTaskApi(taskId);
                          await loadTasks();
                          await loadRuns(taskId, true);
                          message.info(t("testTask.stopSuccess"));
                        } catch (error) {
                          message.error(getRequestErrorMessage(error, t("common.loadFailed")));
                        }
                      }}
                    >
                      {t("testTask.stopTask")}
                    </Button>
                  ) : null}
                  {canViewAnalysisReport ? (
                    <Button disabled={!activeRunId} onClick={() => navigate("/analysis/report")}>
                      {t("testTask.openReport")}
                    </Button>
                  ) : null}
                </Space>
              </Space>
            </div>
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card className="glass-card task-console-card task-console-card--top" title={t("testTask.snapshot")} style={{ borderRadius: 18 }}>
            {currentTask && currentRun ? (
              <Space direction="vertical" size={16} style={{ width: "100%" }}>
                <div className="task-console-snapshot__header">
                  <div>
                    <Typography.Text type="secondary">{t("testTask.colPlan")}</Typography.Text>
                    <Typography.Title level={4} style={{ margin: "4px 0 0" }}>
                      {currentTask.planName || "--"}
                    </Typography.Title>
                  </div>
                  <Tag color={taskStatusColorMap[currentRun.status] ?? "default"}>{renderTaskStatus(t, currentRun.status)}</Tag>
                </div>
                <Row gutter={[16, 16]}>
                  <Col span={12}>
                    <Statistic title={t("testTask.qps")} value={series.at(-1)?.qps ?? 0} suffix="req/s" precision={0} />
                  </Col>
                  <Col span={12}>
                    <Statistic title={t("testTask.p99")} value={series.at(-1)?.p99 ?? 0} suffix="ms" precision={0} />
                  </Col>
                  <Col span={12}>
                    <Statistic
                      title={t("testTask.errorRate")}
                      value={series.at(-1)?.errorRate ?? 0}
                      suffix="%"
                      precision={2}
                      valueStyle={{ color: (series.at(-1)?.errorRate || 0) > 5 ? "#c92a2a" : undefined }}
                    />
                  </Col>
                  <Col span={12}>
                    <Statistic title={t("analysisReport.runId")} value={currentRun.id} />
                  </Col>
                </Row>
              </Space>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t("testTask.noRunSelected")} />
            )}
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <RealtimeMetricsPanel metric={sysMetric} compact className="task-console-card task-console-card--top" />
        </Col>

        <Col xs={24} md={8}>
          <Card className="glass-card task-console-card task-console-card--top" title={t("testTask.runOverview")} style={{ borderRadius: 18 }}>
            {currentTask && currentRun ? (
              <Space direction="vertical" size={14} style={{ width: "100%" }}>
                <Space wrap>
                  <Tag color="blue">#{currentTask.id}</Tag>
                  <Tag color="geekblue">run #{currentRun.id}</Tag>
                  <Tag color={taskStatusColorMap[currentRun.status] ?? "default"}>{renderTaskStatus(t, currentRun.status)}</Tag>
                </Space>
                <div>
                  <Typography.Text type="secondary">{t("testTask.taskStart")}</Typography.Text>
                  <div>{currentRun.startTime ? formatDateTime(currentRun.startTime) : "--"}</div>
                </div>
                <div>
                  <Typography.Text type="secondary">{t("testTask.taskEnd")}</Typography.Text>
                  <div>{currentRun.endTime ? formatDateTime(currentRun.endTime) : "--"}</div>
                </div>
                <div>
                  <Typography.Text type="secondary">{t("testTask.currentTask")}</Typography.Text>
                  <div>{currentTask.sceneName || "--"}</div>
                </div>
                <div>
                  <Typography.Text type="secondary">{t("dashboard.analysisSummary")}</Typography.Text>
                  <div>{report?.summary || t("analysisReport.noReport")}</div>
                </div>
              </Space>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t("testTask.noRunSelected")} />
            )}
          </Card>
        </Col>

        <Col span={24}>
          <Card className="glass-card task-console-card task-console-card--main" style={{ borderRadius: 18 }}>
            <PerformanceChart
              title={t("testTask.realtimeCurves")}
              xAxis={xAxis}
              area
              yAxisName={t("testTask.qps")}
              secondaryYAxisName="ms"
              height={400}
              series={[
                { name: t("testTask.qps"), color: "#0b7285", data: series.map((s) => s.qps), yAxisIndex: 0 },
                { name: t("testTask.seriesP50"), color: "#2b8a3e", data: series.map((s) => s.p50), yAxisIndex: 1 },
                { name: t("testTask.seriesP90"), color: "#f08c00", data: series.map((s) => s.p90), yAxisIndex: 1 },
                { name: t("testTask.p99"), color: "#c92a2a", data: series.map((s) => s.p99), yAxisIndex: 1 }
              ]}
            />
          </Card>
        </Col>

        <Col span={24}>
          <Card className="glass-card task-console-card task-console-card--side" style={{ borderRadius: 18 }}>
            <PerformanceChart
              title={t("testTask.errorCurve")}
              xAxis={xAxis}
              yAxisName="%"
              series={[{ name: t("testTask.errorRate"), color: "#c92a2a", data: series.map((s) => s.errorRate) }]}
              height={220}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default TestTaskPage;
