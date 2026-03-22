SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

USE yuan_auth;

INSERT INTO auth_permission (code, name, module, description)
VALUES
    ('dashboard.view', '查看仪表盘', 'dashboard', '查看首页仪表盘与概览数据'),
    ('test.plan.read', '查看压测计划', 'test', '查看压测计划列表与详情'),
    ('test.plan.write', '管理压测计划', 'test', '新增、编辑、删除压测计划'),
    ('test.scene.read', '查看场景', 'test', '查看场景编排与步骤详情'),
    ('test.scene.write', '管理场景', 'test', '新增、编辑、删除压测场景'),
    ('test.task.read', '查看任务控制台', 'test', '查看压测任务与运行记录'),
    ('test.task.create', '创建任务', 'test', '通过计划与场景组合生成任务'),
    ('test.task.start', '启动任务', 'test', '启动压测任务'),
    ('test.task.stop', '停止任务', 'test', '停止运行中的压测任务'),
    ('monitor.realtime.view', '查看实时监控', 'monitor', '查看业务与资源实时趋势'),
    ('monitor.alert.read', '查看告警管理', 'monitor', '查看告警规则、记录与活跃告警'),
    ('monitor.alert.write', '管理告警规则', 'monitor', '新增、编辑、启停和删除告警规则'),
    ('analysis.report.view', '查看分析报告', 'analysis', '查看 run 级分析报告与 HTML 预览'),
    ('analysis.rule.read', '查看分析规则', 'analysis', '查看规则引擎规则与 AI 规则'),
    ('analysis.rule.write', '管理分析规则', 'analysis', '新增、编辑、启停和删除分析规则'),
    ('report.center.view', '查看报告中心', 'report', '查看报告中心列表与详情预览'),
    ('report.center.export', '导出报告', 'report', '导出 HTML 报告'),
    ('system.user.read', '查看用户管理', 'system', '查看系统用户列表'),
    ('system.user.write', '管理用户', 'system', '新增用户并启停用户'),
    ('system.role.read', '查看角色管理', 'system', '查看角色列表与绑定信息'),
    ('system.role.write', '管理角色', 'system', '新增、编辑、删除角色并绑定权限/菜单'),
    ('system.menu.read', '查看菜单管理', 'system', '查看系统菜单树'),
    ('system.menu.write', '管理菜单', 'system', '调整菜单绑定与排序')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    module = VALUES(module),
    description = VALUES(description);

INSERT INTO auth_role (code, name, description, created_at)
SELECT 'ADMIN', '系统管理员', '拥有平台全部管理能力', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM auth_role WHERE code = 'ADMIN');

INSERT INTO auth_role (code, name, description, created_at)
SELECT 'PERF_ENGINEER', '压测工程师', '负责计划、场景、任务、监控和分析', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM auth_role WHERE code = 'PERF_ENGINEER');

INSERT INTO auth_role (code, name, description, created_at)
SELECT 'VIEWER', '只读观察员', '只查看仪表盘、监控和报告，不执行写操作', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM auth_role WHERE code = 'VIEWER');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '仪表盘', '/dashboard', 0, 10, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/dashboard');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '压测管理', '/test', 0, 20, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/test');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '压测计划', '/test/plan', parent.id, 10, NOW()
FROM auth_menu parent
WHERE parent.path = '/test'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/test/plan');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '场景编排', '/test/scene', parent.id, 20, NOW()
FROM auth_menu parent
WHERE parent.path = '/test'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/test/scene');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '创建任务', '/test/task/create', parent.id, 30, NOW()
FROM auth_menu parent
WHERE parent.path = '/test'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/test/task/create');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '任务控制台', '/test/task', parent.id, 40, NOW()
FROM auth_menu parent
WHERE parent.path = '/test'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/test/task');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '监控中心', '/monitor', 0, 30, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/monitor');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '实时监控', '/monitor/realtime', parent.id, 10, NOW()
FROM auth_menu parent
WHERE parent.path = '/monitor'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/monitor/realtime');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '告警管理', '/monitor/alert', parent.id, 20, NOW()
FROM auth_menu parent
WHERE parent.path = '/monitor'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/monitor/alert');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '智能分析', '/analysis', 0, 40, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/analysis');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '分析报告', '/analysis/report', parent.id, 10, NOW()
FROM auth_menu parent
WHERE parent.path = '/analysis'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/analysis/report');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '分析规则', '/analysis/rule', parent.id, 20, NOW()
FROM auth_menu parent
WHERE parent.path = '/analysis'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/analysis/rule');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '报告中心', '/report', 0, 50, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/report');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '系统管理', '/system', 0, 60, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/system');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '用户管理', '/system/user', parent.id, 10, NOW()
FROM auth_menu parent
WHERE parent.path = '/system'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/system/user');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '角色管理', '/system/role', parent.id, 20, NOW()
FROM auth_menu parent
WHERE parent.path = '/system'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/system/role');

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT '菜单管理', '/system/menu', parent.id, 30, NOW()
FROM auth_menu parent
WHERE parent.path = '/system'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/system/menu');

