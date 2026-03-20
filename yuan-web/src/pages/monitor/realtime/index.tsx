import { WifiOutlined } from "@ant-design/icons";
import { Card, Col, Empty, Row, Space, Statistic, Tag, Typography, message } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getRunSystemMetricPointsApi,
  getRunSystemMetricSummaryApi,
  getSystemMetricsApi
} from "../../../api/monitor";
import { listTaskRunsApi, listTasksApi, runMetricsApi } from "../../../api/test";
import ActiveRunSelector from "../../../components/ActiveRunSelector";
import ActiveTaskSelector from "../../../components/ActiveTaskSelector";
import PerformanceChart from "../../../components/PerformanceChart";
import RealtimeMetricsPanel from "../../../components/RealtimeMetricsPanel";
import { useWebSocket } from "../../../hooks/useWebSocket";
import { useAppStore } from "../../../store/appStore";
import type { RunSystemMetricPoint, RunSystemMetricSummary, SystemMetric } from "../../../types/monitor";
import type { TaskStatusPushMessage, TestTask, TestTaskRun } from "../../../types/test";
import { formatDateTime } from "../../../utils/format";
import { getRequestErrorMessage } from "../../../utils/request";
import { renderTaskStatus, taskStatusColorMap } from "../../../utils/taskStatus";
import { resolveActiveRunId, resolveActiveTaskId } from "../../../utils/taskSelection";

const POLL_INTERVAL_MS = 3000;

const padTime = (value: number) => String(value).padStart(2, "0");

