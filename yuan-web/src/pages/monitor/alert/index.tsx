import { DeleteOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons";
import {
  Button,
  Card,
  Col,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  Typography,
  message
} from "antd";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAuth } from "../../../hooks/useAuth";
import {
  createAlertRuleApi,
  deleteAlertRuleApi,
  listActiveAlertsApi,
  listAlertRecordsApi,
  listAlertRulesApi,
  switchAlertRuleApi,
  updateAlertRuleApi
} from "../../../api/monitor";
import type { ActiveAlert, AlertRecord, AlertRule, AlertRulePayload } from "../../../types/monitor";
import { formatDateTime } from "../../../utils/format";
import { PermissionCodes } from "../../../utils/permissions";
import { getRequestErrorMessage } from "../../../utils/request";

const metricOptions: AlertRulePayload["metric"][] = ["CPU", "MEMORY", "P99", "ERROR_RATE"];
const operatorOptions: AlertRulePayload["op"][] = [">", ">="];
const levelOptions: AlertRulePayload["level"][] = ["INFO", "WARN", "CRITICAL"];

function MonitorAlertPage() {
  const [rules, setRules] = useState<AlertRule[]>([]);
  const [records, setRecords] = useState<AlertRecord[]>([]);
  const [activeAlerts, setActiveAlerts] = useState<ActiveAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<AlertRule | null>(null);
  const [form] = Form.useForm<AlertRulePayload>();
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const canManageAlerts = hasPermission(PermissionCodes.MONITOR_ALERT_WRITE);

  const loadAlerts = async () => {
    setLoading(true);
    try {
      const [ruleRows, recordRows, activeRows] = await Promise.all([
        listAlertRulesApi(),
        listAlertRecordsApi(),
        listActiveAlertsApi()
      ]);
      setRules(ruleRows);
      setRecords(recordRows);
      setActiveAlerts(activeRows);
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.loadFailed")));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadAlerts();
  }, []);

  const openCreateDrawer = () => {
    setEditingRule(null);
    form.setFieldsValue({
      name: "",
      metric: "CPU",
      op: ">=",
      threshold: 80,
      level: "WARN",
      enabled: true
    });
    setDrawerOpen(true);
  };

  const openEditDrawer = (rule: AlertRule) => {
    setEditingRule(rule);
    form.setFieldsValue({
      name: rule.name,
      metric: rule.metric,
      op: rule.op,
      threshold: rule.threshold,
      level: rule.level,
      enabled: rule.enabled
    });
    setDrawerOpen(true);
  };

  const closeDrawer = () => {
    setDrawerOpen(false);
    setEditingRule(null);
    form.resetFields();
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      if (editingRule) {
        await updateAlertRuleApi(editingRule.id, values);
        message.success(t("monitorAlert.updateSuccess"));
      } else {
        await createAlertRuleApi(values);
        message.success(t("monitorAlert.createSuccess"));
      }
      closeDrawer();
      await loadAlerts();
    } catch (error) {
      if (error instanceof Error && error.message) {
        message.error(getRequestErrorMessage(error, t("common.saveFailed")));
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleSwitch = async (rule: AlertRule, enabled: boolean) => {
    try {
      await switchAlertRuleApi(rule.id, enabled);
      setRules((current) => current.map((item) => (item.id === rule.id ? { ...item, enabled } : item)));
      message.success(t("monitorAlert.switchSuccess"));
      await loadAlerts();
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.saveFailed")));
    }
  };

  const handleDelete = async (ruleId: number) => {
    try {
      await deleteAlertRuleApi(ruleId);
      setRules((current) => current.filter((item) => item.id !== ruleId));
      message.success(t("monitorAlert.deleteSuccess"));
      await loadAlerts();
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.deleteFailed")));
    }
  };

  const renderLevelTag = (value: string) => (
    <Tag color={value === "CRITICAL" ? "red" : value === "WARN" ? "orange" : "blue"}>{value}</Tag>
  );

  const renderEventTypeTag = (value: AlertRecord["eventType"]) => (
    <Tag color={value === "TRIGGER" ? "volcano" : "green"}>
      {value === "TRIGGER" ? t("monitorAlert.eventTrigger") : t("monitorAlert.eventRecover")}
    </Tag>
  );

  const criticalCount = activeAlerts.filter((item) => item.level === "CRITICAL").length;
  const warnCount = activeAlerts.filter((item) => item.level === "WARN").length;

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space style={{ width: "100%", justifyContent: "space-between" }} wrap>
              <Space direction="vertical" size={4}>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("monitorAlert.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("monitorAlert.subtitle")}</Typography.Text>
                <Typography.Text type="secondary">{t("monitorAlert.backendNote")}</Typography.Text>
              </Space>
              {canManageAlerts ? (
                <Button type="primary" icon={<PlusOutlined />} onClick={openCreateDrawer}>
                  {t("monitorAlert.newRule")}
                </Button>
              ) : null}
            </Space>
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Statistic title={t("monitorAlert.activeCount")} value={activeAlerts.length} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Statistic title={t("monitorAlert.criticalCount")} value={criticalCount} valueStyle={{ color: "#cf1322" }} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Statistic title={t("monitorAlert.warnCount")} value={warnCount} valueStyle={{ color: "#d48806" }} />
          </Card>
        </Col>

        <Col span={24}>
          <Card className="glass-card" title={t("monitorAlert.activeTitle")} style={{ borderRadius: 18 }}>
            <Table
              rowKey={(record) => `${record.ruleId}-${record.taskId}-${record.runId}`}
              loading={loading}
              dataSource={activeAlerts}
              locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t("monitorAlert.noActiveAlerts")} /> }}
              pagination={{ pageSize: 5 }}
              columns={[
                { title: t("monitorAlert.colTask"), dataIndex: "taskId" },
                { title: t("monitorAlert.colRun"), dataIndex: "runId", render: (value?: number) => value ?? "-" },
                { title: t("monitorAlert.colRule"), dataIndex: "ruleName" },
                { title: t("monitorAlert.colMetric"), dataIndex: "metric" },
                { title: t("monitorAlert.colOp"), dataIndex: "op" },
                { title: t("monitorAlert.colThreshold"), dataIndex: "threshold" },
                { title: t("monitorAlert.colCurrent"), dataIndex: "latestValue" },
                {
                  title: t("monitorAlert.colLevel"),
                  dataIndex: "level",
                  render: (value: string) => renderLevelTag(value)
                },
                {
                  title: t("monitorAlert.colTriggeredAt"),
                  dataIndex: "latestTriggeredAt",
                  render: (value: string) => formatDateTime(value)
                }
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card className="glass-card" title={t("monitorAlert.rulesTitle")} style={{ borderRadius: 18 }}>
            <Table
              rowKey="id"
              loading={loading}
              dataSource={rules}
              locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t("common.noData")} /> }}
              pagination={{ pageSize: 6 }}
              columns={[
                { title: t("monitorAlert.colName"), dataIndex: "name" },
                { title: t("monitorAlert.colMetric"), dataIndex: "metric" },
                { title: t("monitorAlert.colOp"), dataIndex: "op" },
                { title: t("monitorAlert.colThreshold"), dataIndex: "threshold" },
                {
                  title: t("monitorAlert.colLevel"),
                  dataIndex: "level",
                  render: (value: string) => renderLevelTag(value)
                },
                {
                  title: t("monitorAlert.colEnabled"),
                  dataIndex: "enabled",
                  render: (value: boolean, record: AlertRule) => (
                    <Switch checked={value} disabled={!canManageAlerts} onChange={(checked) => void handleSwitch(record, checked)} />
                  )
                },
                ...(canManageAlerts
                  ? [{
                      title: t("monitorAlert.colAction"),
                      key: "action",
                      render: (_: unknown, record: AlertRule) => (
                        <Space size="small">
                          <Button type="link" icon={<EditOutlined />} onClick={() => openEditDrawer(record)}>
                            {t("monitorAlert.editRule")}
                          </Button>
                          <Popconfirm
                            title={t("monitorAlert.deleteConfirm")}
                            okText={t("monitorAlert.confirmDelete")}
                            cancelText={t("monitorAlert.cancel")}
                            onConfirm={() => void handleDelete(record.id)}
                          >
                            <Button type="link" danger icon={<DeleteOutlined />}>
                              {t("monitorAlert.deleteRule")}
                            </Button>
                          </Popconfirm>
                        </Space>
                      )
                    }]
                  : [])
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card className="glass-card" title={t("monitorAlert.recordsTitle")} style={{ borderRadius: 18 }}>
            <Table
              rowKey="id"
              loading={loading}
              dataSource={records}
              locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t("common.noData")} /> }}
              pagination={{ pageSize: 6 }}
              columns={[
                { title: t("monitorAlert.colTask"), dataIndex: "taskId" },
                { title: t("monitorAlert.colRun"), dataIndex: "runId", render: (value?: number) => value ?? "-" },
                { title: t("monitorAlert.colRule"), dataIndex: "ruleName" },
                {
                  title: t("monitorAlert.colEventType"),
                  dataIndex: "eventType",
                  render: (value: AlertRecord["eventType"]) => renderEventTypeTag(value)
                },
                {
                  title: t("monitorAlert.colLevel"),
                  dataIndex: "level",
                  render: (value: string) => renderLevelTag(value)
                },
                { title: t("monitorAlert.colCurrent"), dataIndex: "currentValue" },
                { title: t("monitorAlert.colTime"), dataIndex: "createdAt", render: (value: string) => formatDateTime(value) }
              ]}
            />
          </Card>
        </Col>
      </Row>

      <Drawer
        title={editingRule ? t("monitorAlert.editDrawerTitle") : t("monitorAlert.drawerTitle")}
        open={drawerOpen}
        onClose={closeDrawer}
        destroyOnHidden
        extra={
          canManageAlerts ? (
            <Button type="primary" loading={submitting} onClick={() => void handleSubmit()}>
              {editingRule ? t("monitorAlert.saveChanges") : t("common.save")}
            </Button>
          ) : null
        }
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label={t("monitorAlert.fieldRuleName")} rules={[{ required: true, message: t("monitorAlert.nameRequired") }]}>
            <Input disabled={!canManageAlerts} />
          </Form.Item>
          <Form.Item name="metric" label={t("monitorAlert.fieldMetric")} rules={[{ required: true }]}>
            <Select disabled={!canManageAlerts} options={metricOptions.map((value) => ({ label: value, value }))} />
          </Form.Item>
          <Form.Item name="op" label={t("monitorAlert.fieldOperator")} rules={[{ required: true }]}>
            <Select disabled={!canManageAlerts} options={operatorOptions.map((value) => ({ label: value, value }))} />
          </Form.Item>
          <Form.Item name="threshold" label={t("monitorAlert.fieldThreshold")} rules={[{ required: true, message: t("monitorAlert.thresholdRequired") }]}>
            <InputNumber min={0} precision={2} disabled={!canManageAlerts} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="level" label={t("monitorAlert.fieldLevel")} rules={[{ required: true }]}>
            <Select disabled={!canManageAlerts} options={levelOptions.map((value) => ({ label: value, value }))} />
          </Form.Item>
          <Form.Item name="enabled" label={t("monitorAlert.fieldEnabled")} valuePropName="checked">
            <Switch disabled={!canManageAlerts} />
          </Form.Item>
        </Form>
      </Drawer>
    </div>
  );
}

export default MonitorAlertPage;
