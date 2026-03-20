import { DownloadOutlined, FileSearchOutlined, FundProjectionScreenOutlined } from "@ant-design/icons";
import { Button, Card, Col, DatePicker, Empty, Row, Select, Space, Spin, Statistic, Table, Tag, Typography, message } from "antd";
import type { Dayjs } from "dayjs";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import { getReportByRunApi, getReportHtmlApi, listReportsApi, type ReportQueryParams } from "../../api/report";
import { listTasksApi } from "../../api/test";
import { useAppStore } from "../../store/appStore";
import type { AnalysisReport } from "../../types/analysis";
import type { ReportSummary } from "../../types/report";
import type { TestTask } from "../../types/test";
import { formatDateTime } from "../../utils/format";
import { PermissionCodes } from "../../utils/permissions";
import { getRequestErrorMessage } from "../../utils/request";
import { renderTaskStatus, taskStatusColorMap } from "../../utils/taskStatus";

type ReportDateRange = [Dayjs, Dayjs] | null;

function ReportPage() {
  const [tasks, setTasks] = useState<TestTask[]>([]);
  const [reports, setReports] = useState<ReportSummary[]>([]);
  const [selectedRunId, setSelectedRunId] = useState(0);
  const [selectedTaskFilter, setSelectedTaskFilter] = useState<number>();
  const [selectedGradeFilter, setSelectedGradeFilter] = useState<string>();
  const [selectedDateRange, setSelectedDateRange] = useState<ReportDateRange>(null);
  const [detail, setDetail] = useState<AnalysisReport | null>(null);
  const [tasksLoading, setTasksLoading] = useState(true);
  const [reportsLoading, setReportsLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { activeRunId, setActiveTaskId, setActiveRunId } = useAppStore();
  const { hasPermission } = useAuth();
  const canExportReports = hasPermission(PermissionCodes.REPORT_CENTER_EXPORT);
  const canOpenAnalysis = hasPermission(PermissionCodes.ANALYSIS_REPORT_VIEW);

  const selectedSummary = useMemo(
    () => reports.find((item) => item.runId === selectedRunId) ?? null,
    [reports, selectedRunId]
  );
  const tasksById = useMemo(() => new Map(tasks.map((task) => [task.id, task])), [tasks]);
  const selectedTask = useMemo(
    () => (selectedSummary ? tasksById.get(selectedSummary.taskId) : undefined),
    [selectedSummary, tasksById]
  );
  const stableGradeCount = useMemo(
    () => reports.filter((item) => item.grade === "A" || item.grade === "B").length,
    [reports]
  );

  const renderSource = (value?: string) => {
    if (value === "DATA_RULE") {
      return t("analysisReport.sourceDataRule");
    }
    return value || "--";
  };

  const isNotFoundError = (error: unknown) => {
    const errorMessage = getRequestErrorMessage(error, "");
    return errorMessage.includes("404") || errorMessage.includes("Not Found") || errorMessage === "Report not found";
  };

  const buildReportQueryParams = useCallback((): ReportQueryParams => {
    const [createdFrom, createdTo] = selectedDateRange ?? [];
    return {
      taskId: selectedTaskFilter,
      grade: selectedGradeFilter,
      createdFrom: createdFrom?.startOf("day").format("YYYY-MM-DDTHH:mm:ss"),
      createdTo: createdTo?.endOf("day").format("YYYY-MM-DDTHH:mm:ss")
    };
  }, [selectedDateRange, selectedGradeFilter, selectedTaskFilter]);

  const loadTasks = useCallback(async () => {
    setTasksLoading(true);
    try {
      const taskList = await listTasksApi();
      setTasks(taskList);
    } catch (error) {
      setTasks([]);
      message.error(getRequestErrorMessage(error, t("common.loadFailed")));
    } finally {
      setTasksLoading(false);
    }
  }, [t]);

  const loadReports = useCallback(async () => {
    setReportsLoading(true);
    try {
      const reportList = await listReportsApi(buildReportQueryParams());
      setReports(reportList);
      const preferredRunId = [selectedRunId, activeRunId].find((runId) => runId && reportList.some((item) => item.runId === runId));
      setSelectedRunId((preferredRunId as number) || reportList[0]?.runId || 0);
    } catch (error) {
      setReports([]);
      setSelectedRunId(0);
      if (!isNotFoundError(error)) {
        message.error(getRequestErrorMessage(error, t("common.loadFailed")));
      }
    } finally {
      setReportsLoading(false);
    }
  }, [activeRunId, buildReportQueryParams, selectedRunId, t]);

  const loadDetail = useCallback(
    async (runId: number) => {
      setDetailLoading(true);
      try {
        const data = await getReportByRunApi(runId);
        setDetail(data);
      } catch (error) {
        setDetail(null);
        if (!isNotFoundError(error)) {
          message.error(getRequestErrorMessage(error, t("common.loadFailed")));
        }
      } finally {
        setDetailLoading(false);
      }
    },
    [t]
  );

  useEffect(() => {
    void loadTasks();
  }, [loadTasks]);

  useEffect(() => {
    void loadReports();
  }, [loadReports]);

  useEffect(() => {
    if (!selectedRunId) {
      setDetail(null);
      return;
    }
    void loadDetail(selectedRunId);
  }, [loadDetail, selectedRunId]);

  const exportCurrentReport = useCallback(async () => {
    if (!selectedSummary) {
      message.info(t("report.selectReport"));
      return;
    }
    try {
      const html = await getReportHtmlApi(selectedSummary.runId);
      const blob = new Blob([html], { type: "text/html;charset=utf-8" });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `analysis-report-task-${selectedSummary.taskId}-run-${selectedSummary.runId}.html`;
      document.body.appendChild(anchor);
      anchor.click();
      document.body.removeChild(anchor);
      URL.revokeObjectURL(url);
      message.success(t("report.exportSuccess"));
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.loadFailed")));
    }
  }, [selectedSummary, t]);

  const openHtmlPreview = useCallback(async () => {
    if (!selectedSummary) {
      message.info(t("report.selectReport"));
      return;
    }
    try {
      const html = await getReportHtmlApi(selectedSummary.runId);
      const blob = new Blob([html], { type: "text/html;charset=utf-8" });
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank", "noopener,noreferrer");
      window.setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.loadFailed")));
    }
  }, [selectedSummary, t]);

  const openAnalysisPage = useCallback(() => {
    if (!selectedSummary) {
      message.info(t("report.selectReport"));
      return;
    }
    setActiveTaskId(selectedSummary.taskId);
    setActiveRunId(selectedSummary.runId);
    navigate("/analysis/report");
  }, [navigate, selectedSummary, setActiveRunId, setActiveTaskId, t]);

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space direction="vertical" size={16} style={{ width: "100%" }}>
              <div>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("report.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("report.subtitle")}</Typography.Text>
              </div>
              <Space size={[12, 12]} wrap>
                <Select
                  allowClear
                  showSearch
                  style={{ width: 280 }}
                  loading={tasksLoading}
                  value={selectedTaskFilter}
                  placeholder={t("report.filterTask")}
                  optionFilterProp="label"
                  onChange={(value) => setSelectedTaskFilter(value)}
                  options={tasks.map((task) => ({
                    label: `#${task.id} ${task.planName}`,
                    value: task.id
                  }))}
                />
                <Select
                  allowClear
                  style={{ width: 120 }}
                  value={selectedGradeFilter}
                  placeholder={t("report.filterGrade")}
                  onChange={(value) => setSelectedGradeFilter(value)}
                  options={["A", "B", "C", "D", "E"].map((grade) => ({ label: grade, value: grade }))}
                />
                <DatePicker.RangePicker
                  value={selectedDateRange}
                  onChange={(value) => setSelectedDateRange(value as ReportDateRange)}
                  placeholder={[t("report.createdFrom"), t("report.createdTo")]}
                  allowEmpty={[true, true]}
                />
              </Space>
              <Space style={{ width: "100%", justifyContent: "flex-end" }} wrap>
                <Space wrap>
                  {canOpenAnalysis ? (
                    <Button icon={<FundProjectionScreenOutlined />} disabled={!selectedSummary} onClick={openAnalysisPage}>
                      {t("report.openAnalysis")}
                    </Button>
                  ) : null}
                  {canExportReports ? (
                    <Button icon={<FileSearchOutlined />} disabled={!selectedSummary} onClick={openHtmlPreview}>
                      {t("report.openHtml")}
                    </Button>
                  ) : null}
                  {canExportReports ? (
                    <Button type="primary" icon={<DownloadOutlined />} disabled={!selectedSummary} onClick={exportCurrentReport}>
                      {t("report.exportCurrent")}
                    </Button>
                  ) : null}
                </Space>
              </Space>
            </Space>
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Statistic title={t("report.totalReports")} value={reports.length} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Statistic title={t("report.stableReports")} value={stableGradeCount} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Statistic title={t("report.selectedScore")} value={detail?.score} suffix={detail ? "/100" : undefined} formatter={(value) => value ?? "--"} />
          </Card>
        </Col>

        <Col xs={24} lg={13}>
          <Card className="glass-card" title={t("report.listTitle")} style={{ borderRadius: 18 }}>
            <Table<ReportSummary>
              rowKey="id"
              loading={reportsLoading}
              dataSource={reports}
              locale={{ emptyText: t("report.noReports") }}
              pagination={{ pageSize: 8, hideOnSinglePage: true }}
              onRow={(record) => ({
                onClick: () => setSelectedRunId(record.runId)
              })}
              rowClassName={(record) => (record.runId === selectedRunId ? "ant-table-row-selected" : "")}
              columns={[
                {
                  title: t("report.colTask"),
                  key: "task",
                  render: (_, record) => {
                    const task = tasksById.get(record.taskId);
                    return (
                      <Space direction="vertical" size={0}>
                        <Typography.Text strong>{`#${record.taskId}`}</Typography.Text>
                        <Typography.Text type="secondary" style={{ fontSize: 12 }}>{task?.planName || "--"}</Typography.Text>
                      </Space>
                    );
                  }
                },
                { title: t("report.colRun"), dataIndex: "runId", width: 100 },
                {
                  title: t("report.colGrade"),
                  dataIndex: "grade",
                  width: 92,
                  render: (value: string) => <Tag color={value === "A" ? "green" : value === "B" ? "blue" : value === "C" ? "gold" : "red"}>{value}</Tag>
                },
                {
                  title: t("report.colStatus"),
                  dataIndex: "status",
                  width: 110,
                  render: (value: string) => <Tag color={taskStatusColorMap[value] ?? "default"}>{renderTaskStatus(t, value)}</Tag>
                },
                {
                  title: t("report.colCreated"),
                  dataIndex: "createdAt",
                  width: 180,
                  render: (value: string) => formatDateTime(value)
                }
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} lg={11}>
          <Card className="glass-card" title={t("report.detailPreview")} style={{ borderRadius: 18 }}>
            {detailLoading ? (
              <div style={{ minHeight: 280, display: "grid", placeItems: "center" }}>
                <Spin />
              </div>
            ) : !selectedSummary ? (
              <Empty description={t("report.selectReport")} />
            ) : (
              <Space direction="vertical" size={18} style={{ width: "100%" }}>
                <div>
                  <Typography.Text type="secondary">{t("report.selectedReport")}</Typography.Text>
                  <Typography.Title level={4} style={{ margin: 0 }}>
                    {selectedTask?.planName || `task #${selectedSummary.taskId}`}
                  </Typography.Title>
                  <Space wrap style={{ marginTop: 8 }}>
                    <Tag color="blue">{`task #${selectedSummary.taskId}`}</Tag>
                    <Tag color="geekblue">{`run #${selectedSummary.runId}`}</Tag>
                    {selectedTask?.sceneName ? <Tag>{selectedTask.sceneName}</Tag> : null}
                    <Tag color={taskStatusColorMap[selectedSummary.status] ?? "default"}>{renderTaskStatus(t, selectedSummary.status)}</Tag>
                  </Space>
                </div>

                <Row gutter={[12, 12]}>
                  <Col span={8}>
                    <Statistic title={t("report.colGrade")} value={selectedSummary.grade} />
                  </Col>
                  <Col span={8}>
                    <Statistic title={t("report.colScore")} value={selectedSummary.score} suffix="/100" />
                  </Col>
                  <Col span={8}>
                    <Statistic title={t("report.bottleneckCount")} value={detail?.bottlenecks.length ?? 0} />
                  </Col>
                </Row>

                <Space wrap>
                  <Tag>{`${t("report.createdAtLabel")}: ${formatDateTime(selectedSummary.createdAt)}`}</Tag>
                  <Tag color="processing">{renderSource(selectedSummary.source)}</Tag>
                </Space>

                <Typography.Paragraph style={{ marginBottom: 0 }}>
                  {detail?.summary || selectedSummary.summary || t("common.noData")}
                </Typography.Paragraph>
              </Space>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default ReportPage;
