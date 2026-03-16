import { ArrowRightOutlined, FireOutlined, RocketOutlined } from "@ant-design/icons";
import { Button, Card, Col, Row, Space, Statistic, Tag, Typography, message } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { listTasksApi } from "../../api/test";
import ActiveTaskSelector from "../../components/ActiveTaskSelector";
import PerformanceChart from "../../components/PerformanceChart";
import { useWebSocket } from "../../hooks/useWebSocket";
import { useAppStore } from "../../store/appStore";
import type { TestTask } from "../../types/test";
import { resolveActiveTaskId } from "../../utils/taskSelection";
import { getRequestErrorMessage } from "../../utils/request";

function DashboardPage() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [tasks, setTasks] = useState<TestTask[]>([]);
  const [tasksLoading, setTasksLoading] = useState(true);
  const { activeTaskId, setActiveTaskId } = useAppStore();
  const { series, transport } = useWebSocket(activeTaskId);

  const loadTasks = useCallback(async () => {
    setTasksLoading(true);
    try {
      const taskList = await listTasksApi();
      setTasks(taskList);
      const resolvedTaskId = resolveActiveTaskId(taskList, activeTaskId);
      if (resolvedTaskId && resolvedTaskId !== activeTaskId) {
        setActiveTaskId(resolvedTaskId);
      }
    } catch (error) {
      setTasks([]);
      message.error(getRequestErrorMessage(error, t("common.loadFailed")));
    } finally {
      setTasksLoading(false);
    }
  }, [activeTaskId, setActiveTaskId, t]);

  useEffect(() => {
    void loadTasks();
  }, [loadTasks]);

  const currentTask = useMemo(() => tasks.find((task) => task.id === activeTaskId), [tasks, activeTaskId]);
  const runningTaskCount = useMemo(() => tasks.filter((task) => task.status === "RUNNING").length, [tasks]);
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
                <ActiveTaskSelector
                  label={t("dashboard.currentTask")}
                  tasks={tasks}
                  value={activeTaskId}
                  loading={tasksLoading}
                  onChange={setActiveTaskId}
                />
                <Space wrap>
                  <Tag color={realtimeTagColor}>{realtimeLabel}</Tag>
                  <Button type="primary" icon={<RocketOutlined />} onClick={() => navigate("/test/task")}>{t("dashboard.runTask")}</Button>
                  <Button icon={<ArrowRightOutlined />} onClick={() => navigate("/analysis/report")}>{t("dashboard.openAiReport")}</Button>
                </Space>
              </Space>
              <Space wrap>
                <Tag color="blue">{t("analysisReport.taskTag", { taskId: currentTask?.id ?? "--" })}</Tag>
                <Tag>{currentTask?.planName || "--"}</Tag>
                <Tag>{currentTask?.sceneName || "--"}</Tag>
              </Space>
            </Space>
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 16 }}>
            <Statistic title={t("dashboard.activeTasks")} value={runningTaskCount} prefix={<FireOutlined />} />
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
