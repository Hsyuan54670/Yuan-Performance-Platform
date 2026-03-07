import { PlusOutlined } from "@ant-design/icons";
import { Button, Card, Col, Form, Input, Modal, Row, Select, Space, Switch, Table, Tag, Typography } from "antd";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { listRulesApi } from "../../../api/analysis";
import type { AnalysisRule } from "../../../types/analysis";

function AnalysisRulePage() {
  const [rules, setRules] = useState<AnalysisRule[]>([]);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm<AnalysisRule>();
  const { t } = useTranslation();

  useEffect(() => {
    listRulesApi().then(setRules);
  }, []);

  const onSave = async () => {
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
                <Typography.Title level={3} style={{ margin: 0 }}>{t("analysisRule.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("analysisRule.subtitle")}</Typography.Text>
              </div>
              <Button type="primary" icon={<PlusOutlined />} onClick={() => setOpen(true)}>
                {t("analysisRule.addRule")}
              </Button>
            </Space>
          </Card>
        </Col>

        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Table
              rowKey="id"
              dataSource={rules}
              columns={[
                { title: t("analysisRule.colName"), dataIndex: "name" },
                { title: t("analysisRule.colExpression"), dataIndex: "expression", ellipsis: true },
                { title: t("analysisRule.colType"), dataIndex: "bottleneckType", render: (v: string) => <Tag>{v}</Tag> },
                { title: t("analysisRule.colSeverity"), dataIndex: "severity", render: (v: string) => <Tag color={v === "HIGH" ? "orange" : "blue"}>{v}</Tag> },
                { title: t("analysisRule.colEnabled"), dataIndex: "enabled", render: (v: boolean) => <Switch checked={v} size="small" /> }
              ]}
            />
          </Card>
        </Col>
      </Row>

      <Modal title={t("analysisRule.modalTitle")} open={open} onOk={onSave} onCancel={() => setOpen(false)}>
        <Form layout="vertical" form={form} initialValues={{ enabled: true, severity: "HIGH" }}>
          <Form.Item name="name" label={t("analysisRule.fieldRuleName")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="expression" label={t("analysisRule.fieldExpression")} rules={[{ required: true }]}>
            <Input.TextArea rows={4} placeholder="avg_cpu_usage > 80 and p99_response_time > 2000" />
          </Form.Item>
          <Form.Item name="bottleneckType" label={t("analysisRule.fieldBottleneckType")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="severity" label={t("analysisRule.fieldSeverity")}>
            <Select options={["LOW", "MEDIUM", "HIGH", "CRITICAL"].map((v) => ({ label: v, value: v }))} />
          </Form.Item>
          <Form.Item name="enabled" label={t("analysisRule.fieldEnabled")} valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default AnalysisRulePage;
