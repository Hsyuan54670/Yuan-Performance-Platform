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
  Table,
  Tag,
  Typography,
  message
} from "antd";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAuth } from "../../../hooks/useAuth";
import { createPlanApi, deletePlanApi, listPlansApi, updatePlanApi } from "../../../api/test";
import type { TestPlan, TestPlanPayload } from "../../../types/test";
import { formatDateTime } from "../../../utils/format";
import { PermissionCodes } from "../../../utils/permissions";
import { getRequestErrorMessage } from "../../../utils/request";

const rampOptions: TestPlanPayload["rampType"][] = ["STAIR", "LINEAR"];

function TestPlanPage() {
  const [plans, setPlans] = useState<TestPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingPlan, setEditingPlan] = useState<TestPlan | null>(null);
  const [form] = Form.useForm<TestPlanPayload>();
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const canWritePlans = hasPermission(PermissionCodes.TEST_PLAN_WRITE);

  const loadPlans = async () => {
    setLoading(true);
    try {
      setPlans(await listPlansApi());
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.loadFailed")));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadPlans();
  }, []);

  const openCreateDrawer = () => {
    setEditingPlan(null);
    form.setFieldsValue({
      name: "",
      targetUrl: "",
      concurrency: 100,
      duration: 300,
      rampType: "STAIR"
    });
    setDrawerOpen(true);
  };

  const openEditDrawer = (plan: TestPlan) => {
    setEditingPlan(plan);
    form.setFieldsValue({
      name: plan.name,
      targetUrl: plan.targetUrl,
      concurrency: plan.concurrency,
      duration: plan.duration,
      rampType: plan.rampType
    });
    setDrawerOpen(true);
  };

  const closeDrawer = () => {
    setDrawerOpen(false);
    setEditingPlan(null);
    form.resetFields();
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      if (editingPlan) {
        await updatePlanApi(editingPlan.id, values);
        message.success(t("testPlan.updateSuccess"));
      } else {
        await createPlanApi(values);
        message.success(t("testPlan.createSuccess"));
      }
      closeDrawer();
      await loadPlans();
    } catch (error) {
      if (typeof error === "object" && error && "errorFields" in error) {
        return;
      }
      message.error(getRequestErrorMessage(error, t("common.saveFailed")));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (planId: number) => {
    try {
      await deletePlanApi(planId);
      message.success(t("testPlan.deleteSuccess"));
      await loadPlans();
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.deleteFailed")));
    }
  };

  const columns = useMemo(
    () => [
      { title: t("testPlan.tablePlan"), dataIndex: "name" },
      { title: t("testPlan.tableTargetUrl"), dataIndex: "targetUrl", ellipsis: true },
      { title: t("testPlan.tableConcurrency"), dataIndex: "concurrency", width: 100 },
      { title: t("testPlan.tableDuration"), dataIndex: "duration", width: 120 },
      {
        title: t("testPlan.tableRamp"),
        dataIndex: "rampType",
        width: 120,
        render: (value: string) => <Tag color={value === "STAIR" ? "geekblue" : "cyan"}>{value}</Tag>
      },
      { title: t("testPlan.tableTasks"), dataIndex: "taskCount", width: 120 },
      {
        title: t("testPlan.tableCreated"),
        dataIndex: "createdAt",
        width: 180,
        render: (value: string) => formatDateTime(value)
      },
      ...(canWritePlans
        ? [
            {
              title: t("testPlan.colAction"),
              key: "action",
              width: 180,
              render: (_value: unknown, record: TestPlan) => (
                <Space size="small">
                  <Button type="link" icon={<EditOutlined />} onClick={() => openEditDrawer(record)}>
                    {t("testPlan.editPlan")}
                  </Button>
                  <Popconfirm
                    title={t("testPlan.deleteConfirm")}
                    okText={t("testPlan.confirmDelete")}
                    cancelText={t("testPlan.cancel")}
                    onConfirm={() => void handleDelete(record.id)}
                  >
                    <Button type="link" danger icon={<DeleteOutlined />}>
                      {t("testPlan.deletePlan")}
                    </Button>
                  </Popconfirm>
                </Space>
              )
            }
          ]
        : [])
    ],
    [canWritePlans, t]
  );

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space style={{ width: "100%", justifyContent: "space-between" }} wrap>
              <Space direction="vertical" size={4}>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("testPlan.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("testPlan.subtitle")}</Typography.Text>
              </Space>
              {canWritePlans ? (
                <Button type="primary" icon={<PlusOutlined />} onClick={openCreateDrawer}>
                  {t("testPlan.newPlan")}
                </Button>
              ) : null}
            </Space>
          </Card>
        </Col>

        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Table
              rowKey="id"
              loading={loading}
              dataSource={plans}
              locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t("common.noData")} /> }}
              pagination={{ pageSize: 8 }}
              columns={columns}
            />
          </Card>
        </Col>
      </Row>

      <Drawer
        title={editingPlan ? t("testPlan.editDrawerTitle") : t("testPlan.modalTitle")}
        open={drawerOpen}
        onClose={closeDrawer}
        destroyOnHidden
        extra={
          <Button type="primary" loading={submitting} onClick={() => void handleSubmit()}>
            {editingPlan ? t("testPlan.saveChanges") : t("common.save")}
          </Button>
        }
      >
        <Form layout="vertical" form={form}>
          <Form.Item name="name" label={t("testPlan.fieldName")} rules={[{ required: true, message: t("testPlan.nameRequired") }]}>
            <Input />
          </Form.Item>
          <Form.Item name="targetUrl" label={t("testPlan.fieldTargetUrl")} rules={[{ required: true, message: t("testPlan.targetUrlRequired") }]}>
            <Input placeholder="https://api.demo.local/path" />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="concurrency" label={t("testPlan.fieldConcurrency")} rules={[{ required: true, message: t("testPlan.concurrencyRequired") }]}>
                <InputNumber min={1} max={20000} style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="duration" label={t("testPlan.fieldDuration")} rules={[{ required: true, message: t("testPlan.durationRequired") }]}>
                <InputNumber min={1} max={7200} style={{ width: "100%" }} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="rampType" label={t("testPlan.fieldRampMode")} rules={[{ required: true }]}>
            <Select options={rampOptions.map((value) => ({ label: value, value }))} />
          </Form.Item>
        </Form>
      </Drawer>
    </div>
  );
}

export default TestPlanPage;
