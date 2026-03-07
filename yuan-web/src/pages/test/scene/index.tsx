import { ArrowDownOutlined, ArrowUpOutlined } from "@ant-design/icons";
import { Button, Card, Col, Row, Space, Table, Tag, Typography } from "antd";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { listScenesApi } from "../../../api/test";
import type { SceneStep, TestScene } from "../../../types/test";

const moveStep = (steps: SceneStep[], index: number, direction: "up" | "down") => {
  const target = direction === "up" ? index - 1 : index + 1;
  if (target < 0 || target >= steps.length) return steps;
  const cloned = [...steps];
  [cloned[index], cloned[target]] = [cloned[target], cloned[index]];
  return cloned;
};

function TestScenePage() {
  const [scenes, setScenes] = useState<TestScene[]>([]);
  const [activeSceneId, setActiveSceneId] = useState<number>();
  const { t } = useTranslation();

  useEffect(() => {
    listScenesApi().then((resp) => {
      setScenes(resp);
      setActiveSceneId(resp[0]?.id);
    });
  }, []);

  const activeScene = scenes.find((s) => s.id === activeSceneId);

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Typography.Title level={3} style={{ margin: 0 }}>
              {t("testScene.title")}
            </Typography.Title>
            <Typography.Text type="secondary">{t("testScene.subtitle")}</Typography.Text>
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Card className="glass-card" title={t("testScene.sceneList")} style={{ borderRadius: 18 }}>
            <Space direction="vertical" style={{ width: "100%" }}>
              {scenes.map((scene) => (
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
                  <Typography.Text strong>{scene.name}</Typography.Text>
                  <div>
                    <Tag color="blue">{t("testScene.stepsCount", { count: scene.steps.length })}</Tag>
                  </div>
                </Card>
              ))}
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={16}>
          <Card className="glass-card" title={t("testScene.stepOrchestration")} style={{ borderRadius: 18 }}>
            <Table
              rowKey="id"
              pagination={false}
              dataSource={activeScene?.steps || []}
              columns={[
                { title: t("testScene.colOrder"), render: (_v, _r, idx) => idx + 1, width: 70 },
                { title: t("testScene.colStep"), dataIndex: "name" },
                { title: t("testScene.colMethod"), dataIndex: "method", render: (v) => <Tag>{v}</Tag> },
                { title: t("testScene.colPath"), dataIndex: "path" },
                { title: t("testScene.colWeight"), dataIndex: "weight" },
                {
                  title: t("testScene.colReorder"),
                  render: (_value, _row, index) => (
                    <Space>
                      <Button
                        size="small"
                        icon={<ArrowUpOutlined />}
                        onClick={() => {
                          if (!activeScene) return;
                          setScenes((prev) =>
                            prev.map((item) =>
                              item.id === activeScene.id
                                ? { ...item, steps: moveStep(item.steps, index, "up") }
                                : item
                            )
                          );
                        }}
                      />
                      <Button
                        size="small"
                        icon={<ArrowDownOutlined />}
                        onClick={() => {
                          if (!activeScene) return;
                          setScenes((prev) =>
                            prev.map((item) =>
                              item.id === activeScene.id
                                ? { ...item, steps: moveStep(item.steps, index, "down") }
                                : item
                            )
                          );
                        }}
                      />
                    </Space>
                  )
                }
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default TestScenePage;
