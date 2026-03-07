import { BellOutlined, MenuFoldOutlined, MenuUnfoldOutlined } from "@ant-design/icons";
import { Avatar, Badge, Breadcrumb, Button, Layout, Menu, Segmented, Space, Tag, Typography } from "antd";
import type { MenuProps } from "antd";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { appMenus, type MenuItem } from "../router";
import { useAppStore } from "../store/appStore";

const { Header, Content, Sider } = Layout;

const flattenKeys = (items: MenuProps["items"]): string[] => {
  if (!items) return [];
  return items.flatMap((item) => {
    if (!item || typeof item === "string") return [];
    if ("children" in item && item.children?.length) {
      return [String(item.key), ...flattenKeys(item.children)];
    }
    return [String(item.key)];
  });
};

const findMenuChain = (source: MenuItem[], path: string): MenuItem[] => {
  for (const item of source) {
    if (item.children?.length) {
      const matchedChild = item.children.find((child) => path.startsWith(child.key));
      if (matchedChild) return [item, matchedChild];
    }
    if (item.key.startsWith("/") && path.startsWith(item.key)) {
      return [item];
    }
  }
  return [];
};

function MainLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { collapsed, setCollapsed } = useAppStore();
  const { user, logout } = useAuth();
  const { t, i18n } = useTranslation();

  const menuItems = useMemo<MenuProps["items"]>(
    () =>
      appMenus.map((item) => ({
        key: item.key,
        icon: item.icon,
        label: t(item.labelKey),
        children: item.children?.map((child) => ({
          key: child.key,
          label: t(child.labelKey),
          icon: child.icon
        }))
      })),
    [t]
  );

  const allKeys = useMemo(() => flattenKeys(menuItems), [menuItems]);

  const selected = useMemo(() => {
    const matched = allKeys.find((key) => location.pathname.startsWith(key));
    return matched ? [matched] : ["/dashboard"];
  }, [location.pathname, allKeys]);

  const breadcrumbItems = useMemo(() => {
    const chain = findMenuChain(appMenus, location.pathname);
    if (!chain.length) return [{ title: t("layout.breadcrumbFallback") }];
    return chain.map((item) => ({ title: t(item.labelKey) }));
  }, [location.pathname, t]);

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

        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selected}
          items={menuItems}
          onClick={({ key }) => {
            if (String(key).startsWith("/")) navigate(String(key));
          }}
          style={{ background: "transparent", border: 0 }}
        />
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
              <Button type="link" onClick={logout}>
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
