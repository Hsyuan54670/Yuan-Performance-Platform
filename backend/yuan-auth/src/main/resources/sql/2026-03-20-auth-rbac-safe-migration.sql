-- Safe incremental RBAC migration for existing yuan_auth database
-- Generated on 2026-03-20
-- Target database: MySQL 8+
-- Purpose:
-- 1. Keep existing auth_user / auth_role / auth_menu / auth_user_role / auth_role_menu data intact
-- 2. Add auth_permission / auth_role_permission if missing
-- 3. Seed default permission atoms, role templates, and menu templates without overwriting existing ids
-- 4. Avoid fixed primary-key inserts so it can be applied to an existing database more safely

SET NAMES utf8mb4;

-- ========================================================
-- 1. Create only the missing RBAC tables required by new code
-- ========================================================

CREATE TABLE IF NOT EXISTS auth_permission (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(128) NOT NULL,
    name        VARCHAR(128) NOT NULL,
    module      VARCHAR(64)  NOT NULL,
    description VARCHAR(255) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_auth_permission_code (code),
    KEY idx_auth_permission_module (module)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_role_permission (
    role_id       BIGINT   NOT NULL,
    permission_id BIGINT   NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    KEY idx_auth_role_permission_permission_id (permission_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- If relationship tables do not exist in this environment yet, create them.
CREATE TABLE IF NOT EXISTS auth_user_role (
    user_id    BIGINT   NOT NULL,
    role_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    KEY idx_auth_user_role_role_id (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_role_menu (
    role_id    BIGINT   NOT NULL,
    menu_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, menu_id),
    KEY idx_auth_role_menu_menu_id (menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ========================================================
-- 2. Seed permission atoms used by current backend/frontend code
-- Safe to rerun because code is unique
-- ========================================================

INSERT INTO auth_permission (code, name, module, description)
VALUES
    ('dashboard.view', '查看仪表盘', 'dashboard', '允许查看首页仪表盘与运行概览'),

    ('test.plan.read', '查看压测计划', 'test', '允许查看压测计划列表与详情'),
    ('test.plan.write', '管理压测计划', 'test', '允许新增、编辑、删除压测计划'),
    ('test.scene.read', '查看场景编排', 'test', '允许查看压测场景与步骤'),
    ('test.scene.write', '管理场景编排', 'test', '允许新增、编辑、删除场景'),
    ('test.task.read', '查看任务控制台', 'test', '允许查看任务列表、运行与指标'),
    ('test.task.create', '创建压测任务', 'test', '允许选择计划和场景创建任务'),
    ('test.task.start', '启动压测任务', 'test', '允许启动压测任务'),
    ('test.task.stop', '停止压测任务', 'test', '允许停止运行中的压测任务'),

    ('monitor.realtime.view', '查看实时监控', 'monitor', '允许查看实时性能与资源指标'),
    ('monitor.alert.read', '查看告警规则', 'monitor', '允许查看告警规则与告警记录'),
    ('monitor.alert.write', '管理告警规则', 'monitor', '允许新增、编辑、删除告警规则'),

    ('analysis.report.view', '查看分析报告', 'analysis', '允许查看分析报告页与分析详情'),
    ('analysis.rule.read', '查看分析规则', 'analysis', '允许查看分析规则'),
    ('analysis.rule.write', '管理分析规则', 'analysis', '允许新增、编辑、删除分析规则'),

    ('report.center.view', '查看报告中心', 'report', '允许查看报告中心列表与详情'),
    ('report.center.export', '导出报告', 'report', '允许导出 HTML 等报告文件'),

    ('system.user.read', '查看用户管理', 'system', '允许查看用户列表'),
    ('system.user.write', '管理用户状态', 'system', '允许启用、停用用户与调整用户角色'),
    ('system.role.read', '查看角色管理', 'system', '允许查看角色列表'),
    ('system.role.write', '管理角色', 'system', '允许新增、编辑、删除角色'),
    ('system.menu.read', '查看菜单管理', 'system', '允许查看菜单树'),
    ('system.menu.write', '管理菜单', 'system', '允许调整菜单可见性与排序')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    module = VALUES(module),
    description = VALUES(description);

-- ========================================================
-- 3. Seed default roles only if they do not already exist by code
-- No fixed id usage here, so existing role ids stay untouched
-- ========================================================

INSERT INTO auth_role (code, name, description, created_at)
SELECT 'ADMIN', '管理员', '平台全局管理与治理', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM auth_role WHERE code = 'ADMIN'
);

INSERT INTO auth_role (code, name, description, created_at)
SELECT 'PERF_ENGINEER', '压测工程师', '压测计划、场景、任务与分析主操作者', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM auth_role WHERE code = 'PERF_ENGINEER'
);

INSERT INTO auth_role (code, name, description, created_at)
SELECT 'VIEWER', '只读观察员', '仅查看仪表盘、报告与分析结果', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM auth_role WHERE code = 'VIEWER'
);

-- ========================================================
-- 4. Seed menu tree only when matching path is absent
-- This avoids overwriting existing menu ids or names
-- Assumption: auth_menu already contains columns name/path/parent_id/sort/created_at
-- ========================================================

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'Dashboard', '/dashboard', 0, 10, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM auth_menu WHERE path = '/dashboard'
);

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'Test', '/test', 0, 20, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM auth_menu WHERE path = '/test'
);

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'Test Plan', '/test/plan', id, 10, NOW()
FROM auth_menu
WHERE path = '/test'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/test/plan')
LIMIT 1;

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'Test Scene', '/test/scene', id, 20, NOW()
FROM auth_menu
WHERE path = '/test'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/test/scene')
LIMIT 1;

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'Create Task', '/test/task/create', id, 30, NOW()
FROM auth_menu
WHERE path = '/test'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/test/task/create')
LIMIT 1;

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'Task Console', '/test/task', id, 40, NOW()
FROM auth_menu
WHERE path = '/test'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/test/task')
LIMIT 1;

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'Monitor', '/monitor', 0, 30, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM auth_menu WHERE path = '/monitor'
);

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'Realtime Monitor', '/monitor/realtime', id, 10, NOW()
FROM auth_menu
WHERE path = '/monitor'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/monitor/realtime')
LIMIT 1;

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'Alert Rules', '/monitor/alert', id, 20, NOW()
FROM auth_menu
WHERE path = '/monitor'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/monitor/alert')
LIMIT 1;

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'Analysis', '/analysis', 0, 40, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM auth_menu WHERE path = '/analysis'
);

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'Analysis Report', '/analysis/report', id, 10, NOW()
FROM auth_menu
WHERE path = '/analysis'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/analysis/report')
LIMIT 1;

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'Analysis Rule', '/analysis/rule', id, 20, NOW()
FROM auth_menu
WHERE path = '/analysis'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/analysis/rule')
LIMIT 1;

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'Report Center', '/report', 0, 50, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM auth_menu WHERE path = '/report'
);

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'System', '/system', 0, 60, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM auth_menu WHERE path = '/system'
);

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'User Management', '/system/user', id, 10, NOW()
FROM auth_menu
WHERE path = '/system'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/system/user')
LIMIT 1;

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'Role Management', '/system/role', id, 20, NOW()
FROM auth_menu
WHERE path = '/system'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/system/role')
LIMIT 1;

