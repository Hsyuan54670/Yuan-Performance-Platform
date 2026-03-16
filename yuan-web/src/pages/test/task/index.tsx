import { PauseCircleOutlined, PlayCircleOutlined, WifiOutlined } from "@ant-design/icons";
import { Button, Card, Col, Row, Space, Statistic, Table, Tag, Typography, message } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { getSystemMetricsApi } from "../../../api/monitor";
import { listTasksApi, startTaskApi, stopTaskApi } from "../../../api/test";
import ActiveTaskSelector from "../../../components/ActiveTaskSelector";
import LazyEChart from "../../../components/LazyEChart";
import PerformanceChart from "../../../components/PerformanceChart";
import RealtimeMetricsPanel from "../../../components/RealtimeMetricsPanel";
import { useWebSocket } from "../../../hooks/useWebSocket";
import { useAppStore } from "../../../store/appStore";
import type { SystemMetric } from "../../../types/monitor";
import type { TestTask } from "../../../types/test";
import { formatDateTime, formatPercent } from "../../../utils/format";
import { resolveActiveTaskId } from "../../../utils/taskSelection";

function TestTaskPage() {
  const [tasks, setTasks] = useState<TestTask[]>([]);
  const [sysMetric, setSysMetric] = useState<SystemMetric>({
    cpu: 0,
    memory: 0,
    disk: 0,
    networkIn: 0,
    networkOut: 0
  });
  const { activeTaskId, setActiveTaskId } = useAppStore();
  const activeTask = tasks.find((item) => item.id === activeTaskId) || tasks[0];
  const realtimeResetKey = `${activeTaskId}-${activeTask?.startTime ?? "idle"}`;
  const { transport, series } = useWebSocket(activeTaskId, realtimeResetKey);
  const { t } = useTranslation();

  const loadTasks = useCallback(async () => {
    const resp = await listTasksApi();
    setTasks(resp);
    const resolvedTaskId = resolveActiveTaskId(resp, activeTaskId);
    if (resolvedTaskId && resolvedTaskId !== activeTaskId) {
      setActiveTaskId(resolvedTaskId);
    }
  }, [activeTaskId, setActiveTaskId]);

  useEffect(() => {
    let disposed = false;
    let timer: ReturnType<typeof setTimeout> | null = null;

    const pollTasks = async () => {
      try {
        await loadTasks();
      } catch {
        // Keep the last successful task snapshot to avoid flicker on transient failures.
      } finally {
        if (!disposed) {
          timer = setTimeout(pollTasks, 3000);
        }
      }
    };

    void pollTasks();

    return () => {
      disposed = true;
      if (timer) {
        clearTimeout(timer);
      }
    };
  }, [loadTasks]);

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
    transport === "websocket" ? t("testTask.wsOnline") : transport === "polling" ? t("testTask.wsFallback") : t("testTask.wsOffline");

  const statusCodeOption = {
    tooltip: { trigger: "item" },
    legend: { bottom: 0 },
    series: [
      {
        type: "pie",
        radius: ["46%", "74%"],
        label: { formatter: "{b}: {d}%" },
        data: [
          { value: 85, name: "200" },
          { value: 7, name: "302" },
          { value: 4, name: "429" },
          { value: 3, name: "500" },
          { value: 1, name: "503" }
        ]
      }
    ]
  };

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
              <Space wrap align="start">
                <ActiveTaskSelector
                  label={t("testTask.currentTask")}
                  tasks={tasks}
                  value={activeTaskId}
                  onChange={setActiveTaskId}
                  width={320}
                />
                <Space wrap>
                  <Tag color={connectionTagColor} icon={<WifiOutlined />}>
                    {connectionLabel}
                  </Tag>
                  <Button
                    type="primary"
                    icon={<PlayCircleOutlined />}
                    onClick={async () => {
                      const taskId = activeTaskId || tasks[0]?.id;
                      if (!taskId) {
                        message.warning(t("testTask.selectTaskFirst"));
                        return;
                      }
                      await startTaskApi(taskId);
                      await loadTasks();
                      message.success(t("testTask.startSuccess"));
                    }}
                  >
                    {t("testTask.startTask")}
                  </Button>
                  <Button
                    danger
                    icon={<PauseCircleOutlined />}
                    onClick={async () => {
                      const taskId = activeTaskId || tasks[0]?.id;
                      if (!taskId) {
                        message.warning(t("testTask.selectTaskFirst"));
                        return;
                      }
                      await stopTaskApi(taskId);
                      await loadTasks();
                      message.info(t("testTask.stopSuccess"));
                    }}
                  >
                    {t("testTask.stopTask")}
                  </Button>
                </Space>
              </Space>
            </div>
          </Card>
        </Col>

        <Col xs={24} md={12} xl={7} className="task-console-top-col">
          <Card className="glass-card task-console-card task-console-card--top" title={t("testTask.snapshot")} style={{ borderRadius: 18 }}>
            <div className="task-console-snapshot">
              <div className="task-console-snapshot__header">
                <div>
                  <Typography.Text type="secondary">{t("testTask.colPlan")}</Typography.Text>
                  <Typography.Title level={4} style={{ margin: "4px 0 0" }}>
                    {activeTask?.planName || "--"}
                  </Typography.Title>
                </div>
                <Tag color={activeTask?.status === "RUNNING" ? "green" : activeTask?.status === "SUCCESS" ? "blue" : activeTask?.status === "FAILED" ? "red" : "default"}>
                  {activeTask?.status || "--"}
                </Tag>
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
                  <Statistic title={t("testTask.colTask")} value={activeTask?.id ?? undefined} formatter={(value) => value ?? "--"} />
                </Col>
              </Row>

              <div className="task-console-snapshot__meta">
                <div>
                  <Typography.Text type="secondary">{t("testTask.taskStart")}</Typography.Text>
                  <div>{activeTask?.startTime ? formatDateTime(activeTask.startTime) : "--"}</div>
                </div>
                <div>
                  <Typography.Text type="secondary">Scene</Typography.Text>
                  <div>{activeTask?.sceneName || "--"}</div>
                </div>
              </div>
            </div>
          </Card>
        </Col>

        <Col xs={24} md={12} xl={9} className="task-console-top-col">
          <RealtimeMetricsPanel metric={sysMetric} compact className="task-console-card task-console-card--top" />
        </Col>

        <Col xs={24} xl={8} className="task-console-top-col">
          <Card className="glass-card task-console-card task-console-card--top" title={t("testTask.taskList")} style={{ borderRadius: 18 }}>
            <Table
              rowKey="id"
              size="small"
              pagination={false}
              dataSource={tasks}
              onRow={(record) => ({
                onClick: () => setActiveTaskId(record.id),
                style: { cursor: "pointer", background: activeTaskId === record.id ? "#e7f5ff" : "transparent" }
              })}
              columns={[
                { title: t("testTask.colTask"), dataIndex: "id", width: 76 },
                { title: t("testTask.colPlan"), dataIndex: "planName" },
                {
                  title: t("testTask.colStatus"),
                  dataIndex: "status",
                  render: (value: string) => (
                    <Tag color={value === "RUNNING" ? "green" : value === "SUCCESS" ? "blue" : value === "FAILED" ? "red" : "default"}>{value}</Tag>
                  )
                },
                {
                  title: t("testTask.colError"),
                  dataIndex: "errorRate",
                  render: (v: number) => formatPercent(v)
                }
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} xl={16} className="task-console-main-col">
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

        <Col xs={24} xl={8} className="task-console-side-col">
          <Row gutter={[16, 16]} className="task-console-side-stack">
            <Col span={24} className="task-console-side-stack__item">
              <Card className="glass-card task-console-card task-console-card--side" style={{ borderRadius: 18 }}>
                <PerformanceChart
                  title={t("testTask.errorCurve")}
                  xAxis={xAxis}
                  yAxisName="%"
                  series={[{ name: t("testTask.errorRate"), color: "#c92a2a", data: series.map((s) => s.errorRate) }]}
                  height={190}
                />
              </Card>
            </Col>
            <Col span={24} className="task-console-side-stack__item">
              <Card className="glass-card task-console-card task-console-card--side" title={t("testTask.statusDistribution")} style={{ borderRadius: 18 }}>
                <LazyEChart option={statusCodeOption} style={{ height: 190 }} />
              </Card>
            </Col>
          </Row>
        </Col>
      </Row>
    </div>
  );
}

export default TestTaskPage;
