import { Button, Result, Space, Spin } from "antd";
import type { ReactNode } from "react";
import { Suspense, lazy } from "react";
import { useTranslation } from "react-i18next";
import { Navigate, Outlet, useLocation, useNavigate, useRoutes } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import MainLayout from "../layouts/MainLayout";
import { appMenus, canAccessPath, collectAllowedPaths, resolveFirstAccessiblePath } from "./app-navigation";

const LoginPage = lazy(() => import("../pages/login"));
const DashboardPage = lazy(() => import("../pages/dashboard"));
const TestPlanPage = lazy(() => import("../pages/test/plan"));
const TestScenePage = lazy(() => import("../pages/test/scene"));
const TestTaskCreatePage = lazy(() => import("../pages/test/task-create"));
const TestTaskPage = lazy(() => import("../pages/test/task"));
const MonitorRealtimePage = lazy(() => import("../pages/monitor/realtime"));
const MonitorAlertPage = lazy(() => import("../pages/monitor/alert"));
const AnalysisReportPage = lazy(() => import("../pages/analysis/report"));
const AnalysisRulePage = lazy(() => import("../pages/analysis/rule"));
const ReportPage = lazy(() => import("../pages/report"));
const SystemUserPage = lazy(() => import("../pages/system/user"));
const SystemRolePage = lazy(() => import("../pages/system/role"));
const SystemMenuPage = lazy(() => import("../pages/system/menu"));

const RouteFallback = () => {
  const { t } = useTranslation();
  return (
    <div style={{ minHeight: "42vh", display: "grid", placeItems: "center" }}>
      <Space direction="vertical" size="middle" align="center">
        <Spin size="large" />
        <span>{t("common.loadingModule")}</span>
      </Space>
    </div>
  );
};

const withSuspense = (node: ReactNode) => <Suspense fallback={<RouteFallback />}>{node}</Suspense>;

const Protected = () => {
  const { loggedIn, user } = useAuth();
  const location = useLocation();

  if (!loggedIn) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (!user) {
    return <RouteFallback />;
  }

  return <Outlet />;
};

const DefaultRoute = () => {
  const { user } = useAuth();

  if (!user) {
    return <RouteFallback />;
  }

  const allowedPaths = user.menus?.length ? collectAllowedPaths(user.menus) : undefined;
  const fallbackPath = resolveFirstAccessiblePath(user.permissions, allowedPaths);
  return <Navigate to={fallbackPath || "/login"} replace />;
};

function PermissionGuard({ path, children }: { path: string; children: ReactNode }) {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { user } = useAuth();

  if (!user) {
    return <RouteFallback />;
  }

  const allowedPaths = user.menus?.length ? collectAllowedPaths(user.menus) : undefined;
  if (canAccessPath(path, user.permissions, allowedPaths)) {
    return <>{children}</>;
  }

  const fallbackPath = resolveFirstAccessiblePath(user.permissions, allowedPaths);

  return (
    <div style={{ minHeight: "42vh", display: "grid", placeItems: "center" }}>
      <Result
        status="403"
        title={t("common.accessDenied")}
        subTitle={t("common.accessDeniedHint")}
        extra={
          fallbackPath ? (
            <Button type="primary" onClick={() => navigate(fallbackPath, { replace: true })}>
              {t("common.goToAvailablePage")}
            </Button>
          ) : undefined
        }
      />
    </div>
  );
}

const withPermissionGuard = (path: string, node: ReactNode) => (
  <PermissionGuard path={path}>{withSuspense(node)}</PermissionGuard>
);

export const AppRouter = () => {
  return useRoutes([
    { path: "/login", element: withSuspense(<LoginPage />) },
    {
      path: "/",
      element: <Protected />,
      children: [
        {
          path: "/",
          element: <MainLayout />,
          children: [
            { index: true, element: <DefaultRoute /> },
            { path: "/dashboard", element: withPermissionGuard("/dashboard", <DashboardPage />) },
            { path: "/test/plan", element: withPermissionGuard("/test/plan", <TestPlanPage />) },
            { path: "/test/scene", element: withPermissionGuard("/test/scene", <TestScenePage />) },
            { path: "/test/task/create", element: withPermissionGuard("/test/task/create", <TestTaskCreatePage />) },
            { path: "/test/task", element: withPermissionGuard("/test/task", <TestTaskPage />) },
            { path: "/monitor/realtime", element: withPermissionGuard("/monitor/realtime", <MonitorRealtimePage />) },
            { path: "/monitor/alert", element: withPermissionGuard("/monitor/alert", <MonitorAlertPage />) },
            { path: "/analysis/report", element: withPermissionGuard("/analysis/report", <AnalysisReportPage />) },
            { path: "/analysis/rule", element: withPermissionGuard("/analysis/rule", <AnalysisRulePage />) },
            { path: "/report", element: withPermissionGuard("/report", <ReportPage />) },
            { path: "/system/user", element: withPermissionGuard("/system/user", <SystemUserPage />) },
            { path: "/system/role", element: withPermissionGuard("/system/role", <SystemRolePage />) },
            { path: "/system/menu", element: withPermissionGuard("/system/menu", <SystemMenuPage />) }
          ]
        }
      ]
    },
    { path: "*", element: <Navigate to="/" replace /> }
  ]);
};
