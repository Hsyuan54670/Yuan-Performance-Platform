import { BellOutlined, MenuFoldOutlined, MenuUnfoldOutlined } from "@ant-design/icons";
import { Avatar, Badge, Breadcrumb, Button, Layout, Menu, Segmented, Space, Tag, Typography } from "antd";
import type { MenuProps } from "antd";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { logoutApi } from "../api/auth";
import { useAuth } from "../hooks/useAuth";
import {
  appMenus,
  collectAllowedPaths,
  filterMenuTreeByAccess,
  findMenuChain,
  flattenKeys,
  resolveFirstAccessiblePath
} from "../router/app-navigation";
import { useAppStore } from "../store/appStore";

const { Header, Content, Sider } = Layout;

function MainLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { collapsed, setCollapsed } = useAppStore();
  const { user, logout } = useAuth();
  const { t, i18n } = useTranslation();

  const allowedMenuPaths = useMemo(
    () => (user?.menus?.length ? collectAllowedPaths(user.menus) : undefined),
    [user?.menus]
  );

  const visibleMenus = useMemo(
    () => filterMenuTreeByAccess(appMenus, user?.permissions ?? [], allowedMenuPaths),
    [allowedMenuPaths, user?.permissions]
  );

  const firstVisiblePath = useMemo(
    () => resolveFirstAccessiblePath(user?.permissions ?? [], allowedMenuPaths) ?? "/",
    [allowedMenuPaths, user?.permissions]
  );

  const menuItems = useMemo<NonNullable<MenuProps["items"]>>(
    () =>
      visibleMenus.map((item) => ({
        key: item.key,
        icon: item.icon,
        label: t(item.labelKey),
        children: item.children?.map((child) => ({
          key: child.key,
          label: t(child.labelKey),
          icon: child.icon
        }))
      })),
    [t, visibleMenus]
  );

  const allKeys = useMemo(() => flattenKeys(menuItems), [menuItems]);

  const selected = useMemo(() => {
    const matched = allKeys.find((key) => location.pathname.startsWith(key));
    return matched ? [matched] : firstVisiblePath && firstVisiblePath !== "/" ? [firstVisiblePath] : [];
  }, [allKeys, firstVisiblePath, location.pathname]);

  const breadcrumbItems = useMemo(() => {
    const chain = findMenuChain(visibleMenus, location.pathname);
    if (!chain.length) return [{ title: t("layout.breadcrumbFallback") }];
    return chain.map((item) => ({ title: t(item.labelKey) }));
  }, [location.pathname, t, visibleMenus]);

  const handleLogout = async () => {
    try {
      await logoutApi();
    } finally {
      logout();
      navigate("/login", { replace: true });
    }
  };

  return (
    <Layout style={{ minHeight: "100vh", background: "transparent" }}>
      <Sider
        width={260}
        collapsible
        trigger={null}
        collapsed={collapsed}
        style={{
          background: "linear-gradient(165deg, rgba(11,114,133,0.95), rgba(9,70,92,0.95))",
          borderRight: "1px solid rgba(255,255,255,0.12)",
          position: "sticky",
          top: 0,
          height: "100vh"
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 12,
            padding: "18px 16px",
            color: "#fff",
            fontWeight: 700,
            letterSpacing: "0.03em"
          }}
        >
          <div
            style={{
              width: 34,
              height: 34,
              borderRadius: 12,
              background: "linear-gradient(140deg, #ffd8a8, #fab005)",
              boxShadow: "0 6px 18px rgba(250,176,5,0.35)"
            }}
          />
          {!collapsed ? t("layout.brand") : "YP"}
        </div>

        {menuItems.length ? (
          <Menu
            theme="dark"
            mode="inline"
            selectedKeys={selected}
            items={menuItems}
            onClick={({ key }) => {
              if (String(key).startsWith("/")) {
                navigate(String(key));
              }
            }}
            style={{ background: "transparent", border: 0 }}
          />
        ) : (
          <div style={{ padding: "12px 16px", color: "rgba(255,255,255,0.75)" }}>
            <Typography.Text style={{ color: "inherit" }}>{t("layout.noMenus")}</Typography.Text>
          </div>
        )}
      </Sider>

      <Layout style={{ background: "transparent" }}>
        <Header
          style={{
            margin: 12,
            padding: "0 18px",
            borderRadius: 16,
            background: "rgba(255,255,255,0.75)",
            border: "1px solid rgba(11,114,133,0.15)",
            boxShadow: "0 10px 30px rgba(20,46,66,0.08)",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between"
          }}
        >
          <Space>
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
            />
            <Breadcrumb items={breadcrumbItems} />
          </Space>

          <Space size={16}>
            <Segmented
              size="small"
              value={i18n.resolvedLanguage === "en-US" ? "en-US" : "zh-CN"}
              options={[
                { label: t("common.zhCN"), value: "zh-CN" },
                { label: t("common.enUS"), value: "en-US" }
              ]}
              onChange={(lang) => {
                void i18n.changeLanguage(lang as "zh-CN" | "en-US");
              }}
            />
            <Badge dot>
              <BellOutlined style={{ fontSize: 18 }} />
            </Badge>
            <Tag color="geekblue">{t("layout.gateway")}</Tag>
            <Space>
              <Avatar style={{ backgroundColor: "#0b7285" }}>{(user?.nickname || "U").slice(0, 1)}</Avatar>
              <Typography.Text strong>{user?.nickname || t("layout.userDefault")}</Typography.Text>
              <Button type="link" onClick={() => void handleLogout()}>
                {t("layout.logout")}
              </Button>
            </Space>
          </Space>
        </Header>

        <Content style={{ margin: "0 12px 12px", borderRadius: 16, overflow: "hidden" }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}

export default MainLayout;

