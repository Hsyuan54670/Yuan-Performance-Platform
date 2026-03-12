import { Card, Col, Row, Statistic, Tag, Typography } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMetricsSummaryApi, getSystemMetricsApi } from "../../../api/monitor";
import PerformanceChart from "../../../components/PerformanceChart";
import RealtimeMetricsPanel from "../../../components/RealtimeMetricsPanel";
import { useWebSocket } from "../../../hooks/useWebSocket";
import { useAppStore } from "../../../store/appStore";
import type { MetricsSummary, SystemMetric } from "../../../types/monitor";

function MonitorRealtimePage() {
  const { activeTaskId } = useAppStore();
  const { series, transport } = useWebSocket(activeTaskId);
  const [metric, setMetric] = useState<SystemMetric>({ cpu: 0, memory: 0, disk: 0, networkIn: 0, networkOut: 0 });
  const [summary, setSummary] = useState<MetricsSummary>({
    taskId: null,
    runId: null,
    status: "UNKNOWN",
    qps: 0,
    p99: 0,
    errorRate: 0,
    timestamp: null
  });
  const { t } = useTranslation();

  useEffect(() => {
    let disposed = false;
    let timer: ReturnType<typeof setTimeout> | null = null;

    const pollSystemMetrics = async () => {
      try {
        const systemMetrics = await getSystemMetricsApi();
        if (!disposed) {
          setMetric(systemMetrics);
        }
      } catch {
        // Keep the last successful sample to avoid flashing back to zero.
      } finally {
        if (!disposed) {
          timer = setTimeout(pollSystemMetrics, 5000);
        }
      }
    };

    pollSystemMetrics();

    return () => {
      disposed = true;
      if (timer) {
        clearTimeout(timer);
      }
    };
  }, []);

  useEffect(() => {
    let disposed = false;
    let timer: ReturnType<typeof setTimeout> | null = null;

    const pollMetricsSummary = async () => {
      try {
        const metricsSummary = await getMetricsSummaryApi();
        if (!disposed && metricsSummary) {
          setSummary(metricsSummary);
        }
      } catch {
        // Keep the last successful summary if the backend is temporarily unavailable.
      } finally {
        if (!disposed) {
          timer = setTimeout(pollMetricsSummary, 3000);
        }
      }
    };

    pollMetricsSummary();

    return () => {
      disposed = true;
      if (timer) {
        clearTimeout(timer);
      }
    };
  }, []);

  const xAxis = useMemo(() => series.map((s) => s.time), [series]);
  const displayTaskId = summary.taskId ?? activeTaskId;
  const streamStatus =
    transport === "websocket"
      ? t("monitorRealtime.connected")
      : transport === "polling"
        ? t("monitorRealtime.polling")
        : t("monitorRealtime.disconnected");
  const streamTagColor = transport === "websocket" ? "green" : transport === "polling" ? "gold" : "default";

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Typography.Title level={3} style={{ margin: 0 }}>{t("monitorRealtime.title")}</Typography.Title>
            <Typography.Text type="secondary">
              {t("monitorRealtime.subtitle", { status: streamStatus })}
            </Typography.Text>
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 16 }}><Statistic title={t("monitorRealtime.cpuAvg")} value={metric.cpu} suffix="%" precision={1} /></Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 16 }}><Statistic title={t("monitorRealtime.memoryAvg")} value={metric.memory} suffix="%" precision={1} /></Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 16 }}><Statistic title={t("monitorRealtime.diskAvg")} value={metric.disk} suffix="%" precision={1} /></Card>
        </Col>

        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("testTask.qps")} value={summary.qps} suffix="req/s" precision={2} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("testTask.p99")} value={summary.p99} suffix="ms" precision={2} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("testTask.errorRate")} value={summary.errorRate} suffix="%" precision={2} />
          </Card>
        </Col>

        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <PerformanceChart
              title={t("monitorRealtime.chartTitle")}
              xAxis={xAxis}
              yAxisName={t("monitorRealtime.chartYAxis")}
              series={[
                { name: t("testTask.p99"), color: "#e8590c", data: series.map((s) => s.p99) },
                { name: t("monitorRealtime.seriesError"), color: "#c92a2a", data: series.map((s) => s.errorRate) }
              ]}
            />
          </Card>
        </Col>

        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Row gutter={12}>
              <Col><Tag color="processing">{t("monitorRealtime.taskTag", { id: displayTaskId ?? "-" })}</Tag></Col>
              <Col><Tag color={streamTagColor}>{streamStatus}</Tag></Col>
              <Col><Tag color={summary.status === "RUNNING" ? "green" : summary.status === "SUCCESS" ? "blue" : summary.status === "FAILED" ? "red" : "default"}>{summary.status || "UNKNOWN"}</Tag></Col>
              <Col><Tag color="green">{t("monitorRealtime.cpuTag", { value: metric.cpu })}</Tag></Col>
              <Col><Tag color="gold">{t("monitorRealtime.memTag", { value: metric.memory })}</Tag></Col>
              <Col><Tag color="blue">{t("monitorRealtime.netInTag", { value: metric.networkIn })}</Tag></Col>
            </Row>
          </Card>
        </Col>

        <Col span={24}>
          <RealtimeMetricsPanel metric={metric} />
        </Col>
      </Row>
    </div>
  );
}

export default MonitorRealtimePage;

