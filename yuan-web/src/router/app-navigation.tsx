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
  PlusCircleOutlined,
  RadarChartOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  UserOutlined
} from "@ant-design/icons";
import type { MenuProps } from "antd";
import type { ReactNode } from "react";
import type { AuthorizedMenu } from "../types/auth";
import { PermissionCodes, type PermissionCode } from "../utils/permissions";

export interface MenuItem {
  key: string;
  labelKey: string;
  icon?: ReactNode;
  children?: MenuItem[];
  requiredPermissions?: PermissionCode[];
}

export const appMenus: MenuItem[] = [
  {
    key: "/dashboard",
    labelKey: "menu.dashboard",
    icon: <DashboardOutlined />,
    requiredPermissions: [PermissionCodes.DASHBOARD_VIEW]
  },
  {
    key: "test",
    labelKey: "menu.testGroup",
    icon: <ExperimentOutlined />,
    children: [
      {
        key: "/test/plan",
        labelKey: "menu.testPlan",
        icon: <DeploymentUnitOutlined />,
        requiredPermissions: [PermissionCodes.TEST_PLAN_READ]
      },
      {
        key: "/test/scene",
        labelKey: "menu.testScene",
        icon: <RadarChartOutlined />,
        requiredPermissions: [PermissionCodes.TEST_SCENE_READ]
      },
      {
        key: "/test/task/create",
        labelKey: "menu.testTaskCreate",
        icon: <PlusCircleOutlined />,
        requiredPermissions: [PermissionCodes.TEST_TASK_CREATE]
      },
      {
        key: "/test/task",
        labelKey: "menu.testTask",
        icon: <AreaChartOutlined />,
        requiredPermissions: [PermissionCodes.TEST_TASK_READ]
      }
    ]
  },
  {
    key: "monitor",
    labelKey: "menu.monitorGroup",
    icon: <BarChartOutlined />,
    children: [
      {
        key: "/monitor/realtime",
        labelKey: "menu.monitorRealtime",
        icon: <SafetyCertificateOutlined />,
        requiredPermissions: [PermissionCodes.MONITOR_REALTIME_VIEW]
      },
      {
        key: "/monitor/alert",
        labelKey: "menu.monitorAlert",
        icon: <AlertOutlined />,
        requiredPermissions: [PermissionCodes.MONITOR_ALERT_READ]
      }
    ]
  },
  {
    key: "analysis",
    labelKey: "menu.analysisGroup",
    icon: <FileSearchOutlined />,
    children: [
      {
        key: "/analysis/report",
        labelKey: "menu.analysisReport",
        icon: <FileTextOutlined />,
        requiredPermissions: [PermissionCodes.ANALYSIS_REPORT_VIEW]
      },
      {
        key: "/analysis/rule",
        labelKey: "menu.analysisRule",
        icon: <AreaChartOutlined />,
        requiredPermissions: [PermissionCodes.ANALYSIS_RULE_READ]
      }
    ]
  },
  {
    key: "/report",
    labelKey: "menu.report",
    icon: <FileTextOutlined />,
    requiredPermissions: [PermissionCodes.REPORT_CENTER_VIEW]
  },
  {
    key: "system",
    labelKey: "menu.systemGroup",
    icon: <TeamOutlined />,
    children: [
      {
        key: "/system/user",
        labelKey: "menu.systemUser",
        icon: <UserOutlined />,
        requiredPermissions: [PermissionCodes.SYSTEM_USER_READ]
      },
      {
        key: "/system/role",
        labelKey: "menu.systemRole",
        icon: <TeamOutlined />,
        requiredPermissions: [PermissionCodes.SYSTEM_ROLE_READ]
      },
      {
        key: "/system/menu",
        labelKey: "menu.systemMenu",
        icon: <LoginOutlined />,
        requiredPermissions: [PermissionCodes.SYSTEM_MENU_READ]
      }
    ]
  }
];

const hasRequiredPermission = (permissions: string[], requiredPermissions?: PermissionCode[]) => {
  if (!requiredPermissions?.length) {
    return true;
  }
  return requiredPermissions.some((permission) => permissions.includes(permission));
};

const findBestMatchingItem = (source: MenuItem[], path: string): MenuItem | null => {
  let matched: MenuItem | null = null;

  for (const item of source) {
    if (item.children?.length) {
      const matchedChild = findBestMatchingItem(item.children, path);
      if (matchedChild && (!matched || matchedChild.key.length > matched.key.length)) {
        matched = matchedChild;
      }
    }

    if (item.key.startsWith("/") && path.startsWith(item.key) && (!matched || item.key.length > matched.key.length)) {
      matched = item;
    }
  }

  return matched;
};

const findFirstLeafPath = (source: MenuItem[]): string | null => {
  for (const item of source) {
    if (item.children?.length) {
      const childPath = findFirstLeafPath(item.children);
      if (childPath) {
        return childPath;
      }
    } else if (item.key.startsWith("/")) {
      return item.key;
    }
  }

  return null;
};

export const flattenKeys = (items: MenuProps["items"]): string[] => {
  if (!items) return [];
  return items.flatMap((item) => {
    if (!item || typeof item === "string") return [];
    if ("children" in item && item.children?.length) {
      return [String(item.key), ...flattenKeys(item.children)];
    }
    return [String(item.key)];
  });
};

export const findMenuChain = (source: MenuItem[], path: string): MenuItem[] => {
  for (const item of source) {
    if (item.children?.length) {
      const matchedChild = item.children
        .filter((child) => path.startsWith(child.key))
        .sort((a, b) => b.key.length - a.key.length)[0];
      if (matchedChild) {
        return [item, matchedChild];
      }
    }
    if (item.key.startsWith("/") && path.startsWith(item.key)) {
      return [item];
    }
  }
  return [];
};

export const collectAllowedPaths = (menus: AuthorizedMenu[]): Set<string> => {
  const paths = new Set<string>();

  const walk = (nodes: AuthorizedMenu[]) => {
    nodes.forEach((node) => {
      if (node.path) {
        paths.add(node.path);
      }
      if (node.children?.length) {
        walk(node.children);
      }
    });
  };

  walk(menus);
  return paths;
};

export const filterMenuTreeByAccess = (
  source: MenuItem[],
  permissions: string[],
  allowedPaths?: Set<string>
): MenuItem[] =>
  source.flatMap((item) => {
    const children = item.children?.length ? filterMenuTreeByAccess(item.children, permissions, allowedPaths) : undefined;

    if (children?.length) {
      return [{ ...item, children }];
    }

    const allowedByPermission = hasRequiredPermission(permissions, item.requiredPermissions);
    const allowedByMenu = !allowedPaths || allowedPaths.has(item.key);
    return item.key.startsWith("/") && allowedByPermission && allowedByMenu ? [{ ...item }] : [];
  });

export const canAccessPath = (path: string, permissions: string[], allowedPaths?: Set<string>): boolean => {
  const matched = findBestMatchingItem(appMenus, path);
  if (!matched) {
    return true;
  }

  const allowedByPermission = hasRequiredPermission(permissions, matched.requiredPermissions);
  const allowedByMenu = !allowedPaths || allowedPaths.has(matched.key);
  return allowedByPermission && allowedByMenu;
};

export const resolveFirstAccessiblePath = (permissions: string[], allowedPaths?: Set<string>): string | null => {
  const filtered = filterMenuTreeByAccess(appMenus, permissions, allowedPaths);
  return findFirstLeafPath(filtered);
};
