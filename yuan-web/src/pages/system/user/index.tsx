import { PauseCircleOutlined, PlayCircleOutlined, PlusOutlined } from "@ant-design/icons";
import { Button, Card, Col, Form, Input, Modal, Popconfirm, Row, Select, Space, Table, Tag, Typography, message } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAuth } from "../../../hooks/useAuth";
import {
  createUserApi,
  listRolesApi,
  listUsersApi,
  type RoleRow,
  type UserCreatePayload,
  type UserRow,
  updateUserStatusApi
} from "../../../api/system";
import { PermissionCodes } from "../../../utils/permissions";
import { getRequestErrorMessage } from "../../../utils/request";

function SystemUserPage() {
  const [rows, setRows] = useState<UserRow[]>([]);
  const [roles, setRoles] = useState<RoleRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [roleLoading, setRoleLoading] = useState(false);
  const [actionUserId, setActionUserId] = useState<number | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [form] = Form.useForm<UserCreatePayload>();
  const { t } = useTranslation();
  const { user, hasPermission } = useAuth();
  const canManageUsers = hasPermission(PermissionCodes.SYSTEM_USER_WRITE);
  const canReadRoles = hasPermission(PermissionCodes.SYSTEM_ROLE_READ);

  const loadUsers = async () => {
    setLoading(true);
    try {
      setRows(await listUsersApi());
    } catch (error) {
      setRows([]);
      message.error(getRequestErrorMessage(error, t("common.loadFailed")));
    } finally {
      setLoading(false);
    }
  };

  const loadRoles = async () => {
    if (!canReadRoles) {
      setRoles([]);
      return;
    }
    setRoleLoading(true);
    try {
      setRoles(await listRolesApi());
    } catch (error) {
      setRoles([]);
      message.error(getRequestErrorMessage(error, t("common.loadFailed")));
    } finally {
      setRoleLoading(false);
    }
  };

  useEffect(() => {
    void loadUsers();
    void loadRoles();
  }, [canReadRoles]);

  const handleToggleStatus = async (record: UserRow) => {
    const nextStatus = record.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    try {
      setActionUserId(record.id);
      await updateUserStatusApi(record.id, { status: nextStatus });
      message.success(t("systemUser.switchSuccess"));
      await loadUsers();
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.saveFailed")));
    } finally {
      setActionUserId(null);
    }
  };

  const openCreateModal = () => {
    if (!roles.length) {
      message.warning(t("systemUser.noRolesAvailable"));
      return;
    }
    form.setFieldsValue({ username: "", nickname: "", password: "", roleId: roles[0]?.id });
    setCreateOpen(true);
  };

  const closeCreateModal = () => {
    setCreateOpen(false);
    form.resetFields();
  };

  const handleCreateUser = async () => {
    try {
      const values = await form.validateFields();
      setCreating(true);
      await createUserApi(values);
      message.success(t("systemUser.createSuccess"));
      closeCreateModal();
      await loadUsers();
    } catch (error) {
      if (typeof error === "object" && error && "errorFields" in error) {
        return;
      }
      message.error(getRequestErrorMessage(error, t("common.saveFailed")));
    } finally {
      setCreating(false);
    }
  };

  const columns = useMemo(
    () => [
      { title: t("systemUser.colId"), dataIndex: "id", width: 90 },
      { title: t("systemUser.colUsername"), dataIndex: "username" },
      { title: t("systemUser.colNickname"), dataIndex: "nickname" },
      { title: t("systemUser.colRole"), dataIndex: "role", render: (v: string) => <Tag>{v}</Tag> },
      {
        title: t("systemUser.colStatus"),
        dataIndex: "status",
        render: (v: string) => <Tag color={v === "ACTIVE" ? "green" : "red"}>{v}</Tag>
      },
      ...(canManageUsers
        ? [
            {
              title: t("systemUser.colAction"),
              key: "action",
              width: 180,
              render: (_value: unknown, record: UserRow) => {
                const isSelf = record.id === user?.id;
                const nextActionLabel = record.status === "ACTIVE" ? t("systemUser.disable") : t("systemUser.enable");
                return (
                  <Popconfirm
                    title={record.status === "ACTIVE" ? t("systemUser.disableConfirm") : t("systemUser.enableConfirm")}
                    okText={nextActionLabel}
                    cancelText={t("systemUser.cancel")}
                    onConfirm={() => void handleToggleStatus(record)}
                    disabled={isSelf}
                  >
                    <Button
                      type="link"
                      disabled={isSelf}
                      loading={actionUserId === record.id}
                      icon={record.status === "ACTIVE" ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                    >
                      {isSelf ? t("systemUser.currentUser") : nextActionLabel}
                    </Button>
                  </Popconfirm>
                );
              }
            }
          ]
        : [])
    ],
    [actionUserId, canManageUsers, t, user?.id]
  );

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space style={{ width: "100%", justifyContent: "space-between" }} wrap>
              <Space direction="vertical" size={4}>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("systemUser.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("systemUser.subtitle")}</Typography.Text>
              </Space>
              {canManageUsers ? (
                <Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
                  {t("systemUser.newUser")}
                </Button>
              ) : null}
            </Space>
          </Card>
        </Col>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Table rowKey="id" loading={loading} dataSource={rows} columns={columns} />
          </Card>
        </Col>
      </Row>

      <Modal
        title={t("systemUser.drawerTitle")}
        open={createOpen}
        onCancel={closeCreateModal}
        onOk={() => void handleCreateUser()}
        confirmLoading={creating}
        okText={t("common.save")}
        cancelText={t("systemUser.cancel")}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item name="username" label={t("systemUser.fieldUsername")} rules={[{ required: true, message: t("systemUser.usernameRequired") }]}>
            <Input />
          </Form.Item>
          <Form.Item name="nickname" label={t("systemUser.fieldNickname")} rules={[{ required: true, message: t("systemUser.nicknameRequired") }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label={t("systemUser.fieldPassword")} rules={[{ required: true, message: t("systemUser.passwordRequired") }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="roleId" label={t("systemUser.fieldRole")} rules={[{ required: true, message: t("systemUser.roleRequired") }]}>
            <Select
              loading={roleLoading}
              options={roles.map((role) => ({ value: role.id, label: `${role.name} (${role.code})` }))}
              placeholder={t("systemUser.rolePlaceholder")}
            />
          </Form.Item>
          {!roles.length ? (
            <Typography.Text type="secondary">{t("systemUser.noRolesHint")}</Typography.Text>
          ) : null}
        </Form>
      </Modal>
    </div>
  );
}

export default SystemUserPage;
