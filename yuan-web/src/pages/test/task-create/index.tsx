import { ArrowRightOutlined, PlusOutlined } from "@ant-design/icons";
import {
  Button,
  Card,
  Col,
  Empty,
  Form,
  Row,
  Select,
  Space,
  Statistic,
  Tag,
  Typography,
  message
} from "antd";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../hooks/useAuth";
import { createTaskApi, listPlansApi, listScenesApi } from "../../../api/test";
import { useAppStore } from "../../../store/appStore";
import type { TestPlan, TestScene, TestTaskCreatePayload } from "../../../types/test";
import { formatDateTime } from "../../../utils/format";
import { PermissionCodes } from "../../../utils/permissions";
import { getRequestErrorMessage } from "../../../utils/request";

function TestTaskCreatePage() {
  const navigate = useNavigate();
  const { setActiveTaskId, setActiveRunId } = useAppStore();
  const [plans, setPlans] = useState<TestPlan[]>([]);
  const [scenes, setScenes] = useState<TestScene[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<TestTaskCreatePayload>();
  const selectedPlanId = Form.useWatch("planId", form);
  const selectedSceneId = Form.useWatch("sceneId", form);
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const canCreateTask = hasPermission(PermissionCodes.TEST_TASK_CREATE);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        const [planRows, sceneRows] = await Promise.all([listPlansApi(), listScenesApi()]);
        setPlans(planRows);
        setScenes(sceneRows);
        form.setFieldsValue({
          planId: planRows[0]?.id,
          sceneId: sceneRows[0]?.id
        });
      } catch (error) {
        message.error(getRequestErrorMessage(error, t("common.loadFailed")));
      } finally {
        setLoading(false);
      }
    };

    void loadData();
  }, [form, t]);

  const selectedPlan = useMemo(
    () => plans.find((item) => item.id === selectedPlanId) ?? null,
    [plans, selectedPlanId]
  );

  const selectedScene = useMemo(
    () => scenes.find((item) => item.id === selectedSceneId) ?? null,
    [scenes, selectedSceneId]
  );

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const taskId = await createTaskApi(values);
      setActiveTaskId(taskId);
      setActiveRunId(0);
      message.success(t("testTaskCreate.createSuccess"));
      navigate("/test/task");
    } catch (error) {
      if (typeof error === "object" && error && "errorFields" in error) {
        return;
      }
      message.error(getRequestErrorMessage(error, t("common.saveFailed")));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space style={{ width: "100%", justifyContent: "space-between" }} wrap>
              <Space direction="vertical" size={4}>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("testTaskCreate.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("testTaskCreate.subtitle")}</Typography.Text>
              </Space>
              <Button onClick={() => navigate("/test/task")}>{t("testTaskCreate.backToConsole")}</Button>
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={11}>
          <Card className="glass-card" title={t("testTaskCreate.formTitle")} style={{ borderRadius: 18 }} loading={loading}>
            {plans.length === 0 || scenes.length === 0 ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t("testTaskCreate.emptySource")} />
            ) : (
              <Form form={form} layout="vertical">
                <Form.Item
                  name="planId"
                  label={t("testTaskCreate.selectPlan")}
                  rules={[{ required: true, message: t("testTaskCreate.planRequired") }]}
                >
                  <Select
                    disabled={!canCreateTask}
                    options={plans.map((plan) => ({
                      label: `${plan.name} · ${plan.concurrency}VU · ${plan.duration}s`,
                      value: plan.id
                    }))}
                  />
                </Form.Item>
                <Form.Item
                  name="sceneId"
                  label={t("testTaskCreate.selectScene")}
                  rules={[{ required: true, message: t("testTaskCreate.sceneRequired") }]}
                >
                  <Select
                    disabled={!canCreateTask}
                    options={scenes.map((scene) => ({
                      label: `${scene.name} · ${scene.steps.length} steps`,
                      value: scene.id
                    }))}
                  />
                </Form.Item>
                <Space style={{ width: "100%", justifyContent: "space-between" }} wrap>
                  <Typography.Text type="secondary">{t("testTaskCreate.submitHint")}</Typography.Text>
                  {canCreateTask ? (
                    <Button type="primary" icon={<PlusOutlined />} loading={submitting} onClick={() => void handleSubmit()}>
                      {t("testTaskCreate.createTask")}
                    </Button>
                  ) : null}
                </Space>
              </Form>
            )}
          </Card>
        </Col>

        <Col xs={24} lg={13}>
          <Card className="glass-card" title={t("testTaskCreate.previewTitle")} style={{ borderRadius: 18 }}>
            {selectedPlan && selectedScene ? (
              <Space direction="vertical" size="large" style={{ width: "100%" }}>
                <Row gutter={[16, 16]}>
                  <Col xs={24} md={12}>
                    <Card size="small" variant="borderless" style={{ background: "rgba(255,255,255,0.45)" }}>
                      <Space direction="vertical" size={8} style={{ width: "100%" }}>
                        <Typography.Text type="secondary">{t("testTaskCreate.selectedPlan")}</Typography.Text>
                        <Typography.Title level={4} style={{ margin: 0 }}>{selectedPlan.name}</Typography.Title>
                        <Space wrap>
                          <Tag color="geekblue">{selectedPlan.concurrency} VU</Tag>
                          <Tag color="cyan">{selectedPlan.duration}s</Tag>
                          <Tag>{selectedPlan.rampType}</Tag>
                        </Space>
                        <Typography.Text type="secondary">{selectedPlan.targetUrl}</Typography.Text>
                        <Typography.Text type="secondary">
                          {t("testTaskCreate.createdAt", { value: formatDateTime(selectedPlan.createdAt) })}
                        </Typography.Text>
                      </Space>
                    </Card>
                  </Col>
                  <Col xs={24} md={12}>
                    <Card size="small" variant="borderless" style={{ background: "rgba(255,255,255,0.45)" }}>
                      <Space direction="vertical" size={8} style={{ width: "100%" }}>
                        <Typography.Text type="secondary">{t("testTaskCreate.selectedScene")}</Typography.Text>
                        <Typography.Title level={4} style={{ margin: 0 }}>{selectedScene.name}</Typography.Title>
                        <Space wrap>
                          <Tag color="blue">{t("testScene.stepsCount", { count: selectedScene.steps.length })}</Tag>
                          <Tag color="geekblue">{t("testScene.taskCount", { count: selectedScene.taskCount })}</Tag>
                        </Space>
                        <Typography.Text type="secondary">
                          {t("testTaskCreate.createdAt", { value: formatDateTime(selectedScene.createdAt) })}
                        </Typography.Text>
                      </Space>
                    </Card>
                  </Col>
                </Row>

                <Card size="small" variant="borderless" style={{ background: "rgba(255,255,255,0.45)" }}>
                  <Space direction="vertical" size={12} style={{ width: "100%" }}>
                    <Typography.Title level={5} style={{ margin: 0 }}>{t("testTaskCreate.compositionTitle")}</Typography.Title>
                    <Space align="center" wrap>
                      <Tag color="geekblue">{selectedPlan.name}</Tag>
                      <ArrowRightOutlined />
                      <Tag color="blue">{selectedScene.name}</Tag>
                    </Space>
                    <Row gutter={[16, 16]}>
                      <Col xs={12} md={6}><Statistic title="VU" value={selectedPlan.concurrency} /></Col>
                      <Col xs={12} md={6}><Statistic title="s" value={selectedPlan.duration} /></Col>
                      <Col xs={12} md={6}><Statistic title={t("testTaskCreate.steps")} value={selectedScene.steps.length} /></Col>
                      <Col xs={12} md={6}><Statistic title={t("testTaskCreate.tasks")} value={selectedScene.taskCount} /></Col>
                    </Row>
                  </Space>
                </Card>
              </Space>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t("testTaskCreate.previewHint")} />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default TestTaskCreatePage;