INSERT INTO auth_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM auth_role r
JOIN auth_permission p
WHERE r.code = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM auth_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO auth_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM auth_role r
JOIN auth_permission p ON p.code IN (
    'dashboard.view',
    'test.plan.read', 'test.plan.write',
    'test.scene.read', 'test.scene.write',
    'test.task.read', 'test.task.create', 'test.task.start', 'test.task.stop',
    'monitor.realtime.view', 'monitor.alert.read', 'monitor.alert.write',
    'analysis.report.view', 'analysis.rule.read', 'analysis.rule.write',
    'report.center.view', 'report.center.export'
)
WHERE r.code = 'PERF_ENGINEER'
  AND NOT EXISTS (
      SELECT 1
      FROM auth_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO auth_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM auth_role r
JOIN auth_permission p ON p.code IN (
    'dashboard.view',
    'monitor.realtime.view',
    'analysis.report.view',
    'report.center.view'
)
WHERE r.code = 'VIEWER'
  AND NOT EXISTS (
      SELECT 1
      FROM auth_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO auth_role_menu (role_id, menu_id, created_at)
SELECT r.id, m.id, NOW()
FROM auth_role r
JOIN auth_menu m
WHERE r.code = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM auth_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );

INSERT INTO auth_role_menu (role_id, menu_id, created_at)
SELECT r.id, m.id, NOW()
FROM auth_role r
JOIN auth_menu m ON m.path IN (
    '/dashboard',
    '/test', '/test/plan', '/test/scene', '/test/task/create', '/test/task',
    '/monitor', '/monitor/realtime', '/monitor/alert',
    '/analysis', '/analysis/report', '/analysis/rule',
    '/report'
)
WHERE r.code = 'PERF_ENGINEER'
  AND NOT EXISTS (
      SELECT 1
      FROM auth_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );

INSERT INTO auth_role_menu (role_id, menu_id, created_at)
SELECT r.id, m.id, NOW()
FROM auth_role r
JOIN auth_menu m ON m.path IN (
    '/dashboard',
    '/monitor', '/monitor/realtime',
    '/analysis', '/analysis/report',
    '/report'
)
WHERE r.code = 'VIEWER'
  AND NOT EXISTS (
      SELECT 1
      FROM auth_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );

INSERT INTO auth_user (username, password_hash, nickname, status, created_at, updated_at)
SELECT 'admin', '$2b$12$O/VRq6jfKCrKICYjJJ34UO9cWzIWE3gJw8SKQ7ky3JrAzauraAPPe', '系统管理员', 'ACTIVE', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM auth_user WHERE username = 'admin');

INSERT INTO auth_user_role (user_id, role_id, created_at)
SELECT u.id, r.id, NOW()
FROM auth_user u
JOIN auth_role r ON r.code = 'ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM auth_user_role ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

USE yuan_monitor;

INSERT INTO monitor_alert_rule (user_id, name, metric, op, threshold, level, enabled, created_at, updated_at)
SELECT u.id, 'P99 高延迟告警', 'P99', '>=', 2000, 'HIGH', 1, NOW(), NOW()
FROM yuan_auth.auth_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM monitor_alert_rule r
      WHERE r.user_id = u.id AND r.name = 'P99 高延迟告警'
  );

INSERT INTO monitor_alert_rule (user_id, name, metric, op, threshold, level, enabled, created_at, updated_at)
SELECT u.id, '错误率突增告警', 'ERROR_RATE', '>=', 5, 'CRITICAL', 1, NOW(), NOW()
FROM yuan_auth.auth_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM monitor_alert_rule r
      WHERE r.user_id = u.id AND r.name = '错误率突增告警'
  );