INSERT INTO auth_menu (name, path, parent_id, sort, created_at)
SELECT 'Menu Management', '/system/menu', id, 30, NOW()
FROM auth_menu
WHERE path = '/system'
  AND NOT EXISTS (SELECT 1 FROM auth_menu WHERE path = '/system/menu')
LIMIT 1;

-- ========================================================
-- 5. Seed template role-permission bindings by role code
-- This will not disturb any existing custom bindings
-- ========================================================

INSERT INTO auth_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM auth_role r
JOIN auth_permission p
WHERE r.code = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM auth_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

INSERT INTO auth_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM auth_role r
JOIN auth_permission p
WHERE r.code = 'PERF_ENGINEER'
  AND p.code IN (
      'dashboard.view',
      'test.plan.read', 'test.plan.write',
      'test.scene.read', 'test.scene.write',
      'test.task.read', 'test.task.create', 'test.task.start', 'test.task.stop',
      'monitor.realtime.view',
      'monitor.alert.read', 'monitor.alert.write',
      'analysis.report.view',
      'analysis.rule.read', 'analysis.rule.write',
      'report.center.view', 'report.center.export'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM auth_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

INSERT INTO auth_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM auth_role r
JOIN auth_permission p
WHERE r.code = 'VIEWER'
  AND p.code IN (
      'dashboard.view',
      'monitor.realtime.view',
      'analysis.report.view',
      'report.center.view'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM auth_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- ========================================================
-- 6. Seed template role-menu bindings by role code and menu path
-- This is frontend visibility only and will not remove existing bindings
-- ========================================================

INSERT INTO auth_role_menu (role_id, menu_id, created_at)
SELECT r.id, m.id, NOW()
FROM auth_role r
JOIN auth_menu m
WHERE r.code = 'ADMIN'
  AND m.path IN (
      '/dashboard',
      '/test', '/test/plan', '/test/scene', '/test/task/create', '/test/task',
      '/monitor', '/monitor/realtime', '/monitor/alert',
      '/analysis', '/analysis/report', '/analysis/rule',
      '/report',
      '/system', '/system/user', '/system/role', '/system/menu'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM auth_role_menu rm
      WHERE rm.role_id = r.id
        AND rm.menu_id = m.id
  );

INSERT INTO auth_role_menu (role_id, menu_id, created_at)
SELECT r.id, m.id, NOW()
FROM auth_role r
JOIN auth_menu m
WHERE r.code = 'PERF_ENGINEER'
  AND m.path IN (
      '/dashboard',
      '/test', '/test/plan', '/test/scene', '/test/task/create', '/test/task',
      '/monitor', '/monitor/realtime', '/monitor/alert',
      '/analysis', '/analysis/report', '/analysis/rule',
      '/report'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM auth_role_menu rm
      WHERE rm.role_id = r.id
        AND rm.menu_id = m.id
  );

INSERT INTO auth_role_menu (role_id, menu_id, created_at)
SELECT r.id, m.id, NOW()
FROM auth_role r
JOIN auth_menu m
WHERE r.code = 'VIEWER'
  AND m.path IN (
      '/dashboard',
      '/monitor', '/monitor/realtime',
      '/analysis', '/analysis/report',
      '/report'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM auth_role_menu rm
      WHERE rm.role_id = r.id
        AND rm.menu_id = m.id
  );

-- ========================================================
-- 7. Optional bootstrap example for assigning an existing admin user
-- Uncomment and replace the username if needed
-- ========================================================

-- INSERT INTO auth_user_role (user_id, role_id, created_at)
-- SELECT u.id, r.id, NOW()
-- FROM auth_user u
-- JOIN auth_role r ON r.code = 'ADMIN'
-- WHERE u.username = 'admin'
--   AND NOT EXISTS (
--       SELECT 1
--       FROM auth_user_role ur
--       WHERE ur.user_id = u.id
--         AND ur.role_id = r.id
--   );
