import { PauseCircleOutlined, PlayCircleOutlined, WifiOutlined } from "@ant-design/icons";
import { Button, Card, Col, Row, Space, Statistic, Table, Tag, Typography, message } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { getSystemMetricsApi } from "../../../api/monitor";
import { listTasksApi, startTaskApi, stopTaskApi } from "../../../api/test";
import LazyEChart from "../../../components/LazyEChart";
import PerformanceChart from "../../../components/PerformanceChart";
import RealtimeMetricsPanel from "../../../components/RealtimeMetricsPanel";
import { useWebSocket } from "../../../hooks/useWebSocket";
import { useAppStore } from "../../../store/appStore";
import type { SystemMetric } from "../../../types/monitor";
import type { TestTask } from "../../../types/test";
import { formatDateTime, formatPercent } from "../../../utils/format";

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
  const { connected, series } = useWebSocket(activeTaskId);
  const { t } = useTranslation();

  const loadTasks = async () => {
    const resp = await listTasksApi();
    setTasks(resp);
    if (!resp.length) {
      return;
    }
    if (!resp.some((item) => item.id === activeTaskId)) {
      setActiveTaskId(resp[0].id);
    }
  };

  useEffect(() => {
    loadTasks();
    getSystemMetricsApi().then(setSysMetric);
  }, [activeTaskId, setActiveTaskId]);

  const xAxis = useMemo(() => series.map((s) => s.time), [series]);

  const statusCodeOption = {
    tooltip: { trigger: "item" },
    legend: { bottom: 0 },
    series: [
      {
        type: "pie",
        radius: ["44%", "72%"],
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
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space style={{ width: "100%", justifyContent: "space-between" }}>
              <div>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("testTask.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("testTask.subtitle")}</Typography.Text>
              </div>
              <Space>
                <Tag color={connected ? "green" : "default"} icon={<WifiOutlined />}>
                  {connected ? t("testTask.wsOnline") : t("testTask.wsOffline")}
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
          </Card>
        </Col>

        <Col xs={24} lg={10}>
          <Card className="glass-card" title={t("testTask.taskList")} style={{ borderRadius: 18 }}>
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
                    <Tag color={value === "RUNNING" ? "green" : value === "SUCCESS" ? "blue" : "default"}>{value}</Tag>
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

        <Col xs={24} lg={14}>
          <Card className="glass-card" title={t("testTask.snapshot")} style={{ borderRadius: 18 }}>
            <Row gutter={[12, 12]}>
              <Col span={8}>
                <Statistic title={t("testTask.qps")} value={series.at(-1)?.qps ?? 0} suffix="req/s" precision={0} />
              </Col>
              <Col span={8}>
                <Statistic title={t("testTask.p99")} value={series.at(-1)?.p99 ?? 0} suffix="ms" precision={0} />
              </Col>
              <Col span={8}>
                <Statistic
                  title={t("testTask.errorRate")}
                  value={series.at(-1)?.errorRate ?? 0}
                  suffix="%"
                  precision={2}
                  valueStyle={{ color: (series.at(-1)?.errorRate || 0) > 5 ? "#c92a2a" : undefined }}
                />
              </Col>
            </Row>
            <Typography.Text type="secondary">
              {t("testTask.taskStart")}: {formatDateTime(tasks.find((item) => item.id === activeTaskId)?.startTime || new Date().toISOString())}
            </Typography.Text>
          </Card>
        </Col>

        <Col xs={24} xl={16}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <PerformanceChart
              title={t("testTask.realtimeCurves")}
              xAxis={xAxis}
              area
              series={[
                { name: t("testTask.qps"), color: "#0b7285", data: series.map((s) => s.qps) },
                { name: t("testTask.seriesP50"), color: "#2b8a3e", data: series.map((s) => s.p50) },
                { name: t("testTask.seriesP90"), color: "#f08c00", data: series.map((s) => s.p90) },
                { name: t("testTask.p99"), color: "#c92a2a", data: series.map((s) => s.p99) }
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} xl={8}>
          <Card className="glass-card" title={t("testTask.statusDistribution")} style={{ borderRadius: 18 }}>
            <LazyEChart option={statusCodeOption} style={{ height: 280 }} />
          </Card>
          <Card className="glass-card" style={{ borderRadius: 18, marginTop: 16 }}>
            <PerformanceChart
              title={t("testTask.errorCurve")}
              xAxis={xAxis}
              yAxisName="%"
              series={[{ name: t("testTask.errorRate"), color: "#c92a2a", data: series.map((s) => s.errorRate) }]}
              height={220}
            />
          </Card>
        </Col>

        <Col span={24}>
          <RealtimeMetricsPanel metric={sysMetric} />
        </Col>
      </Row>
    </div>
  );
}

export default TestTaskPage;
