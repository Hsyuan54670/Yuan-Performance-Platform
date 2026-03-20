import { Card, Col, Empty, Row, Spin, Tree, Typography, message } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMenuTreeApi, type MenuNode } from "../../../api/system";
import { getRequestErrorMessage } from "../../../utils/request";

type MenuTreeNode = {
  key: number;
  title: string;
  children?: MenuTreeNode[];
};

const buildTreeData = (rows: MenuNode[]): MenuTreeNode[] =>
  rows.map((row) => ({
    key: row.id,
    title: row.path ? `${row.name} (${row.path})` : row.name,
    children: row.children?.length ? buildTreeData(row.children) : undefined
  }));

function SystemMenuPage() {
  const [rows, setRows] = useState<MenuNode[]>([]);
  const [loading, setLoading] = useState(true);
  const { t } = useTranslation();

  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true);
      try {
        const data = await getMenuTreeApi();
        if (active) {
          setRows(data);
        }
      } catch (error) {
        if (active) {
          setRows([]);
          message.error(getRequestErrorMessage(error, t("common.loadFailed")));
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    void load();
    return () => {
      active = false;
    };
  }, [t]);

  const treeData = useMemo(() => buildTreeData(rows), [rows]);

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
            {loading ? (
              <div style={{ minHeight: 240, display: "grid", placeItems: "center" }}>
                <Spin />
              </div>
            ) : treeData.length ? (
              <Tree treeData={treeData} defaultExpandAll />
            ) : (
              <Empty description={t("common.noData")} />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default SystemMenuPage;