INSERT INTO monitor_alert_rule (user_id, name, metric, op, threshold, level, enabled, created_at, updated_at)
SELECT u.id, 'CPU 高压告警', 'CPU', '>=', 85, 'HIGH', 1, NOW(), NOW()
FROM yuan_auth.auth_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM monitor_alert_rule r
      WHERE r.user_id = u.id AND r.name = 'CPU 高压告警'
  );

INSERT INTO monitor_alert_rule (user_id, name, metric, op, threshold, level, enabled, created_at, updated_at)
SELECT u.id, '内存高压告警', 'MEMORY', '>=', 85, 'HIGH', 1, NOW(), NOW()
FROM yuan_auth.auth_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM monitor_alert_rule r
      WHERE r.user_id = u.id AND r.name = '内存高压告警'
  );

USE yuan_analysis;

INSERT INTO analysis_rule (user_id, rule_type, name, expression, instruction, bottleneck_type, severity, priority, enabled, created_at, updated_at)
SELECT u.id, 'ENGINE', '高延迟持续风险', 'summary.p99 > 2000 && feature.highLatencySeconds >= 3', NULL, 'HIGH_LATENCY', 'HIGH', 'P1', 1, NOW(), NOW()
FROM yuan_auth.auth_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM analysis_rule r
      WHERE r.user_id = u.id AND r.name = '高延迟持续风险'
  );

INSERT INTO analysis_rule (user_id, rule_type, name, expression, instruction, bottleneck_type, severity, priority, enabled, created_at, updated_at)
SELECT u.id, 'ENGINE', '错误率突增风险', 'summary.errorRate > 5 && feature.errorSpikeSeconds >= 3', NULL, 'ERROR_RATE', 'CRITICAL', 'P0', 1, NOW(), NOW()
FROM yuan_auth.auth_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM analysis_rule r
      WHERE r.user_id = u.id AND r.name = '错误率突增风险'
  );

INSERT INTO analysis_rule (user_id, rule_type, name, expression, instruction, bottleneck_type, severity, priority, enabled, created_at, updated_at)
SELECT u.id, 'ENGINE', 'CPU 压力趋势', 'summary.avgCpu > 80 || feature.peakCpu > 90', NULL, 'CPU_PRESSURE', 'HIGH', 'P1', 1, NOW(), NOW()
FROM yuan_auth.auth_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM analysis_rule r
      WHERE r.user_id = u.id AND r.name = 'CPU 压力趋势'
  );

INSERT INTO analysis_rule (user_id, rule_type, name, expression, instruction, bottleneck_type, severity, priority, enabled, created_at, updated_at)
SELECT u.id, 'ENGINE', '内存压力趋势', 'summary.avgMemory > 80 || feature.peakMemory > 90', NULL, 'MEMORY_PRESSURE', 'HIGH', 'P1', 1, NOW(), NOW()
FROM yuan_auth.auth_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM analysis_rule r
      WHERE r.user_id = u.id AND r.name = '内存压力趋势'
  );

INSERT INTO analysis_rule (user_id, rule_type, name, expression, instruction, bottleneck_type, severity, priority, enabled, created_at, updated_at)
SELECT u.id, 'AI', '下游依赖归因增强', NULL, '当延迟和错误率同时升高时，优先结合时序变化分析下游依赖、数据库连接池和线程池阻塞，并给出排查顺序。', 'ROOT_CAUSE', 'HIGH', 'P1', 1, NOW(), NOW()
FROM yuan_auth.auth_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM analysis_rule r
      WHERE r.user_id = u.id AND r.name = '下游依赖归因增强'
  );

INSERT INTO analysis_rule (user_id, rule_type, name, expression, instruction, bottleneck_type, severity, priority, enabled, created_at, updated_at)
SELECT u.id, 'AI', '容量与扩容建议增强', NULL, '结合吞吐、尾延迟和资源时序，输出更偏容量规划和下一轮压测设计的建议，而不是重复规则层结论。', 'CAPACITY', 'MEDIUM', 'P2', 1, NOW(), NOW()
FROM yuan_auth.auth_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM analysis_rule r
      WHERE r.user_id = u.id AND r.name = '容量与扩容建议增强'
  );
USE nacos_config;

INSERT INTO `users` (`username`, `password`, `enabled`)
SELECT 'nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM `users` WHERE `username` = 'nacos'
);

INSERT INTO `roles` (`username`, `role`)
SELECT 'nacos', 'ROLE_ADMIN'
WHERE NOT EXISTS (
    SELECT 1 FROM `roles` WHERE `username` = 'nacos' AND `role` = 'ROLE_ADMIN'
);

