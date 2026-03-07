import { Card, Col, Row, Tree, Typography } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMenuTreeApi, type MenuNode } from "../../../api/system";

function SystemMenuPage() {
  const [rows, setRows] = useState<MenuNode[]>([]);
  const { t } = useTranslation();

  useEffect(() => {
    getMenuTreeApi().then(setRows);
  }, []);

  const treeData = useMemo(
    () =>
      rows.map((row) => ({
        key: row.id,
        title: `${row.name} (${row.path})`
      })),
    [rows]
  );

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Typography.Title level={3} style={{ margin: 0 }}>{t("systemMenu.title")}</Typography.Title>
            <Typography.Text type="secondary">{t("systemMenu.subtitle")}</Typography.Text>
          </Card>
        </Col>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Tree treeData={treeData} defaultExpandAll />
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default SystemMenuPage;