const toTimeLabel = (value?: string | null) => {
  if (!value) {
    return "--:--:--";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return [parsed.getHours(), parsed.getMinutes(), parsed.getSeconds()].map(padTime).join(":");
};

const toChartValue = (value: number | null | undefined, missing?: boolean) => {
  if (missing || value == null || Number.isNaN(value)) {
    return Number.NaN;
  }
  return value;
};

function MonitorRealtimePage() {
  const { t } = useTranslation();
  const { activeTaskId, activeRunId, setActiveTaskId, setActiveRunId } = useAppStore();
  const [tasks, setTasks] = useState<TestTask[]>([]);
  const [runs, setRuns] = useState<TestTaskRun[]>([]);
  const [tasksLoading, setTasksLoading] = useState(true);
  const [runsLoading, setRunsLoading] = useState(false);
  const [resourceLoading, setResourceLoading] = useState(false);
  const [hostMetric, setHostMetric] = useState<SystemMetric>({ cpu: 0, memory: 0, disk: 0, networkIn: 0, networkOut: 0 });
  const [resourceSummary, setResourceSummary] = useState<RunSystemMetricSummary | null>(null);
  const [resourcePoints, setResourcePoints] = useState<RunSystemMetricPoint[]>([]);

  const loadTasks = useCallback(
    async (silent = false) => {
      if (!silent) {
        setTasksLoading(true);
      }
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
        if (!silent) {
          message.error(getRequestErrorMessage(error, t("common.loadFailed")));
        }
        setTasks([]);
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

  const loadRunResources = useCallback(
    async (runId: number, silent = false) => {
      if (!silent) {
        setResourceLoading(true);
      }
      try {
        const [summary, points] = await Promise.all([
          getRunSystemMetricSummaryApi(runId),
          getRunSystemMetricPointsApi(runId)
        ]);
        setResourceSummary(summary);
        setResourcePoints(points);
        return true;
      } catch (error) {
        setResourceSummary(null);
        setResourcePoints([]);
        if (!silent) {
          message.error(getRequestErrorMessage(error, t("common.loadFailed")));
        }
        return false;
      } finally {
        if (!silent) {
          setResourceLoading(false);
        }
      }
    },
    [t]
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
    let disposed = false;
    let timer: ReturnType<typeof setTimeout> | null = null;

    const pollHostMetrics = async () => {
      try {
        const metrics = await getSystemMetricsApi();
        if (!disposed) {
          setHostMetric(metrics);
        }
      } catch {
        // Keep the last successful machine-wide sample to avoid noisy resets.
      } finally {
        if (!disposed) {
          timer = setTimeout(pollHostMetrics, 5000);
        }
      }
    };

    void pollHostMetrics();

    return () => {
      disposed = true;
      if (timer) {
        clearTimeout(timer);
      }
    };
  }, []);

  const currentTask = useMemo(() => tasks.find((task) => task.id === activeTaskId), [tasks, activeTaskId]);
  const currentRun = useMemo(() => runs.find((run) => run.id === activeRunId), [runs, activeRunId]);

  useEffect(() => {
    if (!activeRunId) {
      setResourceSummary(null);
      setResourcePoints([]);
      return;
    }

    let disposed = false;
    let timer: ReturnType<typeof setTimeout> | null = null;

    const pollRunResources = async (silent = true) => {
      await loadRunResources(activeRunId, silent);
      if (!disposed && currentRun?.status === "RUNNING") {
        timer = setTimeout(() => {
          void pollRunResources(true);
        }, POLL_INTERVAL_MS);
      }
    };

    void pollRunResources(false);

    return () => {
      disposed = true;
      if (timer) {
        clearTimeout(timer);
      }
    };
  }, [activeRunId, currentRun?.status, loadRunResources]);

  const latestResourcePoint = resourcePoints.at(-1);
  const historyLoader = useCallback(() => (activeRunId ? runMetricsApi(activeRunId) : Promise.resolve([])), [activeRunId]);
  const { series, transport } = useWebSocket({
    taskId: activeTaskId,
    resetKey: `${activeTaskId}-${activeRunId}-${currentRun?.startTime ?? "idle"}`,
    onTaskStatus: handleTaskStatus,
    historyLoader,
    realtimeEnabled: currentRun?.status === "RUNNING"
  });

  const connectionTagColor = transport === "websocket" ? "green" : transport === "polling" ? "gold" : "default";
  const connectionLabel =
    transport === "websocket"
      ? t("monitorRealtime.connected")
      : transport === "polling"
        ? t("monitorRealtime.polling")
        : t("monitorRealtime.disconnected");

  const businessXAxis = useMemo(() => series.map((point) => point.time), [series]);
  const resourceXAxis = useMemo(() => resourcePoints.map((point) => toTimeLabel(point.ts)), [resourcePoints]);


  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space direction="vertical" size={16} style={{ width: "100%" }}>
              <div>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("monitorRealtime.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("monitorRealtime.subtitle")}</Typography.Text>
              </div>
              <Space wrap style={{ width: "100%", justifyContent: "space-between" }} align="start">
                <Space wrap align="start" size={16}>
                  <ActiveTaskSelector
                    label={t("testTask.currentTask")}
                    tasks={tasks}
                    value={activeTaskId}
                    loading={tasksLoading}
                    onChange={setActiveTaskId}
                  />
                  <ActiveRunSelector
                    label={t("testTask.currentRun")}
                    runs={runs}
                    value={activeRunId}
                    loading={runsLoading}
                    onChange={setActiveRunId}
                  />
                </Space>
                <Space wrap>
                  {currentRun?.status === "RUNNING" ? (
                    <Tag color={connectionTagColor} icon={<WifiOutlined />}>
                      {connectionLabel}
                    </Tag>
                  ) : null}
                </Space>
              </Space>
            </Space>
          </Card>
        </Col>

        <Col xs={24} md={8} xl={4}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("testTask.qps")} value={series.at(-1)?.qps ?? 0} suffix="req/s" precision={0} />
          </Card>
        </Col>
        <Col xs={24} md={8} xl={4}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("testTask.p99")} value={series.at(-1)?.p99 ?? 0} suffix="ms" precision={0} />
          </Card>
        </Col>
        <Col xs={24} md={8} xl={4}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("testTask.errorRate")} value={series.at(-1)?.errorRate ?? 0} suffix="%" precision={2} />
          </Card>
        </Col>
        <Col xs={24} md={8} xl={4}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("dashboard.avgCpu")} value={resourceSummary?.avgCpu} suffix="%" precision={1} formatter={(value) => value ?? "--"} />
          </Card>
        </Col>
        <Col xs={24} md={8} xl={4}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("dashboard.peakCpu")} value={resourceSummary?.peakCpu} suffix="%" precision={1} formatter={(value) => value ?? "--"} />
          </Card>
        </Col>
        <Col xs={24} md={8} xl={4}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("dashboard.avgMemory")} value={resourceSummary?.avgMemory} suffix="%" precision={1} formatter={(value) => value ?? "--"} />
          </Card>
        </Col>

        <Col xs={24} xl={14}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            {!activeRunId ? (
              <Empty description={t("testTask.noRunSelected")} />
            ) : (
              <PerformanceChart
                title={t("monitorRealtime.businessChartTitle")}
                xAxis={businessXAxis}
                area
                yAxisName={t("dashboard.seriesQps")}
                secondaryYAxisName="ms / %"
                height={360}
                series={[
                  { name: t("dashboard.seriesQps"), color: "#0b7285", data: series.map((point) => point.qps), yAxisIndex: 0 },
                  { name: t("dashboard.seriesP99"), color: "#e8590c", data: series.map((point) => point.p99), yAxisIndex: 1 },
                  { name: t("dashboard.seriesError"), color: "#c92a2a", data: series.map((point) => point.errorRate), yAxisIndex: 1 }
                ]}
              />
            )}
          </Card>
        </Col>

        <Col xs={24} xl={10}>
          <Card className="glass-card" style={{ borderRadius: 18, minHeight: 420 }}>
            {!currentTask || !currentRun ? (
              <Empty description={t("testTask.noRunSelected")} />
            ) : (
              <Space direction="vertical" size={16} style={{ width: "100%" }}>
                <div>
                  <Typography.Title level={4} style={{ margin: 0 }}>{t("monitorRealtime.runOverview")}</Typography.Title>
                  <Typography.Text type="secondary">{currentTask.planName || currentTask.sceneName || "--"}</Typography.Text>
                </div>
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
                <Row gutter={[12, 12]}>
                  <Col span={12}>
                    <Statistic title={t("dashboard.sampledSeconds")} value={resourceSummary?.sampledSeconds} formatter={(value) => value ?? "--"} />
                  </Col>
                  <Col span={12}>
                    <Statistic title={t("dashboard.missingSeconds")} value={resourceSummary?.missingSeconds} formatter={(value) => value ?? "--"} />
                  </Col>
                  <Col span={12}>
                    <Statistic title={t("dashboard.peakMemory")} value={resourceSummary?.peakMemory} suffix="%" precision={1} formatter={(value) => value ?? "--"} />
                  </Col>
                  <Col span={12}>
                    <Statistic title={t("monitorRealtime.latestSampleCount")} value={latestResourcePoint?.sampleCount} formatter={(value) => value ?? "--"} />
                  </Col>
                </Row>
                <div>
                  <Typography.Text type="secondary">{t("monitorRealtime.latestSampleTime")}</Typography.Text>
                  <div>{latestResourcePoint?.ts ? formatDateTime(latestResourcePoint.ts) : "--"}</div>
                </div>
              </Space>
            )}
          </Card>
        </Col>

        <Col xs={24} xl={14}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            {!activeRunId ? (
              <Empty description={t("testTask.noRunSelected")} />
            ) : !resourcePoints.length ? (
              <Empty description={t("monitorRealtime.noResourceData")} />
            ) : (
              <PerformanceChart
                title={t("monitorRealtime.resourceChartTitle")}
                xAxis={resourceXAxis}
                yAxisName="%"
                secondaryYAxisName={t("monitorRealtime.sampleCount")}
                height={320}
                series={[
                  { name: t("dashboard.avgCpu"), color: "#0b7285", data: resourcePoints.map((point) => toChartValue(point.cpu, point.missing)), yAxisIndex: 0 },
                  { name: t("dashboard.peakCpu"), color: "#1971c2", data: resourcePoints.map((point) => toChartValue(point.cpuMax, point.missing)), yAxisIndex: 0 },
                  { name: t("dashboard.avgMemory"), color: "#e67700", data: resourcePoints.map((point) => toChartValue(point.memory, point.missing)), yAxisIndex: 0 },
                  { name: t("monitorRealtime.sampleCount"), color: "#5f3dc4", data: resourcePoints.map((point) => (point.missing ? 0 : point.sampleCount ?? 0)), yAxisIndex: 1 }
                ]}
              />
            )}
          </Card>
        </Col>

        <Col xs={24} xl={10}>
          <Space direction="vertical" size={12} style={{ width: "100%" }}>
            <RealtimeMetricsPanel metric={hostMetric} />
            <Typography.Text type="secondary">{t("monitorRealtime.hostPanelNote")}</Typography.Text>
          </Space>
        </Col>
      </Row>
    </div>
  );
}

export default MonitorRealtimePage;



