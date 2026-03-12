import { Card, Col, Progress, Row, Statistic } from "antd";
import { useTranslation } from "react-i18next";
import type { SystemMetric } from "../types/monitor";

interface RealtimeMetricsPanelProps {
  metric: SystemMetric;
  compact?: boolean;
  className?: string;
}

function RealtimeMetricsPanel({ metric, compact = false, className }: RealtimeMetricsPanelProps) {
  const { t } = useTranslation();
  const colSpan = compact ? { xs: 12, md: 12, lg: 12 } : { xs: 24, md: 12, lg: 6 };

  return (
    <Card
      className={["glass-card", className].filter(Boolean).join(" ")}
      title={t("components.resourcePanel.title")}
      style={{ borderRadius: 18 }}
      styles={{ body: { padding: compact ? 18 : 24 } }}
    >
      <Row gutter={[compact ? 12 : 14, compact ? 12 : 14]}>
        <Col {...colSpan}>
          <Statistic title={t("components.resourcePanel.cpu")} value={metric.cpu} suffix="%" />
          <Progress percent={metric.cpu} strokeColor="#0b7285" showInfo={false} />
        </Col>
        <Col {...colSpan}>
          <Statistic title={t("components.resourcePanel.memory")} value={metric.memory} suffix="%" />
          <Progress percent={metric.memory} strokeColor="#e67700" showInfo={false} />
        </Col>
        <Col {...colSpan}>
          <Statistic title={t("components.resourcePanel.disk")} value={metric.disk} suffix="%" />
          <Progress percent={metric.disk} strokeColor="#1971c2" showInfo={false} />
        </Col>
        <Col {...colSpan}>
          <Statistic title={t("components.resourcePanel.networkIn")} value={metric.networkIn} suffix="MB/s" precision={2} />
          <Statistic title={t("components.resourcePanel.networkOut")} value={metric.networkOut} suffix="MB/s" precision={2} />
        </Col>
      </Row>
    </Card>
  );
}

export default RealtimeMetricsPanel;
