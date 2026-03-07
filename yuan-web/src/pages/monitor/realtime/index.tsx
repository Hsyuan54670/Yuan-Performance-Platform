import { Card, Col, Row, Statistic, Tag, Typography } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { getSystemMetricsApi } from "../../../api/monitor";
import PerformanceChart from "../../../components/PerformanceChart";
import RealtimeMetricsPanel from "../../../components/RealtimeMetricsPanel";
import { useWebSocket } from "../../../hooks/useWebSocket";
import { useAppStore } from "../../../store/appStore";
import type { SystemMetric } from "../../../types/monitor";

function MonitorRealtimePage() {
  const { activeTaskId } = useAppStore();
  const { connected, series } = useWebSocket(activeTaskId);
  const [metric, setMetric] = useState<SystemMetric>({ cpu: 0, memory: 0, disk: 0, networkIn: 0, networkOut: 0 });
  const { t } = useTranslation();

  useEffect(() => {
    getSystemMetricsApi().then(setMetric);
  }, []);

  const xAxis = useMemo(() => series.map((s) => s.time), [series]);

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Typography.Title level={3} style={{ margin: 0 }}>{t("monitorRealtime.title")}</Typography.Title>
            <Typography.Text type="secondary">
              {t("monitorRealtime.subtitle", { status: connected ? t("monitorRealtime.connected") : t("monitorRealtime.disconnected") })}
            </Typography.Text>
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 16 }}><Statistic title={t("monitorRealtime.cpuAvg")} value={metric.cpu} suffix="%" /></Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 16 }}><Statistic title={t("monitorRealtime.memoryAvg")} value={metric.memory} suffix="%" /></Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 16 }}><Statistic title={t("monitorRealtime.diskAvg")} value={metric.disk} suffix="%" /></Card>
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
              <Col><Tag color="processing">{t("monitorRealtime.taskTag", { id: activeTaskId })}</Tag></Col>
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
