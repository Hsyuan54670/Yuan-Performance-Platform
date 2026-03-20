-- RBAC schema draft for yuan-auth
-- Generated on 2026-03-20
-- Target database: MySQL 8+
-- Goal:
-- 1. Keep existing auth_user / auth_role / auth_menu / auth_user_role / auth_role_menu naming
-- 2. Add auth_permission / auth_role_permission to support permission-based authorization
-- 3. Allow admin-defined roles while keeping permission atoms fixed by the system

SET NAMES utf8mb4;

-- =========================
-- 1. Core identity tables
-- =========================

CREATE TABLE IF NOT EXISTS auth_user (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname      VARCHAR(64)  NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_auth_user_username (username),
    KEY idx_auth_user_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    description VARCHAR(255) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_auth_role_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

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

CREATE TABLE IF NOT EXISTS auth_menu (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    name       VARCHAR(64)  NOT NULL,
    path       VARCHAR(128) NOT NULL,
    parent_id  BIGINT       NOT NULL DEFAULT 0,
    sort       INT          NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_auth_menu_parent_sort (parent_id, sort),
    KEY idx_auth_menu_path (path)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =========================
-- 2. Relationship tables
-- =========================

CREATE TABLE IF NOT EXISTS auth_user_role (
    user_id    BIGINT   NOT NULL,
    role_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    KEY idx_auth_user_role_role_id (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_role_permission (
    role_id       BIGINT   NOT NULL,
    permission_id BIGINT   NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    KEY idx_auth_role_permission_permission_id (permission_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_role_menu (
    role_id    BIGINT   NOT NULL,
    menu_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, menu_id),
    KEY idx_auth_role_menu_menu_id (menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =========================
-- 3. Suggested foreign keys
-- Commented out by default so that this draft can be applied incrementally
-- Uncomment after confirming existing production data is clean
-- =========================

-- ALTER TABLE auth_user_role
--     ADD CONSTRAINT fk_auth_user_role_user FOREIGN KEY (user_id) REFERENCES auth_user (id),
--     ADD CONSTRAINT fk_auth_user_role_role FOREIGN KEY (role_id) REFERENCES auth_role (id);
--
-- ALTER TABLE auth_role_permission
--     ADD CONSTRAINT fk_auth_role_permission_role FOREIGN KEY (role_id) REFERENCES auth_role (id),
--     ADD CONSTRAINT fk_auth_role_permission_permission FOREIGN KEY (permission_id) REFERENCES auth_permission (id);
--
-- ALTER TABLE auth_role_menu
--     ADD CONSTRAINT fk_auth_role_menu_role FOREIGN KEY (role_id) REFERENCES auth_role (id),
--     ADD CONSTRAINT fk_auth_role_menu_menu FOREIGN KEY (menu_id) REFERENCES auth_menu (id);

-- =========================
-- 4. Permission atoms for current yuan project
-- These codes are fixed by the system; roles can be customized by admin later
-- =========================

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

-- =========================
-- 5. Suggested default menu seed for current frontend routes
-- Menus remain configurable, but these seeds match the current route tree
-- =========================

INSERT INTO auth_menu (id, name, path, parent_id, sort)
VALUES
    (1, 'Dashboard', '/dashboard', 0, 10),

    (2, 'Test', '/test', 0, 20),
    (21, 'Test Plan', '/test/plan', 2, 10),
    (22, 'Test Scene', '/test/scene', 2, 20),
    (23, 'Create Task', '/test/task/create', 2, 30),
    (24, 'Task Console', '/test/task', 2, 40),

    (3, 'Monitor', '/monitor', 0, 30),
    (31, 'Realtime Monitor', '/monitor/realtime', 3, 10),
    (32, 'Alert Rules', '/monitor/alert', 3, 20),

    (4, 'Analysis', '/analysis', 0, 40),
    (41, 'Analysis Report', '/analysis/report', 4, 10),
    (42, 'Analysis Rule', '/analysis/rule', 4, 20),

    (5, 'Report Center', '/report', 0, 50),

    (6, 'System', '/system', 0, 60),
    (61, 'User Management', '/system/user', 6, 10),
    (62, 'Role Management', '/system/role', 6, 20),
    (63, 'Menu Management', '/system/menu', 6, 30)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    path = VALUES(path),
    parent_id = VALUES(parent_id),
    sort = VALUES(sort);

-- =========================
-- 6. Suggested default roles as templates only
-- Admin can customize or replace them later
-- =========================

INSERT INTO auth_role (id, code, name, description)
VALUES
    (1, 'ADMIN', '管理员', '平台全局管理与治理'),
    (2, 'PERF_ENGINEER', '压测工程师', '压测计划、场景、任务与分析主操作者'),
    (3, 'VIEWER', '只读观察员', '仅查看仪表盘、报告与分析结果')
ON DUPLICATE KEY UPDATE
    code = VALUES(code),
    name = VALUES(name),
    description = VALUES(description);

-- =========================
-- 7. Suggested template bindings for default roles
-- Role names are templates only; permission atoms remain fixed by the system
-- =========================

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 1, p.id FROM auth_permission p
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 2, p.id
FROM auth_permission p
WHERE p.code IN (
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
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 3, p.id
FROM auth_permission p
WHERE p.code IN (
    'dashboard.view',
    'monitor.realtime.view',
    'analysis.report.view',
    'report.center.view'
)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

INSERT INTO auth_role_menu (role_id, menu_id)
VALUES
    (1, 1), (1, 2), (1, 21), (1, 22), (1, 23), (1, 24),
    (1, 3), (1, 31), (1, 32),
    (1, 4), (1, 41), (1, 42),
    (1, 5),
    (1, 6), (1, 61), (1, 62), (1, 63),

    (2, 1), (2, 2), (2, 21), (2, 22), (2, 23), (2, 24),
    (2, 3), (2, 31), (2, 32),
    (2, 4), (2, 41), (2, 42),
    (2, 5),

    (3, 1),
    (3, 3), (3, 31),
    (3, 4), (3, 41),
    (3, 5)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

-- =========================
-- 8. Recommended next-step APIs
-- GET  /auth/me                    -> return user + roles + permissions + menus
-- GET  /auth/permissions           -> list all permission atoms
-- GET  /auth/roles/{id}/permissions
-- PUT  /auth/roles/{id}/permissions
-- GET  /auth/roles/{id}/menus
-- PUT  /auth/roles/{id}/menus
-- GET  /auth/users/{id}/roles
-- PUT  /auth/users/{id}/roles
-- =========================
