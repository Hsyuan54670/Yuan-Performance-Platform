import { PlusOutlined } from "@ant-design/icons";
import { Button, Card, Col, Drawer, Form, Input, InputNumber, Row, Select, Space, Switch, Table, Tag, Typography } from "antd";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { listAlertRecordsApi, listAlertRulesApi } from "../../../api/monitor";
import type { AlertRecord, AlertRule } from "../../../types/monitor";
import { formatDateTime } from "../../../utils/format";

function MonitorAlertPage() {
  const [rules, setRules] = useState<AlertRule[]>([]);
  const [records, setRecords] = useState<AlertRecord[]>([]);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm<AlertRule>();
  const { t } = useTranslation();

  useEffect(() => {
    listAlertRulesApi().then(setRules);
    listAlertRecordsApi().then(setRecords);
  }, []);

  const addRule = async () => {
    const values = await form.validateFields();
    setRules((prev) => [...prev, { ...values, id: Date.now() }]);
    setOpen(false);
    form.resetFields();
  };

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space style={{ width: "100%", justifyContent: "space-between" }}>
              <div>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("monitorAlert.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("monitorAlert.subtitle")}</Typography.Text>
              </div>
              <Button type="primary" icon={<PlusOutlined />} onClick={() => setOpen(true)}>
                {t("monitorAlert.newRule")}
              </Button>
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card className="glass-card" title={t("monitorAlert.rulesTitle")} style={{ borderRadius: 18 }}>
            <Table
              rowKey="id"
              dataSource={rules}
              pagination={{ pageSize: 6 }}
              columns={[
                { title: t("monitorAlert.colName"), dataIndex: "name" },
                { title: t("monitorAlert.colMetric"), dataIndex: "metric" },
                { title: t("monitorAlert.colOp"), dataIndex: "op" },
                { title: t("monitorAlert.colThreshold"), dataIndex: "threshold" },
                {
                  title: t("monitorAlert.colLevel"),
                  dataIndex: "level",
                  render: (value: string) => <Tag color={value === "CRITICAL" ? "red" : value === "WARN" ? "orange" : "blue"}>{value}</Tag>
                },
                { title: t("monitorAlert.colEnabled"), dataIndex: "enabled", render: (value: boolean) => <Switch checked={value} size="small" /> }
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card className="glass-card" title={t("monitorAlert.recordsTitle")} style={{ borderRadius: 18 }}>
            <Table
              rowKey="id"
              dataSource={records}
              pagination={{ pageSize: 6 }}
              columns={[
                { title: t("monitorAlert.colTask"), dataIndex: "taskId" },
                { title: t("monitorAlert.colRule"), dataIndex: "ruleName" },
                {
                  title: t("monitorAlert.colLevel"),
                  dataIndex: "level",
                  render: (value: string) => <Tag color={value === "CRITICAL" ? "red" : "orange"}>{value}</Tag>
                },
                { title: t("monitorAlert.colCurrent"), dataIndex: "currentValue" },
                { title: t("monitorAlert.colTime"), dataIndex: "createdAt", render: (v: string) => formatDateTime(v) }
              ]}
            />
          </Card>
        </Col>
      </Row>

      <Drawer
        title={t("monitorAlert.drawerTitle")}
        open={open}
        onClose={() => setOpen(false)}
        extra={<Button type="primary" onClick={addRule}>{t("common.save")}</Button>}
      >
        <Form layout="vertical" form={form} initialValues={{ enabled: true, op: ">", level: "WARN" }}>
          <Form.Item name="name" label={t("monitorAlert.fieldRuleName")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="metric" label={t("monitorAlert.fieldMetric")} rules={[{ required: true }]}>
            <Select options={["CPU", "MEMORY", "P99", "ERROR_RATE"].map((v) => ({ label: v, value: v }))} />
          </Form.Item>
          <Form.Item name="op" label={t("monitorAlert.fieldOperator")}>
            <Select options={[{ label: ">", value: ">" }, { label: ">=", value: ">=" }]} />
          </Form.Item>
          <Form.Item name="threshold" label={t("monitorAlert.fieldThreshold")} rules={[{ required: true }]}>
            <InputNumber style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="level" label={t("monitorAlert.fieldLevel")}>
            <Select options={["INFO", "WARN", "CRITICAL"].map((v) => ({ label: v, value: v }))} />
          </Form.Item>
          <Form.Item name="enabled" label={t("monitorAlert.fieldEnabled")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Drawer>
    </div>
  );
}

export default MonitorAlertPage;
