import { Card, Col, Row, Table, Tag, Typography } from "antd";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { listRolesApi, type RoleRow } from "../../../api/system";

function SystemRolePage() {
  const [rows, setRows] = useState<RoleRow[]>([]);
  const { t } = useTranslation();

  useEffect(() => {
    listRolesApi().then(setRows);
  }, []);

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Typography.Title level={3} style={{ margin: 0 }}>{t("systemRole.title")}</Typography.Title>
            <Typography.Text type="secondary">{t("systemRole.subtitle")}</Typography.Text>
          </Card>
        </Col>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Table
              rowKey="id"
              dataSource={rows}
              columns={[
                { title: t("systemRole.colId"), dataIndex: "id", width: 90 },
                { title: t("systemRole.colRole"), dataIndex: "name", render: (v: string) => <Tag color="geekblue">{v}</Tag> },
                { title: t("systemRole.colDescription"), dataIndex: "description" }
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default SystemRolePage;
