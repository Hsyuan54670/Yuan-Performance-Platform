import { DeleteOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons";
import { Button, Card, Checkbox, Col, Divider, Drawer, Empty, Form, Input, Popconfirm, Row, Space, Spin, Table, Tag, Tree, Typography, message } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAuth } from "../../../hooks/useAuth";
import {
  createRoleApi,
  deleteRoleApi,
  getMenuTreeApi,
  getPermissionsApi,
  getRoleMenuIdsApi,
  getRolePermissionIdsApi,
  listRolesApi,
  updateRoleApi,
  updateRoleMenuIdsApi,
  updateRolePermissionIdsApi,
  type MenuNode,
  type PermissionRow,
  type RolePayload,
  type RoleRow
} from "../../../api/system";
import { PermissionCodes } from "../../../utils/permissions";
import { getRequestErrorMessage } from "../../../utils/request";

type MenuTreeNode = {
  key: number;
  title: string;
  children?: MenuTreeNode[];
};

const buildMenuTreeData = (rows: MenuNode[]): MenuTreeNode[] =>
  rows.map((row) => ({
    key: row.id,
    title: row.path ? `${row.name} (${row.path})` : row.name,
    children: row.children?.length ? buildMenuTreeData(row.children) : undefined
  }));

function SystemRolePage() {
  const [rows, setRows] = useState<RoleRow[]>([]);
  const [permissions, setPermissions] = useState<PermissionRow[]>([]);
  const [menus, setMenus] = useState<MenuNode[]>([]);
  const [loading, setLoading] = useState(true);
  const [drawerLoading, setDrawerLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingRole, setEditingRole] = useState<RoleRow | null>(null);
  const [selectedPermissionIds, setSelectedPermissionIds] = useState<number[]>([]);
  const [checkedMenuIds, setCheckedMenuIds] = useState<number[]>([]);
  const [form] = Form.useForm<RolePayload>();
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const canManageRoles = hasPermission(PermissionCodes.SYSTEM_ROLE_WRITE);

  const loadPageData = async () => {
    setLoading(true);
    try {
      const [roleRows, permissionRows, menuRows] = await Promise.all([
        listRolesApi(),
        getPermissionsApi(),
        getMenuTreeApi()
      ]);
      setRows(roleRows);
      setPermissions(permissionRows);
      setMenus(menuRows);
    } catch (error) {
      setRows([]);
      setPermissions([]);
      setMenus([]);
      message.error(getRequestErrorMessage(error, t("common.loadFailed")));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadPageData();
  }, []);

  const openCreateDrawer = () => {
    setEditingRole(null);
    setSelectedPermissionIds([]);
    setCheckedMenuIds([]);
    form.setFieldsValue({ code: "", name: "", description: "" });
    setDrawerLoading(false);
    setDrawerOpen(true);
  };

  const openEditDrawer = async (role: RoleRow) => {
    setEditingRole(role);
    setSelectedPermissionIds([]);
    setCheckedMenuIds([]);
    form.setFieldsValue({ code: role.code, name: role.name, description: role.description });
    setDrawerOpen(true);
    setDrawerLoading(true);
    try {
      const [permissionIds, menuIds] = await Promise.all([
        getRolePermissionIdsApi(role.id),
        getRoleMenuIdsApi(role.id)
      ]);
      setSelectedPermissionIds(permissionIds);
      setCheckedMenuIds(menuIds);
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("systemRole.bindingLoadFailed")));
    } finally {
      setDrawerLoading(false);
    }
  };

  const closeDrawer = () => {
    setDrawerOpen(false);
    setEditingRole(null);
    setSelectedPermissionIds([]);
    setCheckedMenuIds([]);
    form.resetFields();
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);

      let roleId = editingRole?.id ?? null;
      if (editingRole) {
        await updateRoleApi(editingRole.id, values);
      } else {
        roleId = await createRoleApi(values);
      }

      if (!roleId) {
        throw new Error("Role id is missing after save");
      }

      await Promise.all([
        updateRolePermissionIdsApi(roleId, { permissionIds: selectedPermissionIds }),
        updateRoleMenuIdsApi(roleId, { menuIds: checkedMenuIds })
      ]);

      message.success(editingRole ? t("systemRole.updateSuccess") : t("systemRole.createSuccess"));
      closeDrawer();
      await loadPageData();
    } catch (error) {
      if (typeof error === "object" && error && "errorFields" in error) {
        return;
      }
      message.error(getRequestErrorMessage(error, t("common.saveFailed")));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (roleId: number) => {
    try {
      await deleteRoleApi(roleId);
      message.success(t("systemRole.deleteSuccess"));
      await loadPageData();
    } catch (error) {
      message.error(getRequestErrorMessage(error, t("common.deleteFailed")));
    }
  };

  const togglePermission = (permissionId: number, checked: boolean) => {
    setSelectedPermissionIds((current) => {
      if (checked) {
        return current.includes(permissionId) ? current : [...current, permissionId];
      }
      return current.filter((id) => id !== permissionId);
    });
  };

  const permissionGroups = useMemo(() => {
    const groups = new Map<string, PermissionRow[]>();
    permissions.forEach((permission) => {
      const key = permission.module || t("systemRole.moduleFallback");
      const rowsInGroup = groups.get(key) ?? [];
      rowsInGroup.push(permission);
      groups.set(key, rowsInGroup);
    });
    return Array.from(groups.entries());
  }, [permissions, t]);

  const menuTreeData = useMemo(() => buildMenuTreeData(menus), [menus]);

  return (
    <div className="page-shell">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card className="glass-card" style={{ borderRadius: 18 }}>
            <Space style={{ width: "100%", justifyContent: "space-between" }} wrap>
              <Space direction="vertical" size={4}>
                <Typography.Title level={3} style={{ margin: 0 }}>{t("systemRole.title")}</Typography.Title>
                <Typography.Text type="secondary">{t("systemRole.subtitle")}</Typography.Text>
              </Space>
              {canManageRoles ? (
                <Button type="primary" icon={<PlusOutlined />} onClick={openCreateDrawer}>
                  {t("systemRole.newRole")}
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
              dataSource={rows}
              columns={[
                { title: t("systemRole.colId"), dataIndex: "id", width: 90 },
                { title: t("systemRole.colCode"), dataIndex: "code", width: 160, render: (v: string) => <Tag color="geekblue">{v}</Tag> },
                { title: t("systemRole.colName"), dataIndex: "name", width: 180 },
                { title: t("systemRole.colDescription"), dataIndex: "description" },
                ...(canManageRoles
                  ? [{
                      title: t("systemRole.colAction"),
                      key: "action",
                      width: 180,
                      render: (_value: unknown, record: RoleRow) => (
                        <Space size="small">
                          <Button type="link" icon={<EditOutlined />} onClick={() => void openEditDrawer(record)}>
                            {t("systemRole.editRole")}
                          </Button>
                          <Popconfirm
                            title={t("systemRole.deleteConfirm")}
                            okText={t("systemRole.confirmDelete")}
                            cancelText={t("systemRole.cancel")}
                            onConfirm={() => void handleDelete(record.id)}
                          >
                            <Button type="link" danger icon={<DeleteOutlined />}>
                              {t("systemRole.deleteRole")}
                            </Button>
                          </Popconfirm>
                        </Space>
                      )
                    }]
                  : [])
              ]}
            />
          </Card>
        </Col>
      </Row>

      <Drawer
        title={editingRole ? t("systemRole.editDrawerTitle") : t("systemRole.drawerTitle")}
        open={drawerOpen}
        onClose={closeDrawer}
        destroyOnHidden
        width={640}
        extra={
          canManageRoles ? (
            <Button type="primary" loading={submitting} onClick={() => void handleSubmit()}>
              {editingRole ? t("systemRole.saveChanges") : t("common.save")}
            </Button>
          ) : null
        }
      >
        {drawerLoading ? (
          <div style={{ minHeight: 320, display: "grid", placeItems: "center" }}>
            <Spin />
          </div>
        ) : (
          <>
            <Form layout="vertical" form={form}>
              <Form.Item name="code" label={t("systemRole.fieldCode")} rules={[{ required: true, message: t("systemRole.codeRequired") }]}>
                <Input placeholder="ADMIN" disabled={!canManageRoles} />
              </Form.Item>
              <Form.Item name="name" label={t("systemRole.fieldName")} rules={[{ required: true, message: t("systemRole.nameRequired") }]}>
                <Input placeholder="Administrator" disabled={!canManageRoles} />
              </Form.Item>
              <Form.Item name="description" label={t("systemRole.fieldDescription")}>
                <Input.TextArea rows={4} disabled={!canManageRoles} />
              </Form.Item>
            </Form>

            <Divider orientation="left">{t("systemRole.permissionsSection")}</Divider>
            <Typography.Paragraph type="secondary">{t("systemRole.permissionsHint")}</Typography.Paragraph>
            {permissionGroups.length ? (
              <Space direction="vertical" size={16} style={{ width: "100%" }}>
                {permissionGroups.map(([module, rowsInGroup]) => (
                  <div key={module}>
                    <Typography.Text strong>{module}</Typography.Text>
                    <div style={{ display: "grid", gap: 12, marginTop: 12 }}>
                      {rowsInGroup.map((permission) => (
                        <Checkbox
                          key={permission.id}
                          checked={selectedPermissionIds.includes(permission.id)}
                          disabled={!canManageRoles}
                          onChange={(event) => togglePermission(permission.id, event.target.checked)}
                        >
                          <Space direction="vertical" size={0}>
                            <Typography.Text>{permission.name}</Typography.Text>
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                              {permission.code}
                              {permission.description ? ` · ${permission.description}` : ""}
                            </Typography.Text>
                          </Space>
                        </Checkbox>
                      ))}
                    </div>
                  </div>
                ))}
              </Space>
            ) : (
              <Empty description={t("systemRole.noPermissions")} />
            )}

            <Divider orientation="left">{t("systemRole.menusSection")}</Divider>
            <Typography.Paragraph type="secondary">{t("systemRole.menusHint")}</Typography.Paragraph>
            {menuTreeData.length ? (
              <Tree
                checkable
                defaultExpandAll
                checkedKeys={checkedMenuIds}
                treeData={menuTreeData}
                selectable={false}
                disabled={!canManageRoles}
                onCheck={(checkedKeys) => {
                  const normalized = Array.isArray(checkedKeys) ? checkedKeys : checkedKeys.checked;
                  setCheckedMenuIds(normalized.map((key) => Number(key)));
                }}
              />
            ) : (
              <Empty description={t("systemRole.noMenus")} />
            )}
          </>
        )}
      </Drawer>
    </div>
  );
}

export default SystemRolePage;
