import { Card, Col, Row, Table, Tag, Typography } from "antd";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { listUsersApi, type UserRow } from "../../../api/system";

function SystemUserPage() {
  const [rows, setRows] = useState<UserRow[]>([]);
  const { t } = useTranslation();

  useEffect(() => {
    listUsersApi().then(setRows);
  }, []);

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Typography.Title level={3} style={{ margin: 0 }}>{t("systemUser.title")}</Typography.Title>
            <Typography.Text type="secondary">{t("systemUser.subtitle")}</Typography.Text>
          </Card>
        </Col>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Table
              rowKey="id"
              dataSource={rows}
              columns={[
                { title: t("systemUser.colId"), dataIndex: "id", width: 90 },
                { title: t("systemUser.colUsername"), dataIndex: "username" },
                { title: t("systemUser.colNickname"), dataIndex: "nickname" },
                { title: t("systemUser.colRole"), dataIndex: "role", render: (v: string) => <Tag>{v}</Tag> },
                { title: t("systemUser.colStatus"), dataIndex: "status", render: (v: string) => <Tag color="green">{v}</Tag> }
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default SystemUserPage;
