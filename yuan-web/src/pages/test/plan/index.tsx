import { PlusOutlined } from "@ant-design/icons";
import { Button, Card, Col, Form, Input, InputNumber, Modal, Row, Select, Space, Table, Tag, Typography, message } from "antd";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { listPlansApi } from "../../../api/test";
import type { TestPlan } from "../../../types/test";
import { formatDateTime } from "../../../utils/format";

function TestPlanPage() {
  const [plans, setPlans] = useState<TestPlan[]>([]);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm<TestPlan>();
  const { t } = useTranslation();

  useEffect(() => {
    listPlansApi().then(setPlans);
  }, []);

  const onCreate = async () => {
    const values = await form.validateFields();
    setPlans((prev) => [
      {
        ...values,
        id: Date.now(),
        createdAt: new Date().toISOString()
      }
    ]);
    setOpen(false);
    form.resetFields();
    message.success(t("testPlan.createSuccess"));
  };

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space style={{ width: "100%", justifyContent: "space-between" }}>
              <div>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("testPlan.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("testPlan.subtitle")}</Typography.Text>
              </div>
              <Button type="primary" icon={<PlusOutlined />} onClick={() => setOpen(true)}>
                {t("testPlan.newPlan")}
              </Button>
            </Space>
          </Card>
        </Col>

        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Table
              rowKey="id"
              dataSource={plans}
              pagination={{ pageSize: 8 }}
              columns={[
                { title: t("testPlan.tablePlan"), dataIndex: "name" },
                { title: t("testPlan.tableTargetUrl"), dataIndex: "targetUrl", ellipsis: true },
                { title: t("testPlan.tableConcurrency"), dataIndex: "concurrency" },
                { title: t("testPlan.tableDuration"), dataIndex: "duration" },
                {
                  title: t("testPlan.tableRamp"),
                  dataIndex: "rampType",
                  render: (value: string) => <Tag color={value === "STAIR" ? "geekblue" : "cyan"}>{value}</Tag>
                },
                {
                  title: t("testPlan.tableCreated"),
                  dataIndex: "createdAt",
                  render: (value: string) => formatDateTime(value)
                }
              ]}
            />
          </Card>
        </Col>
      </Row>

      <Modal title={t("testPlan.modalTitle")} open={open} onOk={onCreate} onCancel={() => setOpen(false)} destroyOnClose>
        <Form layout="vertical" form={form} initialValues={{ concurrency: 100, duration: 300, rampType: "STAIR" }}>
          <Form.Item name="name" label={t("testPlan.fieldName")} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="targetUrl" label={t("testPlan.fieldTargetUrl")} rules={[{ required: true }]}>
            <Input placeholder="https://api.demo.local/path" />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="concurrency" label={t("testPlan.fieldConcurrency")} rules={[{ required: true }]}>
                <InputNumber min={1} max={20000} style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="duration" label={t("testPlan.fieldDuration")} rules={[{ required: true }]}>
                <InputNumber min={30} max={7200} style={{ width: "100%" }} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="rampType" label={t("testPlan.fieldRampMode")}>
            <Select options={[{ label: "STAIR", value: "STAIR" }, { label: "LINEAR", value: "LINEAR" }]} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default TestPlanPage;
