import { ReloadOutlined } from "@ant-design/icons";
import { Button, Card, Col, Empty, Row, Space, Spin, Statistic, Table, Tag, Typography, message } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { getLatestAnalysisReportApi } from "../../../api/analysis";
import { listTasksApi } from "../../../api/test";
import ActiveTaskSelector from "../../../components/ActiveTaskSelector";
import BottleneckTimeline from "../../../components/BottleneckTimeline";
import OptimizationSuggestionList from "../../../components/OptimizationSuggestionList";
import { useAppStore } from "../../../store/appStore";
import type { AnalysisReport } from "../../../types/analysis";
import type { TestTask } from "../../../types/test";
import { getRequestErrorMessage } from "../../../utils/request";
import { resolveActiveTaskId } from "../../../utils/taskSelection";

function AnalysisReportPage() {
  const [report, setReport] = useState<AnalysisReport | null>(null);
  const [tasks, setTasks] = useState<TestTask[]>([]);
  const [tasksLoading, setTasksLoading] = useState(true);
  const [reportLoading, setReportLoading] = useState(false);
  const [reportError, setReportError] = useState<string>();
  const { activeTaskId, setActiveTaskId } = useAppStore();
  const { t } = useTranslation();

  const selectedTask = useMemo(() => tasks.find((item) => item.id === activeTaskId), [tasks, activeTaskId]);

  useEffect(() => {
    void loadTasks();
  }, []);

  useEffect(() => {
    if (!activeTaskId) {
      setReport(null);
      setReportError(undefined);
      return;
    }
    void loadReport(activeTaskId, true);
  }, [activeTaskId]);

  const loadTasks = async () => {
    setTasksLoading(true);
    try {
      const taskList = await listTasksApi();
      setTasks(taskList);
      const resolvedTaskId = resolveActiveTaskId(taskList, activeTaskId);
      if (resolvedTaskId && resolvedTaskId !== activeTaskId) {
        setActiveTaskId(resolvedTaskId);
      }
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.loadFailed")));
      setTasks([]);
    } finally {
      setTasksLoading(false);
    }
  };

  const loadReport = async (taskId: number, silent = false) => {
    setReportLoading(true);
    setReportError(undefined);
    try {
      const data = await getLatestAnalysisReportApi(taskId);
      setReport(data);
      return true;
    } catch (error) {
      setReport(null);
      const msg = getRequestErrorMessage(error, t("common.loadFailed"));
      const noReport = msg === "Report not found";
      setReportError(noReport ? t("analysisReport.noReport") : msg);
      if (!silent && !noReport) {
        message.error(msg);
      }
      return false;
    } finally {
      setReportLoading(false);
    }
  };

  const refresh = async () => {
    if (!activeTaskId) {
      message.warning(t("testTask.selectTaskFirst"));
      return;
    }
    const refreshed = await loadReport(activeTaskId);
    if (refreshed) {
      message.success(t("analysisReport.triggerSuccess"));
    }
  };

  const renderStatus = (value?: string) => {
    if (!value) {
      return "--";
    }
    if (value === "RUNNING") {
      return t("common.statusRunning");
    }
    if (value === "PENDING") {
      return t("common.statusPending");
    }
    if (value === "SUCCESS") {
      return t("common.statusSuccess");
    }
    if (value === "FAILED") {
      return t("common.statusFailed");
    }
    if (value === "STOPPED") {
      return t("common.statusStopped");
    }
    return value;
  };

  const formatDateTime = (value?: string) => {
    if (!value) {
      return "--";
    }
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return value;
    }
    return new Intl.DateTimeFormat(undefined, {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit"
    }).format(parsed);
  };

  const renderSource = (value?: string) => {
    if (value === "DATA_RULE") {
      return t("analysisReport.sourceDataRule");
    }
    return value || "--";
  };

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space direction="vertical" size={16} style={{ width: "100%" }}>
              <div>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("analysisReport.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("analysisReport.subtitle")}</Typography.Text>
              </div>
              <Space style={{ width: "100%", justifyContent: "space-between" }} wrap align="start">
                <ActiveTaskSelector
                  label={t("analysisReport.currentTask")}
                  tasks={tasks}
                  value={activeTaskId}
                  loading={tasksLoading}
                  onChange={setActiveTaskId}
                />
                <Button type="primary" icon={<ReloadOutlined />} onClick={refresh} loading={reportLoading}>
                  {t("analysisReport.trigger")}
                </Button>
              </Space>
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={9}>
          <Card className="glass-card" title={t("analysisReport.listTitle")} style={{ borderRadius: 18 }}>
            <Table
              rowKey="id"
              size="small"
              loading={tasksLoading}
              pagination={false}
              dataSource={tasks}
              locale={{ emptyText: t("common.noData") }}
              onRow={(record) => ({
                onClick: () => setActiveTaskId(record.id),
                style: {
                  cursor: "pointer",
                  background: activeTaskId === record.id ? "#e7f5ff" : "transparent"
                }
              })}
              columns={[
                {
                  title: t("analysisReport.colTask"),
                  dataIndex: "id",
                  width: 90,
                  render: (value: number, record: TestTask) => (
                    <Space size={6}>
                      <span>{value}</span>
                      {activeTaskId === record.id ? <Tag color="blue">{t("analysisReport.current")}</Tag> : null}
                    </Space>
                  )
                },
                { title: t("analysisReport.colPlan"), dataIndex: "planName" },
                { title: t("analysisReport.colStatus"), dataIndex: "status", render: (value: string) => <Tag>{renderStatus(value)}</Tag> }
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} lg={15}>
          <Card className="glass-card" title={t("analysisReport.detailTitle")} style={{ borderRadius: 18 }}>
            {reportLoading ? (
              <div style={{ minHeight: 220, display: "grid", placeItems: "center" }}>
                <Spin tip={t("analysisReport.loading")} />
              </div>
            ) : !activeTaskId ? (
              <Empty description={t("testTask.selectTaskFirst")} />
            ) : !report ? (
              <Empty description={reportError || t("analysisReport.noReport")} />
            ) : (
              <>
                <Space direction="vertical" size={4} style={{ width: "100%", marginBottom: 16 }}>
                  <Typography.Text type="secondary">{t("analysisReport.currentViewing")}</Typography.Text>
                  <Typography.Title level={4} style={{ margin: 0 }}>
                    {selectedTask?.planName || "--"}
                  </Typography.Title>
                  <Space wrap>
                    <Tag color="blue">{t("analysisReport.taskTag", { taskId: selectedTask?.id || report.taskId })}</Tag>
                    <Tag>{selectedTask?.sceneName || "--"}</Tag>
                    <Tag color="processing">{renderStatus(report.status)}</Tag>
                  </Space>
                </Space>

                <Row gutter={[12, 12]}>
                  <Col span={6}>
                    <Statistic title={t("analysisReport.task")} value={selectedTask?.id || report.taskId} />
                  </Col>
                  <Col span={6}>
                    <Statistic title={t("analysisReport.runId")} value={report.runId} />
                  </Col>
                  <Col span={6}>
                    <Statistic title={t("analysisReport.grade")} value={report.grade} valueStyle={{ color: report.grade === "A" ? "#2b8a3e" : report.grade === "D" ? "#c92a2a" : "#e67700" }} />
                  </Col>
                  <Col span={6}>
                    <Statistic title={t("analysisReport.score")} value={report.score} suffix="/100" />
                  </Col>
                </Row>
                <Space wrap style={{ marginTop: 12 }}>
                  <Tag>{selectedTask?.planName || "--"}</Tag>
                  <Tag color="processing">{renderStatus(report.status)}</Tag>
                  <Tag color="geekblue">{renderSource(report.source)}</Tag>
                  <Tag>{t("analysisReport.createdAt")}: {formatDateTime(report.createdAt)}</Tag>
                </Space>
                <Typography.Paragraph style={{ marginBottom: 0, marginTop: 12 }}>
                  {report.summary}
                </Typography.Paragraph>
              </>
            )}
          </Card>
        </Col>

        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space wrap>
              <Tag color="geekblue">{report ? renderSource(report.source) : t("analysisReport.pipeline")}</Tag>
              <Tag color="blue">{t("analysisReport.currentTask")}: #{selectedTask?.id ?? "--"}</Tag>
              <Tag color="orange">{t("analysisReport.bottlenecks")}: {report?.bottlenecks.length ?? 0}</Tag>
              <Tag color="green">{t("analysisReport.suggestions")}: {report?.suggestions.length ?? 0}</Tag>
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <BottleneckTimeline items={report?.bottlenecks ?? []} />
        </Col>
        <Col xs={24} lg={12}>
          <OptimizationSuggestionList items={report?.suggestions ?? []} />
        </Col>
      </Row>
    </div>
  );
}

export default AnalysisReportPage;
