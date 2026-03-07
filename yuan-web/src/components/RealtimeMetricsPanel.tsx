import { Card, Col, Progress, Row, Statistic } from "antd";
import { useTranslation } from "react-i18next";
import type { SystemMetric } from "../types/monitor";

interface RealtimeMetricsPanelProps {
  metric: SystemMetric;
}

function RealtimeMetricsPanel({ metric }: RealtimeMetricsPanelProps) {
  const { t } = useTranslation();

  return (
    <Card className="glass-card" title={t("components.resourcePanel.title")}>
      <Row gutter={[14, 14]}>
        <Col xs={24} md={12} lg={6}>
          <Statistic title={t("components.resourcePanel.cpu")} value={metric.cpu} suffix="%" />
          <Progress percent={metric.cpu} strokeColor="#0b7285" showInfo={false} />
        </Col>
        <Col xs={24} md={12} lg={6}>
          <Statistic title={t("components.resourcePanel.memory")} value={metric.memory} suffix="%" />
          <Progress percent={metric.memory} strokeColor="#e67700" showInfo={false} />
        </Col>
        <Col xs={24} md={12} lg={6}>
          <Statistic title={t("components.resourcePanel.disk")} value={metric.disk} suffix="%" />
          <Progress percent={metric.disk} strokeColor="#1971c2" showInfo={false} />
        </Col>
        <Col xs={24} md={12} lg={6}>
          <Statistic title={t("components.resourcePanel.networkIn")} value={metric.networkIn} suffix="MB/s" precision={2} />
          <Statistic title={t("components.resourcePanel.networkOut")} value={metric.networkOut} suffix="MB/s" precision={2} />
        </Col>
      </Row>
    </Card>
  );
}

export default RealtimeMetricsPanel;
