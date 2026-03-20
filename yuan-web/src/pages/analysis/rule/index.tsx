import { DeleteOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons";
import {
  Button,
  Card,
  Col,
  Drawer,
  Empty,
  Form,
  Input,
  Popconfirm,
  Row,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
  message
} from "antd";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAuth } from "../../../hooks/useAuth";
import { createRuleApi, deleteRuleApi, listRulesApi, switchRuleApi, updateRuleApi } from "../../../api/analysis";
import type { AnalysisRule, AnalysisRulePayload, AnalysisRulePriority, AnalysisRuleSeverity, AnalysisRuleType } from "../../../types/analysis";
import { PermissionCodes } from "../../../utils/permissions";
import { getRequestErrorMessage } from "../../../utils/request";

const severityOptions: AnalysisRuleSeverity[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];
const priorityOptions: AnalysisRulePriority[] = ["P0", "P1", "P2"];
const engineTypeOptions = ["HIGH_LATENCY", "ERROR_RATE", "THROUGHPUT", "STABILITY"];
const aiTypeOptions = ["ROOT_CAUSE", "OPTIMIZATION", "CAPACITY", "STABILITY"];

function AnalysisRulePage() {
  const [rules, setRules] = useState<AnalysisRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<AnalysisRule | null>(null);
  const [form] = Form.useForm<AnalysisRulePayload>();
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const currentRuleType = Form.useWatch("ruleType", form) as AnalysisRuleType | undefined;
  const canManageRules = hasPermission(PermissionCodes.ANALYSIS_RULE_WRITE);

  const engineRules = useMemo(() => rules.filter((rule) => rule.ruleType === "ENGINE"), [rules]);
  const aiRules = useMemo(() => rules.filter((rule) => rule.ruleType === "AI"), [rules]);

  const loadRules = async () => {
    setLoading(true);
    try {
      setRules(await listRulesApi());
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.loadFailed")));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadRules();
  }, []);

  const openCreateDrawer = (ruleType: AnalysisRuleType) => {
    setEditingRule(null);
    form.setFieldsValue({
      ruleType,
      name: "",
      expression: "",
      instruction: "",
      bottleneckType: ruleType === "ENGINE" ? "HIGH_LATENCY" : "ROOT_CAUSE",
      severity: ruleType === "ENGINE" ? "HIGH" : "MEDIUM",
      priority: "P1",
      enabled: true
    });
    setDrawerOpen(true);
  };

  const openEditDrawer = (rule: AnalysisRule) => {
    setEditingRule(rule);
    form.setFieldsValue({
      ruleType: rule.ruleType,
      name: rule.name,
      expression: rule.expression,
      instruction: rule.instruction,
      bottleneckType: rule.bottleneckType,
      severity: rule.severity,
      priority: rule.priority,
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
        await updateRuleApi(editingRule.id, values);
        message.success(t("analysisRule.updateSuccess"));
      } else {
        await createRuleApi(values);
        message.success(t("analysisRule.createSuccess"));
      }
      closeDrawer();
      await loadRules();
    } catch (error) {
      if (error instanceof Error && error.message) {
        message.error(getRequestErrorMessage(error, t("common.saveFailed")));
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleSwitch = async (rule: AnalysisRule, enabled: boolean) => {
    try {
      await switchRuleApi(rule.id, enabled);
      setRules((current) => current.map((item) => (item.id === rule.id ? { ...item, enabled } : item)));
      message.success(t("analysisRule.switchSuccess"));
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.saveFailed")));
    }
  };

  const handleDelete = async (ruleId: number) => {
    try {
      await deleteRuleApi(ruleId);
      setRules((current) => current.filter((item) => item.id !== ruleId));
      message.success(t("analysisRule.deleteSuccess"));
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.deleteFailed")));
    }
  };

  const renderSeverityTag = (value: string) => (
    <Tag color={value === "CRITICAL" ? "red" : value === "HIGH" ? "orange" : value === "MEDIUM" ? "blue" : "green"}>{value}</Tag>
  );

  const renderPriorityTag = (value: string) => (
    <Tag color={value === "P0" ? "red" : value === "P1" ? "orange" : "blue"}>{value}</Tag>
  );

  const renderActionColumn = (_: unknown, record: AnalysisRule) => (
    <Space size="small">
      <Tooltip title={t("analysisRule.editRule")}>
        <Button type="link" icon={<EditOutlined />} onClick={() => openEditDrawer(record)} aria-label={t("analysisRule.editRule")} />
      </Tooltip>
      <Popconfirm
        title={t("analysisRule.deleteConfirm")}
        okText={t("analysisRule.confirmDelete")}
        cancelText={t("analysisRule.cancel")}
        onConfirm={() => void handleDelete(record.id)}
      >
        <Tooltip title={t("analysisRule.deleteRule")}>
          <Button type="link" danger icon={<DeleteOutlined />} aria-label={t("analysisRule.deleteRule")} />
        </Tooltip>
      </Popconfirm>
    </Space>
  );

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space style={{ width: "100%", justifyContent: "space-between" }} wrap>
              <div>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("analysisRule.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("analysisRule.subtitle")}</Typography.Text>
              </div>
              {canManageRules ? (
                <Space wrap>
                  <Button icon={<PlusOutlined />} onClick={() => openCreateDrawer("ENGINE")}>
                    {t("analysisRule.addEngineRule")}
                  </Button>
                  <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreateDrawer("AI")}>
                    {t("analysisRule.addAiRule")}
                  </Button>
                </Space>
              ) : null}
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            className="glass-card"
            title={t("analysisRule.engineRulesTitle")}
            extra={<Tag color="blue">{t("analysisRule.engineRulesCount", { count: engineRules.length })}</Tag>}
            style={{ borderRadius: 18 }}
          >
            <Table
              rowKey="id"
              loading={loading}
              dataSource={engineRules}
              locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t("common.noData")} /> }}
              pagination={{ pageSize: 6 }}
              columns={[
                { title: t("analysisRule.colName"), dataIndex: "name" },
                { title: t("analysisRule.colExpression"), dataIndex: "expression", ellipsis: true },
                { title: t("analysisRule.colType"), dataIndex: "bottleneckType", render: (value: string) => <Tag>{value}</Tag> },
                { title: t("analysisRule.colSeverity"), dataIndex: "severity", render: (value: string) => renderSeverityTag(value) },
                { title: t("analysisRule.colPriority"), dataIndex: "priority", render: (value: string) => renderPriorityTag(value) },
                {
                  title: t("analysisRule.colEnabled"),
                  dataIndex: "enabled",
                  render: (value: boolean, record: AnalysisRule) => <Switch checked={value} disabled={!canManageRules} onChange={(checked) => void handleSwitch(record, checked)} />
                },
                ...(canManageRules ? [{ title: t("analysisRule.colAction"), key: "action", render: renderActionColumn }] : [])
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            className="glass-card"
            title={t("analysisRule.aiRulesTitle")}
            extra={<Tag color="purple">{t("analysisRule.aiRulesCount", { count: aiRules.length })}</Tag>}
            style={{ borderRadius: 18 }}
          >
            <Table
              rowKey="id"
              loading={loading}
              dataSource={aiRules}
              locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t("common.noData")} /> }}
              pagination={{ pageSize: 6 }}
              columns={[
                { title: t("analysisRule.colName"), dataIndex: "name" },
                { title: t("analysisRule.colInstruction"), dataIndex: "instruction", ellipsis: true },
                { title: t("analysisRule.colType"), dataIndex: "bottleneckType", render: (value: string) => <Tag>{value}</Tag> },
                { title: t("analysisRule.colSeverity"), dataIndex: "severity", render: (value: string) => renderSeverityTag(value) },
                { title: t("analysisRule.colPriority"), dataIndex: "priority", render: (value: string) => renderPriorityTag(value) },
                {
                  title: t("analysisRule.colEnabled"),
                  dataIndex: "enabled",
                  render: (value: boolean, record: AnalysisRule) => <Switch checked={value} disabled={!canManageRules} onChange={(checked) => void handleSwitch(record, checked)} />
                },
                ...(canManageRules ? [{ title: t("analysisRule.colAction"), key: "action", render: renderActionColumn }] : [])
              ]}
            />
          </Card>
        </Col>
      </Row>

      <Drawer
        title={editingRule ? t("analysisRule.editDrawerTitle") : currentRuleType === "AI" ? t("analysisRule.createAiRule") : t("analysisRule.createEngineRule")}
        open={drawerOpen}
        onClose={closeDrawer}
        destroyOnHidden
        extra={
          canManageRules ? (
            <Button type="primary" loading={submitting} onClick={() => void handleSubmit()}>
              {editingRule ? t("analysisRule.saveChanges") : t("common.save")}
            </Button>
          ) : null
        }
      >
        <Form layout="vertical" form={form} initialValues={{ ruleType: "ENGINE", enabled: true, severity: "HIGH", priority: "P1" }}>
          <Form.Item name="ruleType" label={t("analysisRule.fieldRuleType")} rules={[{ required: true }]}>
            <Select disabled={!canManageRules} options={[{ label: t("analysisRule.ruleTypeEngine"), value: "ENGINE" }, { label: t("analysisRule.ruleTypeAi"), value: "AI" }]} />
          </Form.Item>
          <Form.Item name="name" label={t("analysisRule.fieldRuleName")} rules={[{ required: true, message: t("analysisRule.nameRequired") }]}>
            <Input disabled={!canManageRules} />
          </Form.Item>
          {currentRuleType === "AI" ? (
            <Form.Item name="instruction" label={t("analysisRule.fieldInstruction")} rules={[{ required: true, message: t("analysisRule.instructionRequired") }]}>
              <Input.TextArea rows={5} disabled={!canManageRules} placeholder={t("analysisRule.instructionPlaceholder")} />
            </Form.Item>
          ) : (
            <Form.Item name="expression" label={t("analysisRule.fieldExpression")} rules={[{ required: true, message: t("analysisRule.expressionRequired") }]}>
              <Input.TextArea rows={4} disabled={!canManageRules} placeholder="summary.p99 > 2000 && feature.highLatencySeconds >= 3" />
            </Form.Item>
          )}
          <Form.Item name="bottleneckType" label={t("analysisRule.fieldBottleneckType")} rules={[{ required: true }]}>
            <Select disabled={!canManageRules} options={(currentRuleType === "AI" ? aiTypeOptions : engineTypeOptions).map((value) => ({ label: value, value }))} />
          </Form.Item>
          <Form.Item name="severity" label={t("analysisRule.fieldSeverity")} rules={[{ required: true }]}>
            <Select disabled={!canManageRules} options={severityOptions.map((value) => ({ label: value, value }))} />
          </Form.Item>
          <Form.Item name="priority" label={t("analysisRule.fieldPriority")} rules={[{ required: true }]}>
            <Select disabled={!canManageRules} options={priorityOptions.map((value) => ({ label: value, value }))} />
          </Form.Item>
          <Form.Item name="enabled" label={t("analysisRule.fieldEnabled")} valuePropName="checked">
            <Switch disabled={!canManageRules} />
          </Form.Item>
        </Form>
      </Drawer>
    </div>
  );
}

export default AnalysisRulePage;




