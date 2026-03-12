import { ArrowRightOutlined, FireOutlined, RocketOutlined } from "@ant-design/icons";
import { Button, Card, Col, Row, Space, Statistic, Tag, Typography } from "antd";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import PerformanceChart from "../../components/PerformanceChart";
import { useWebSocket } from "../../hooks/useWebSocket";
import { useAppStore } from "../../store/appStore";

function DashboardPage() {
  const navigate = useNavigate();
  const { activeTaskId } = useAppStore();
  const { series, transport } = useWebSocket(activeTaskId);
  const { t } = useTranslation();

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
            <Space direction="vertical" size={8}>
              <Typography.Title level={2} style={{ margin: 0 }}>
                {t("dashboard.title")}
              </Typography.Title>
              <Typography.Text type="secondary">{t("dashboard.subtitle")}</Typography.Text>
              <Space>
                <Tag color={realtimeTagColor}>{realtimeLabel}</Tag>
                <Button type="primary" icon={<RocketOutlined />} onClick={() => navigate("/test/task")}>{t("dashboard.runTask")}</Button>
                <Button icon={<ArrowRightOutlined />} onClick={() => navigate("/analysis/report")}>{t("dashboard.openAiReport")}</Button>
              </Space>
            </Space>
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("dashboard.activeTasks")} value={1} prefix={<FireOutlined />} />
            <Typography.Text type="secondary">{t("dashboard.activeTasksDesc")}</Typography.Text>
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("dashboard.latestQps")} value={series.at(-1)?.qps ?? 0} suffix="req/s" precision={0} />
            <Typography.Text type="secondary">{t("dashboard.latestQpsDesc")}</Typography.Text>
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("dashboard.latestP99")} value={series.at(-1)?.p99 ?? 0} suffix="ms" precision={0} />
            <Typography.Text type="secondary">{t("dashboard.latestP99Desc")}</Typography.Text>
          </Card>
        </Col>

        <Col span={24}>
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
      </Row>
    </div>
  );
}

export default DashboardPage;
