import { ThunderboltOutlined } from "@ant-design/icons";
import { Button, Card, Col, Row, Space, Statistic, Table, Tag, Typography, message } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { getAnalysisReportApi, triggerAnalysisApi } from "../../../api/analysis";
import { listTasksApi } from "../../../api/test";
import BottleneckTimeline from "../../../components/BottleneckTimeline";
import OptimizationSuggestionList from "../../../components/OptimizationSuggestionList";
import type { AnalysisReport } from "../../../types/analysis";
import type { TestTask } from "../../../types/test";

function AnalysisReportPage() {
  const [report, setReport] = useState<AnalysisReport | null>(null);
  const [tasks, setTasks] = useState<TestTask[]>([]);
  const [selectedTaskId, setSelectedTaskId] = useState<number>();
  const { t } = useTranslation();

  useEffect(() => {
    getAnalysisReportApi().then((res) => {
      setReport(res);
      setSelectedTaskId(res.taskId);
    });
    listTasksApi().then(setTasks);
  }, []);

  const selectedTask = useMemo(() => tasks.find((item) => item.id === selectedTaskId), [tasks, selectedTaskId]);

  if (!report) {
    return <div className="page-shell">{t("analysisReport.loading")}</div>;
  }

  const trigger = async () => {
    await triggerAnalysisApi(selectedTaskId || report.taskId);
    message.success(t("analysisReport.triggerSuccess"));
  };

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space style={{ width: "100%", justifyContent: "space-between" }}>
              <div>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("analysisReport.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("analysisReport.subtitle")}</Typography.Text>
              </div>
              <Button type="primary" icon={<ThunderboltOutlined />} onClick={trigger}>
                {t("analysisReport.trigger")}
              </Button>
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={9}>
          <Card className="glass-card" title={t("analysisReport.listTitle")} style={{ borderRadius: 18 }}>
            <Table
              rowKey="id"
              size="small"
              pagination={false}
              dataSource={tasks}
              onRow={(record) => ({
                onClick: () => setSelectedTaskId(record.id),
                style: {
                  cursor: "pointer",
                  background: selectedTaskId === record.id ? "#e7f5ff" : "transparent"
                }
              })}
              columns={[
                { title: t("analysisReport.colTask"), dataIndex: "id", width: 70 },
                { title: t("analysisReport.colPlan"), dataIndex: "planName" },
                { title: t("analysisReport.colStatus"), dataIndex: "status", render: (value: string) => <Tag>{value === "RUNNING" ? t("common.statusRunning") : value === "PENDING" ? t("common.statusPending") : value === "SUCCESS" ? t("common.statusSuccess") : value === "FAILED" ? t("common.statusFailed") : value === "STOPPED" ? t("common.statusStopped") : value}</Tag> }
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} lg={15}>
          <Card className="glass-card" title={t("analysisReport.detailTitle")} style={{ borderRadius: 18 }}>
            <Row gutter={[12, 12]}>
              <Col span={8}>
                <Statistic title={t("analysisReport.task")} value={selectedTask?.id || report.taskId} />
              </Col>
              <Col span={8}>
                <Statistic title={t("analysisReport.grade")} value={report.grade} valueStyle={{ color: report.grade === "A" ? "#2b8a3e" : "#e67700" }} />
              </Col>
              <Col span={8}>
                <Statistic title={t("analysisReport.score")} value={report.score} suffix="/100" />
              </Col>
            </Row>
            <Typography.Paragraph style={{ marginBottom: 0, marginTop: 12 }}>
              {report.summary}
            </Typography.Paragraph>
          </Card>
        </Col>

        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space>
              <Tag color="geekblue">{t("analysisReport.pipeline")}</Tag>
              <Tag color="orange">{t("analysisReport.bottlenecks")}: {report.bottlenecks.length}</Tag>
              <Tag color="green">{t("analysisReport.suggestions")}: {report.suggestions.length}</Tag>
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <BottleneckTimeline items={report.bottlenecks} />
        </Col>
        <Col xs={24} lg={12}>
          <OptimizationSuggestionList items={report.suggestions} />
        </Col>
      </Row>
    </div>
  );
}

export default AnalysisReportPage;

