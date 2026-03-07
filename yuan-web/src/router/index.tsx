import {
  AlertOutlined,
  AreaChartOutlined,
  BarChartOutlined,
  DashboardOutlined,
  DeploymentUnitOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  LoginOutlined,
  RadarChartOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  UserOutlined
} from "@ant-design/icons";
import { Spin } from "antd";
import type { ReactNode } from "react";
import { Suspense, lazy } from "react";
import { useTranslation } from "react-i18next";
import { Navigate, Outlet, useLocation, useRoutes } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import MainLayout from "../layouts/MainLayout";

const LoginPage = lazy(() => import("../pages/login"));
const DashboardPage = lazy(() => import("../pages/dashboard"));
const TestPlanPage = lazy(() => import("../pages/test/plan"));
const TestScenePage = lazy(() => import("../pages/test/scene"));
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
      <Spin size="large" tip={t("common.loadingModule")} />
    </div>
  );
};

const withSuspense = (node: ReactNode) => <Suspense fallback={<RouteFallback />}>{node}</Suspense>;

export interface MenuItem {
  key: string;
  labelKey: string;
  icon?: ReactNode;
  children?: MenuItem[];
}

export const appMenus: MenuItem[] = [
  { key: "/dashboard", labelKey: "menu.dashboard", icon: <DashboardOutlined /> },
  {
    key: "test",
    labelKey: "menu.testGroup",
    icon: <ExperimentOutlined />,
    children: [
      { key: "/test/plan", labelKey: "menu.testPlan", icon: <DeploymentUnitOutlined /> },
      { key: "/test/scene", labelKey: "menu.testScene", icon: <RadarChartOutlined /> },
      { key: "/test/task", labelKey: "menu.testTask", icon: <AreaChartOutlined /> }
    ]
  },
  {
    key: "monitor",
    labelKey: "menu.monitorGroup",
    icon: <BarChartOutlined />,
    children: [
      { key: "/monitor/realtime", labelKey: "menu.monitorRealtime", icon: <SafetyCertificateOutlined /> },
      { key: "/monitor/alert", labelKey: "menu.monitorAlert", icon: <AlertOutlined /> }
    ]
  },
  {
    key: "analysis",
    labelKey: "menu.analysisGroup",
    icon: <FileSearchOutlined />,
    children: [
      { key: "/analysis/report", labelKey: "menu.analysisReport", icon: <FileTextOutlined /> },
      { key: "/analysis/rule", labelKey: "menu.analysisRule", icon: <AreaChartOutlined /> }
    ]
  },
  { key: "/report", labelKey: "menu.report", icon: <FileTextOutlined /> },
  {
    key: "system",
    labelKey: "menu.systemGroup",
    icon: <TeamOutlined />,
    children: [
      { key: "/system/user", labelKey: "menu.systemUser", icon: <UserOutlined /> },
      { key: "/system/role", labelKey: "menu.systemRole", icon: <TeamOutlined /> },
      { key: "/system/menu", labelKey: "menu.systemMenu", icon: <LoginOutlined /> }
    ]
  }
];

const Protected = () => {
  const { loggedIn } = useAuth();
  const location = useLocation();
  if (!loggedIn) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <Outlet />;
};

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
            { index: true, element: <Navigate to="/dashboard" replace /> },
            { path: "/dashboard", element: withSuspense(<DashboardPage />) },
            { path: "/test/plan", element: withSuspense(<TestPlanPage />) },
            { path: "/test/scene", element: withSuspense(<TestScenePage />) },
            { path: "/test/task", element: withSuspense(<TestTaskPage />) },
            { path: "/monitor/realtime", element: withSuspense(<MonitorRealtimePage />) },
            { path: "/monitor/alert", element: withSuspense(<MonitorAlertPage />) },
            { path: "/analysis/report", element: withSuspense(<AnalysisReportPage />) },
            { path: "/analysis/rule", element: withSuspense(<AnalysisRulePage />) },
            { path: "/report", element: withSuspense(<ReportPage />) },
            { path: "/system/user", element: withSuspense(<SystemUserPage />) },
            { path: "/system/role", element: withSuspense(<SystemRolePage />) },
            { path: "/system/menu", element: withSuspense(<SystemMenuPage />) }
          ]
        }
      ]
    },
    { path: "*", element: <Navigate to="/dashboard" replace /> }
  ]);
};
