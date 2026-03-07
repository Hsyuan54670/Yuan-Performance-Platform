import { DownloadOutlined } from "@ant-design/icons";
import { Button, Card, Col, Row, Space, Table, Tag, Typography, message } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { compareReportsApi, exportReportApi, listReportsApi } from "../../api/report";
import PerformanceChart from "../../components/PerformanceChart";
import type { ComparePoint, ReportItem } from "../../types/report";
import { formatDateTime } from "../../utils/format";

function ReportPage() {
  const [reports, setReports] = useState<ReportItem[]>([]);
  const [compare, setCompare] = useState<ComparePoint[]>([]);
  const { t } = useTranslation();

  useEffect(() => {
    listReportsApi().then(setReports);
    compareReportsApi().then(setCompare);
  }, []);

  const xAxis = useMemo(() => compare.map((item) => item.label), [compare]);

  const doExport = async () => {
    await exportReportApi();
    message.success(t("report.exportSuccess"));
  };

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space style={{ width: "100%", justifyContent: "space-between" }}>
              <div>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("report.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("report.subtitle")}</Typography.Text>
              </div>
              <Button type="primary" icon={<DownloadOutlined />} onClick={doExport}>
                {t("report.exportCurrent")}
              </Button>
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={14}>
          <Card className="glass-card" title={t("report.listTitle")} style={{ borderRadius: 18 }}>
            <Table
              rowKey="id"
              dataSource={reports}
              pagination={{ pageSize: 7 }}
              columns={[
                { title: t("report.colId"), dataIndex: "id", width: 80 },
                { title: t("report.colTask"), dataIndex: "taskId", width: 90 },
                { title: t("report.colTitle"), dataIndex: "title" },
                {
                  title: t("report.colGrade"),
                  dataIndex: "grade",
                  render: (value: string) => <Tag color={value === "A" ? "green" : value === "B" ? "blue" : "orange"}>{value}</Tag>
                },
                { title: t("report.colCreated"), dataIndex: "createdAt", render: (v: string) => formatDateTime(v) }
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} lg={10}>
          <Card className="glass-card" title={t("report.detailPreview")} style={{ borderRadius: 18 }}>
            <Typography.Paragraph style={{ marginTop: 0 }}>{reports[0]?.summary}</Typography.Paragraph>
            <Tag color="processing">{t("report.exportSupported")}</Tag>
            <Tag color="geekblue" style={{ marginTop: 10 }}>{t("report.templateReady")}</Tag>
          </Card>
        </Col>

        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <PerformanceChart
              title={t("report.comparisonTitle")}
              xAxis={xAxis}
              smooth={false}
              yAxisName={t("report.comparisonYAxis")}
              series={[
                { name: t("report.seriesBaseline"), color: "#1c7ed6", data: compare.map((item) => item.baseline) },
                { name: t("report.seriesCurrent"), color: "#e8590c", data: compare.map((item) => item.current) }
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default ReportPage;
