import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  DeleteOutlined,
  EditOutlined,
  MinusCircleOutlined,
  PlusOutlined
} from "@ant-design/icons";
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
import { createSceneApi, deleteSceneApi, listScenesApi, updateSceneApi } from "../../../api/test";
import { useAuth } from "../../../hooks/useAuth";
import type { SceneStepPayload, TestScene, TestScenePayload } from "../../../types/test";
import { formatDateTime } from "../../../utils/format";
import { PermissionCodes } from "../../../utils/permissions";
import { getRequestErrorMessage } from "../../../utils/request";

const methodOptions: SceneStepPayload["method"][] = ["GET", "POST", "PUT", "DELETE"];

const buildEmptyStep = (): SceneStepPayload => ({
  name: "",
  method: "GET",
  path: "/",
  weight: 100
});

function TestScenePage() {
  const [scenes, setScenes] = useState<TestScene[]>([]);
  const [activeSceneId, setActiveSceneId] = useState<number>();
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingScene, setEditingScene] = useState<TestScene | null>(null);
  const [form] = Form.useForm<TestScenePayload>();
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const canWriteScenes = hasPermission(PermissionCodes.TEST_SCENE_WRITE);

  const loadScenes = async (preferredSceneId?: number) => {
    setLoading(true);
    try {
      const rows = await listScenesApi();
      setScenes(rows);
      const nextSceneId = preferredSceneId && rows.some((scene) => scene.id === preferredSceneId)
        ? preferredSceneId
        : rows[0]?.id;
      setActiveSceneId(nextSceneId);
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.loadFailed")));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadScenes();
  }, []);

  const activeScene = useMemo(
    () => scenes.find((scene) => scene.id === activeSceneId) ?? null,
    [activeSceneId, scenes]
  );

  const openCreateDrawer = () => {
    setEditingScene(null);
    form.setFieldsValue({
      name: "",
      steps: [buildEmptyStep()]
    });
    setDrawerOpen(true);
  };

  const openEditDrawer = (scene: TestScene) => {
    setEditingScene(scene);
    form.setFieldsValue({
      name: scene.name,
      steps: (scene.steps ?? []).map((step) => ({
        name: step.name,
        method: step.method,
        path: step.path,
        weight: step.weight
      }))
    });
    setDrawerOpen(true);
  };

  const closeDrawer = () => {
    setDrawerOpen(false);
    setEditingScene(null);
    form.resetFields();
  };

  const moveStep = (index: number, direction: "up" | "down") => {
    const steps = [...(form.getFieldValue("steps") || [])] as SceneStepPayload[];
    const target = direction === "up" ? index - 1 : index + 1;
    if (target < 0 || target >= steps.length) {
      return;
    }
    [steps[index], steps[target]] = [steps[target], steps[index]];
    form.setFieldsValue({ steps });
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (!values.steps?.length) {
        message.error(t("testScene.stepsRequired"));
        return;
      }
      setSubmitting(true);
      if (editingScene) {
        await updateSceneApi(editingScene.id, values);
        message.success(t("testScene.updateSuccess"));
        closeDrawer();
        await loadScenes(editingScene.id);
      } else {
        const createdId = await createSceneApi(values);
        message.success(t("testScene.createSuccess"));
        closeDrawer();
        await loadScenes(createdId);
      }
    } catch (error) {
      if (typeof error === "object" && error && "errorFields" in error) {
        return;
      }
      message.error(getRequestErrorMessage(error, t("common.saveFailed")));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (sceneId: number) => {
    try {
      await deleteSceneApi(sceneId);
      message.success(t("testScene.deleteSuccess"));
      await loadScenes(activeSceneId === sceneId ? undefined : activeSceneId);
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.deleteFailed")));
    }
  };

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space style={{ width: "100%", justifyContent: "space-between" }} wrap>
              <Space direction="vertical" size={4}>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("testScene.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("testScene.subtitle")}</Typography.Text>
              </Space>
              {canWriteScenes ? (
                <Button type="primary" icon={<PlusOutlined />} onClick={openCreateDrawer}>
                  {t("testScene.newScene")}
                </Button>
              ) : null}
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={9}>
          <Card className="glass-card" title={t("testScene.sceneList")} style={{ borderRadius: 18 }}>
            <Space direction="vertical" style={{ width: "100%" }} size="middle">
              {loading ? (
                <Typography.Text type="secondary">{t("common.loading")}</Typography.Text>
              ) : scenes.length === 0 ? (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t("common.noData")} />
              ) : (
                scenes.map((scene) => (
                  <Card
                    key={scene.id}
                    size="small"
                    hoverable
                    onClick={() => setActiveSceneId(scene.id)}
                    style={{
                      cursor: "pointer",
                      borderColor: scene.id === activeSceneId ? "#0b7285" : undefined
                    }}
                  >
                    <Space style={{ width: "100%", justifyContent: "space-between" }} align="start">
                      <Space direction="vertical" size={6} style={{ width: "100%" }}>
                        <Typography.Text strong>{scene.name}</Typography.Text>
                        <Space wrap>
                          <Tag color="blue">{t("testScene.stepsCount", { count: scene.steps?.length ?? 0 })}</Tag>
                          <Tag color="geekblue">{t("testScene.taskCount", { count: scene.taskCount })}</Tag>
                        </Space>
                      </Space>
                      {canWriteScenes ? (
                        <Space>
                          <Button
                            type="text"
                            icon={<EditOutlined />}
                            onClick={(event) => {
                              event.stopPropagation();
                              openEditDrawer(scene);
                            }}
                          />
                          <Popconfirm
                            title={t("testScene.deleteConfirm")}
                            okText={t("testScene.confirmDelete")}
                            cancelText={t("testScene.cancel")}
                            onConfirm={() => void handleDelete(scene.id)}
                          >
                            <Button
                              type="text"
                              danger
                              icon={<DeleteOutlined />}
                              onClick={(event) => event.stopPropagation()}
                            />
                          </Popconfirm>
                        </Space>
                      ) : null}
                    </Space>
                  </Card>
                ))
              )}
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={15}>
          <Card
            className="glass-card"
            title={activeScene ? activeScene.name : t("testScene.detailTitle")}
            extra={
              activeScene && canWriteScenes ? (
                <Button type="link" icon={<EditOutlined />} onClick={() => openEditDrawer(activeScene)}>
                  {t("testScene.editScene")}
                </Button>
              ) : null
            }
            style={{ borderRadius: 18 }}
          >
            {activeScene ? (
              <Space direction="vertical" style={{ width: "100%" }} size="large">
                <Space direction="vertical" size={8}>
                  <Typography.Text type="secondary">
                    {t("testScene.detailHint", { createdAt: formatDateTime(activeScene.createdAt) })}
                  </Typography.Text>
                  <Space wrap>
                    <Tag color="blue">{t("testScene.stepsCount", { count: activeScene.steps?.length ?? 0 })}</Tag>
                    <Tag color="geekblue">{t("testScene.taskCount", { count: activeScene.taskCount })}</Tag>
                  </Space>
                </Space>
                <Table
                  rowKey="id"
                  pagination={false}
                  dataSource={activeScene.steps ?? []}
                  locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t("common.noData")} /> }}
                  columns={[
                    { title: t("testScene.colOrder"), render: (_value, _row, index) => index + 1, width: 70 },
                    { title: t("testScene.colStep"), dataIndex: "name" },
                    { title: t("testScene.colMethod"), dataIndex: "method", width: 100, render: (value: string) => <Tag>{value}</Tag> },
                    { title: t("testScene.colPath"), dataIndex: "path" },
                    { title: t("testScene.colWeight"), dataIndex: "weight", width: 100 }
                  ]}
                />
              </Space>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t("testScene.emptyDetail")} />
            )}
          </Card>
        </Col>
      </Row>

      <Drawer
        title={editingScene ? t("testScene.editDrawerTitle") : t("testScene.drawerTitle")}
        open={drawerOpen}
        onClose={closeDrawer}
        width={760}
        destroyOnHidden
        extra={
          canWriteScenes ? (
            <Button type="primary" loading={submitting} onClick={() => void handleSubmit()}>
              {editingScene ? t("testScene.saveChanges") : t("common.save")}
            </Button>
          ) : null
        }
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label={t("testScene.fieldName")} rules={[{ required: true, message: t("testScene.nameRequired") }]}>
            <Input disabled={!canWriteScenes} />
          </Form.Item>

          <Form.List name="steps">
            {(fields, { add, remove }) => (
              <Space direction="vertical" style={{ width: "100%" }} size="middle">
                <Space style={{ width: "100%", justifyContent: "space-between" }} wrap>
                  <Typography.Title level={5} style={{ margin: 0 }}>{t("testScene.stepOrchestration")}</Typography.Title>
                  {canWriteScenes ? (
                    <Button type="dashed" icon={<PlusOutlined />} onClick={() => add(buildEmptyStep())}>
                      {t("testScene.addStep")}
                    </Button>
                  ) : null}
                </Space>

                {fields.map((field, index) => (
                  <Card
                    key={field.key}
                    size="small"
                    title={`${t("testScene.colOrder")} ${index + 1}`}
                    extra={
                      canWriteScenes ? (
                        <Space>
                          <Button size="small" icon={<ArrowUpOutlined />} onClick={() => moveStep(index, "up")} />
                          <Button size="small" icon={<ArrowDownOutlined />} onClick={() => moveStep(index, "down")} />
                          <Button size="small" danger icon={<MinusCircleOutlined />} onClick={() => remove(field.name)} />
                        </Space>
                      ) : null
                    }
                  >
                    <Row gutter={12}>
                      <Col xs={24} md={12}>
                        <Form.Item
                          name={[field.name, "name"]}
                          label={t("testScene.fieldStepName")}
                          rules={[{ required: true, message: t("testScene.stepNameRequired") }]}
                        >
                          <Input disabled={!canWriteScenes} />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={12}>
                        <Form.Item
                          name={[field.name, "method"]}
                          label={t("testScene.fieldMethod")}
                          rules={[{ required: true }]}
                        >
                          <Select disabled={!canWriteScenes} options={methodOptions.map((value) => ({ label: value, value }))} />
                        </Form.Item>
                      </Col>
                    </Row>
                    <Row gutter={12}>
                      <Col xs={24} md={16}>
                        <Form.Item
                          name={[field.name, "path"]}
                          label={t("testScene.fieldPath")}
                          rules={[{ required: true, message: t("testScene.pathRequired") }]}
                        >
                          <Input disabled={!canWriteScenes} placeholder="/api/orders" />
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={8}>
                        <Form.Item
                          name={[field.name, "weight"]}
                          label={t("testScene.fieldWeight")}
                          rules={[{ required: true, message: t("testScene.weightRequired") }]}
                        >
                          <InputNumber min={1} disabled={!canWriteScenes} style={{ width: "100%" }} />
                        </Form.Item>
                      </Col>
                    </Row>
                  </Card>
                ))}

                {fields.length === 0 ? (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t("testScene.stepsRequired")} />
                ) : null}
              </Space>
            )}
          </Form.List>
        </Form>
      </Drawer>
    </div>
  );
}

export default TestScenePage;
