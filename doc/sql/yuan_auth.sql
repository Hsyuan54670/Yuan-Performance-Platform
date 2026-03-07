-- ============================================
-- yuan_auth 认证授权服务数据库
-- ============================================

CREATE DATABASE IF NOT EXISTS `yuan_auth` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `yuan_auth`;

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `password` VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
    `nickname` VARCHAR(64) DEFAULT '' COMMENT '用户昵称',
    `email` VARCHAR(128) DEFAULT '' COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT '' COMMENT '手机号',
    `avatar` VARCHAR(512) DEFAULT '' COMMENT '头像URL',
    `sex` TINYINT DEFAULT 0 COMMENT '性别（0未知 1男 2女）',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0禁用 1正常）',
    `login_ip` VARCHAR(128) DEFAULT '' COMMENT '最后登录IP',
    `login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `remark` VARCHAR(512) DEFAULT '' COMMENT '备注',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 角色表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name` VARCHAR(64) NOT NULL COMMENT '角色名称',
    `role_key` VARCHAR(64) NOT NULL COMMENT '角色标识',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0禁用 1正常）',
    `remark` VARCHAR(512) DEFAULT '' COMMENT '备注',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ----------------------------
-- 用户角色关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ----------------------------
-- 菜单权限表
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `menu_name` VARCHAR(64) NOT NULL COMMENT '菜单名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID（0为顶级）',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `path` VARCHAR(256) DEFAULT '' COMMENT '路由地址',
    `component` VARCHAR(256) DEFAULT '' COMMENT '组件路径',
    `menu_type` CHAR(1) DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
    `perms` VARCHAR(128) DEFAULT '' COMMENT '权限标识',
    `icon` VARCHAR(128) DEFAULT '' COMMENT '菜单图标',
    `visible` TINYINT DEFAULT 1 COMMENT '是否可见（0隐藏 1显示）',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0禁用 1正常）',
    `remark` VARCHAR(512) DEFAULT '' COMMENT '备注',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- ----------------------------
-- 角色菜单关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ----------------------------
-- 初始数据
-- ----------------------------

-- 管理员角色
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `sort`, `remark`) VALUES
(1, '超级管理员', 'admin', 1, '超级管理员角色'),
(2, '普通用户', 'user', 2, '普通用户角色');

-- 管理员账号 (密码: admin123, BCrypt加密)
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `status`) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', 1);

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 菜单数据
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `sort`, `path`, `component`, `menu_type`, `perms`, `icon`) VALUES
-- 顶级菜单
(1,   '仪表盘',   0, 1, '/dashboard',       'Dashboard',              'C', '',                    'DashboardOutlined'),
(100, '压测管理', 0, 2, '/test',             '',                        'M', '',                    'ThunderboltOutlined'),
(200, '监控中心', 0, 3, '/monitor',          '',                        'M', '',                    'MonitorOutlined'),
(300, '智能分析', 0, 4, '/analysis',         '',                        'M', '',                    'RobotOutlined'),
(400, '报告中心', 0, 5, '/report',           '',                        'M', '',                    'FileTextOutlined'),
(500, '系统管理', 0, 6, '/system',           '',                        'M', '',                    'SettingOutlined'),
-- 压测管理子菜单
(101, '压测计划', 100, 1, '/test/plan',      'test/Plan',               'C', 'test:plan:list',      ''),
(102, '场景管理', 100, 2, '/test/scene',     'test/Scene',              'C', 'test:scene:list',     ''),
(103, '任务管理', 100, 3, '/test/task',      'test/Task',               'C', 'test:task:list',      ''),
-- 监控中心子菜单
(201, '实时监控', 200, 1, '/monitor/realtime','monitor/Realtime',       'C', 'monitor:realtime',    ''),
(202, '告警管理', 200, 2, '/monitor/alert',   'monitor/Alert',          'C', 'monitor:alert:list',  ''),
-- 智能分析子菜单
(301, '分析报告', 300, 1, '/analysis/report', 'analysis/Report',        'C', 'analysis:report:list',''),
(302, '分析规则', 300, 2, '/analysis/rule',   'analysis/Rule',          'C', 'analysis:rule:list',  ''),
-- 报告中心子菜单
(401, '报告列表', 400, 1, '/report/list',     'report/List',            'C', 'report:list',         ''),
(402, '对比分析', 400, 2, '/report/compare',  'report/Compare',         'C', 'report:compare',      ''),
-- 系统管理子菜单
(501, '用户管理', 500, 1, '/system/user',     'system/User',            'C', 'system:user:list',    ''),
(502, '角色管理', 500, 2, '/system/role',     'system/Role',            'C', 'system:role:list',    ''),
(503, '菜单管理', 500, 3, '/system/menu',     'system/Menu',            'C', 'system:menu:list',    '');

-- 为管理员角色分配所有菜单
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 1), (1, 100), (1, 101), (1, 102), (1, 103),
(1, 200), (1, 201), (1, 202),
(1, 300), (1, 301), (1, 302),
(1, 400), (1, 401), (1, 402),
(1, 500), (1, 501), (1, 502), (1, 503);

-- 为普通用户分配部分菜单
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2, 1), (2, 100), (2, 101), (2, 102), (2, 103),
(2, 200), (2, 201),
(2, 300), (2, 301),
(2, 400), (2, 401), (2, 402);
