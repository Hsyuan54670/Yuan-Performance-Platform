package com.yuan.common.constant;

public final class PermissionCode {

    private PermissionCode() {
    }

    public static final String DASHBOARD_VIEW = "dashboard.view";

    public static final String TEST_PLAN_READ = "test.plan.read";
    public static final String TEST_PLAN_WRITE = "test.plan.write";
    public static final String TEST_SCENE_READ = "test.scene.read";
    public static final String TEST_SCENE_WRITE = "test.scene.write";
    public static final String TEST_TASK_READ = "test.task.read";
    public static final String TEST_TASK_CREATE = "test.task.create";
    public static final String TEST_TASK_START = "test.task.start";
    public static final String TEST_TASK_STOP = "test.task.stop";

    public static final String MONITOR_REALTIME_VIEW = "monitor.realtime.view";
    public static final String MONITOR_ALERT_READ = "monitor.alert.read";
    public static final String MONITOR_ALERT_WRITE = "monitor.alert.write";

    public static final String ANALYSIS_REPORT_VIEW = "analysis.report.view";
    public static final String ANALYSIS_RULE_READ = "analysis.rule.read";
    public static final String ANALYSIS_RULE_WRITE = "analysis.rule.write";

    public static final String REPORT_CENTER_VIEW = "report.center.view";
    public static final String REPORT_CENTER_EXPORT = "report.center.export";

    public static final String SYSTEM_USER_READ = "system.user.read";
    public static final String SYSTEM_USER_WRITE = "system.user.write";
    public static final String SYSTEM_ROLE_READ = "system.role.read";
    public static final String SYSTEM_ROLE_WRITE = "system.role.write";
    public static final String SYSTEM_MENU_READ = "system.menu.read";
    public static final String SYSTEM_MENU_WRITE = "system.menu.write";
}
