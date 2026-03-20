export const PermissionCodes = {
  DASHBOARD_VIEW: "dashboard.view",
  TEST_PLAN_READ: "test.plan.read",
  TEST_PLAN_WRITE: "test.plan.write",
  TEST_SCENE_READ: "test.scene.read",
  TEST_SCENE_WRITE: "test.scene.write",
  TEST_TASK_READ: "test.task.read",
  TEST_TASK_CREATE: "test.task.create",
  TEST_TASK_START: "test.task.start",
  TEST_TASK_STOP: "test.task.stop",
  MONITOR_REALTIME_VIEW: "monitor.realtime.view",
  MONITOR_ALERT_READ: "monitor.alert.read",
  MONITOR_ALERT_WRITE: "monitor.alert.write",
  ANALYSIS_REPORT_VIEW: "analysis.report.view",
  ANALYSIS_RULE_READ: "analysis.rule.read",
  ANALYSIS_RULE_WRITE: "analysis.rule.write",
  REPORT_CENTER_VIEW: "report.center.view",
  REPORT_CENTER_EXPORT: "report.center.export",
  SYSTEM_USER_READ: "system.user.read",
  SYSTEM_USER_WRITE: "system.user.write",
  SYSTEM_ROLE_READ: "system.role.read",
  SYSTEM_ROLE_WRITE: "system.role.write",
  SYSTEM_MENU_READ: "system.menu.read",
  SYSTEM_MENU_WRITE: "system.menu.write"
} as const;

export type PermissionCode = (typeof PermissionCodes)[keyof typeof PermissionCodes];
