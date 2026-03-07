-- ============================================
-- AI 性能智能压测平台 - 一键初始化数据库脚本
-- 执行方式: mysql -u root -p < init_all.sql
-- ============================================

SET NAMES utf8mb4;

-- ----------------------------
-- 创建所有数据库
-- ----------------------------
CREATE DATABASE IF NOT EXISTS `yuan_auth` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `yuan_test` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `yuan_monitor` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `yuan_analysis` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `yuan_report` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- ----------------------------
-- yuan_auth 认证授权服务
-- ----------------------------
USE `yuan_auth`;

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

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

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

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 初始数据
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `sort`, `remark`) VALUES
(1, '超级管理员', 'admin', 1, '超级管理员角色'),
(2, '普通用户', 'user', 2, '普通用户角色');

-- 管理员账号 (密码: admin123)
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `status`) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', 1);

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `sort`, `path`, `component`, `menu_type`, `perms`, `icon`) VALUES
(1, '仪表盘', 0, 1, '/dashboard', 'Dashboard', 'C', '', 'DashboardOutlined'),
(100, '压测管理', 0, 2, '/test', '', 'M', '', 'ThunderboltOutlined'),
(101, '压测计划', 100, 1, '/test/plan', 'test/Plan', 'C', 'test:plan:list', ''),
(102, '场景管理', 100, 2, '/test/scene', 'test/Scene', 'C', 'test:scene:list', ''),
(103, '任务管理', 100, 3, '/test/task', 'test/Task', 'C', 'test:task:list', ''),
(200, '监控中心', 0, 3, '/monitor', '', 'M', '', 'MonitorOutlined'),
(201, '实时监控', 200, 1, '/monitor/realtime', 'monitor/Realtime', 'C', 'monitor:realtime', ''),
(202, '告警管理', 200, 2, '/monitor/alert', 'monitor/Alert', 'C', 'monitor:alert:list', ''),
(300, '智能分析', 0, 4, '/analysis', '', 'M', '', 'RobotOutlined'),
(301, '分析报告', 300, 1, '/analysis/report', 'analysis/Report', 'C', 'analysis:report:list', ''),
(302, '分析规则', 300, 2, '/analysis/rule', 'analysis/Rule', 'C', 'analysis:rule:list', ''),
(400, '报告中心', 0, 5, '/report', '', 'M', '', 'FileTextOutlined'),
(401, '报告列表', 400, 1, '/report/list', 'report/List', 'C', 'report:list', ''),
(402, '对比分析', 400, 2, '/report/compare', 'report/Compare', 'C', 'report:compare', ''),
(500, '系统管理', 0, 6, '/system', '', 'M', '', 'SettingOutlined'),
(501, '用户管理', 500, 1, '/system/user', 'system/User', 'C', 'system:user:list', ''),
(502, '角色管理', 500, 2, '/system/role', 'system/Role', 'C', 'system:role:list', ''),
(503, '菜单管理', 500, 3, '/system/menu', 'system/Menu', 'C', 'system:menu:list', '');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 1), (1, 100), (1, 101), (1, 102), (1, 103),
(1, 200), (1, 201), (1, 202),
(1, 300), (1, 301), (1, 302),
(1, 400), (1, 401), (1, 402),
(1, 500), (1, 501), (1, 502), (1, 503),
(2, 1), (2, 100), (2, 101), (2, 102), (2, 103),
(2, 200), (2, 201),
(2, 300), (2, 301),
(2, 400), (2, 401), (2, 402);

-- ----------------------------
-- yuan_test 压测服务
-- ----------------------------
USE `yuan_test`;

DROP TABLE IF EXISTS `test_plan`;
CREATE TABLE `test_plan` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '计划ID',
    `plan_name` VARCHAR(128) NOT NULL COMMENT '计划名称',
    `plan_desc` VARCHAR(512) DEFAULT '' COMMENT '计划描述',
    `target_url` VARCHAR(512) NOT NULL COMMENT '目标URL',
    `protocol` VARCHAR(16) DEFAULT 'HTTP' COMMENT '协议(HTTP/HTTPS)',
    `method` VARCHAR(16) DEFAULT 'GET' COMMENT '请求方法',
    `thread_count` INT DEFAULT 10 COMMENT '并发线程数',
    `ramp_up_seconds` INT DEFAULT 10 COMMENT '预热时间(秒)',
    `duration_seconds` INT DEFAULT 60 COMMENT '持续时间(秒)',
    `loop_count` INT DEFAULT 1 COMMENT '循环次数(-1为永远)',
    `think_time` INT DEFAULT 0 COMMENT '思考时间(毫秒)',
    `status` TINYINT DEFAULT 0 COMMENT '状态(0草稿 1已发布 2已归档)',
    `user_id` BIGINT NOT NULL COMMENT '创建用户ID',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='压测计划表';

DROP TABLE IF EXISTS `test_plan_api`;
CREATE TABLE `test_plan_api` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '接口ID',
    `plan_id` BIGINT NOT NULL COMMENT '计划ID',
    `api_name` VARCHAR(128) NOT NULL COMMENT '接口名称',
    `api_path` VARCHAR(256) NOT NULL COMMENT '接口路径',
    `method` VARCHAR(16) DEFAULT 'GET' COMMENT '请求方法',
    `headers` TEXT DEFAULT NULL COMMENT '请求头(JSON格式)',
    `body_type` VARCHAR(16) DEFAULT '' COMMENT '请求体类型(none/json/form)',
    `body_content` TEXT DEFAULT NULL COMMENT '请求体内容',
    `extract_vars` TEXT DEFAULT NULL COMMENT '提取变量(JSON格式)',
    `assertions` TEXT DEFAULT NULL COMMENT '断言配置(JSON格式)',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
    PRIMARY KEY (`id`),
    KEY `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='压测计划接口配置表';

DROP TABLE IF EXISTS `test_scene`;
CREATE TABLE `test_scene` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '场景ID',
    `scene_name` VARCHAR(128) NOT NULL COMMENT '场景名称',
    `scene_desc` VARCHAR(512) DEFAULT '' COMMENT '场景描述',
    `plan_id` BIGINT DEFAULT NULL COMMENT '关联计划ID(可选)',
    `thread_count` INT DEFAULT 10 COMMENT '并发数',
    `ramp_up_seconds` INT DEFAULT 10 COMMENT '预热时间',
    `duration_seconds` INT DEFAULT 60 COMMENT '持续时间',
    `think_time` INT DEFAULT 1000 COMMENT '步骤间思考时间(毫秒)',
    `status` TINYINT DEFAULT 0 COMMENT '状态(0禁用 1启用)',
    `user_id` BIGINT NOT NULL COMMENT '创建用户ID',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='压测场景表';

DROP TABLE IF EXISTS `test_scene_step`;
CREATE TABLE `test_scene_step` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '步骤ID',
    `scene_id` BIGINT NOT NULL COMMENT '场景ID',
    `step_name` VARCHAR(128) NOT NULL COMMENT '步骤名称',
    `step_order` INT DEFAULT 0 COMMENT '步骤顺序',
    `request_type` VARCHAR(16) DEFAULT 'HTTP' COMMENT '请求类型(HTTP/DUBBO/SQL)',
    `url` VARCHAR(512) NOT NULL COMMENT '请求URL',
    `method` VARCHAR(16) DEFAULT 'GET' COMMENT '请求方法',
    `headers` TEXT DEFAULT NULL COMMENT '请求头',
    `body_type` VARCHAR(16) DEFAULT '' COMMENT '请求体类型',
    `body_content` TEXT DEFAULT NULL COMMENT '请求体内容',
    `extract_vars` TEXT DEFAULT NULL COMMENT '提取变量',
    `assertions` TEXT DEFAULT NULL COMMENT '断言配置',
    `think_time` INT DEFAULT 0 COMMENT '思考时间(毫秒)',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用(0禁用 1启用)',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
    PRIMARY KEY (`id`),
    KEY `idx_scene_id` (`scene_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='压测场景步骤表';

DROP TABLE IF EXISTS `test_task`;
CREATE TABLE `test_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `task_name` VARCHAR(128) NOT NULL COMMENT '任务名称',
    `task_type` VARCHAR(16) DEFAULT 'SIMPLE' COMMENT '任务类型(SIMPLE简单/SCENE场景/STEP阶梯)',
    `scene_id` BIGINT DEFAULT NULL COMMENT '关联场景ID',
    `plan_id` BIGINT DEFAULT NULL COMMENT '关联计划ID',
    `target_url` VARCHAR(512) NOT NULL COMMENT '目标URL',
    `thread_count` INT DEFAULT 10 COMMENT '并发数',
    `ramp_up_seconds` INT DEFAULT 10 COMMENT '预热时间',
    `duration_seconds` INT DEFAULT 60 COMMENT '持续时间',
    `step_load` VARCHAR(512) DEFAULT NULL COMMENT '阶梯加压配置(JSON)',
    `status` VARCHAR(16) DEFAULT 'PENDING' COMMENT '状态(PENDING/READY/RUNNING/STOPPED/COMPLETED/FAILED)',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `total_requests` BIGINT DEFAULT 0 COMMENT '总请求数',
    `error_count` BIGINT DEFAULT 0 COMMENT '错误数',
    `avg_rt` DECIMAL(10,2) DEFAULT 0 COMMENT '平均响应时间(ms)',
    `p50_rt` DECIMAL(10,2) DEFAULT 0 COMMENT 'P50响应时间(ms)',
    `p90_rt` DECIMAL(10,2) DEFAULT 0 COMMENT 'P90响应时间(ms)',
    `p99_rt` DECIMAL(10,2) DEFAULT 0 COMMENT 'P99响应时间(ms)',
    `max_rt` DECIMAL(10,2) DEFAULT 0 COMMENT '最大响应时间(ms)',
    `qps` DECIMAL(10,2) DEFAULT 0 COMMENT '每秒请求数',
    `error_rate` DECIMAL(5,2) DEFAULT 0 COMMENT '错误率(%)',
    `user_id` BIGINT NOT NULL COMMENT '创建用户ID',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='压测任务表';

DROP TABLE IF EXISTS `test_task_result`;
CREATE TABLE `test_task_result` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '结果ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `timestamp` DATETIME NOT NULL COMMENT '采样时间',
    `thread_count` INT DEFAULT 0 COMMENT '活跃线程数',
    `sample_count` INT DEFAULT 0 COMMENT '采样数',
    `error_count` INT DEFAULT 0 COMMENT '错误数',
    `sent_bytes` BIGINT DEFAULT 0 COMMENT '发送字节数',
    `received_bytes` BIGINT DEFAULT 0 COMMENT '接收字节数',
    `min_rt` DECIMAL(10,2) DEFAULT 0 COMMENT '最小响应时间',
    `max_rt` DECIMAL(10,2) DEFAULT 0 COMMENT '最大响应时间',
    `avg_rt` DECIMAL(10,2) DEFAULT 0 COMMENT '平均响应时间',
    `p50_rt` DECIMAL(10,2) DEFAULT 0 COMMENT 'P50响应时间',
    `p90_rt` DECIMAL(10,2) DEFAULT 0 COMMENT 'P90响应时间',
    `p95_rt` DECIMAL(10,2) DEFAULT 0 COMMENT 'P95响应时间',
    `p99_rt` DECIMAL(10,2) DEFAULT 0 COMMENT 'P99响应时间',
    `qps` DECIMAL(10,2) DEFAULT 0 COMMENT 'QPS',
    `throughput` DECIMAL(10,2) DEFAULT 0 COMMENT '吞吐量(MB/s)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='压测任务结果详情表';

DROP TABLE IF EXISTS `test_task_status_code`;
CREATE TABLE `test_task_status_code` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `status_code` VARCHAR(16) NOT NULL COMMENT 'HTTP状态码',
    `count` INT DEFAULT 0 COMMENT '出现次数',
    `avg_rt` DECIMAL(10,2) DEFAULT 0 COMMENT '平均响应时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='压测任务HTTP状态码统计表';

-- ----------------------------
-- yuan_monitor 监控服务
-- ----------------------------
USE `yuan_monitor`;

DROP TABLE IF EXISTS `monitor_metrics`;
CREATE TABLE `monitor_metrics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `timestamp` DATETIME NOT NULL COMMENT '采样时间',
    `qps` DECIMAL(10,2) DEFAULT 0 COMMENT '每秒请求数',
    `avg_rt` DECIMAL(10,2) DEFAULT 0 COMMENT '平均响应时间(ms)',
    `min_rt` DECIMAL(10,2) DEFAULT 0 COMMENT '最小响应时间(ms)',
    `max_rt` DECIMAL(10,2) DEFAULT 0 COMMENT '最大响应时间(ms)',
    `p50_rt` DECIMAL(10,2) DEFAULT 0 COMMENT 'P50响应时间(ms)',
    `p90_rt` DECIMAL(10,2) DEFAULT 0 COMMENT 'P90响应时间(ms)',
    `p95_rt` DECIMAL(10,2) DEFAULT 0 COMMENT 'P95响应时间(ms)',
    `p99_rt` DECIMAL(10,2) DEFAULT 0 COMMENT 'P99响应时间(ms)',
    `error_count` INT DEFAULT 0 COMMENT '错误数',
    `total_count` INT DEFAULT 0 COMMENT '总请求数',
    `error_rate` DECIMAL(5,2) DEFAULT 0 COMMENT '错误率(%)',
    `sent_bytes` BIGINT DEFAULT 0 COMMENT '发送字节数',
    `received_bytes` BIGINT DEFAULT 0 COMMENT '接收字节数',
    `throughput` DECIMAL(10,2) DEFAULT 0 COMMENT '吞吐量(MB/s)',
    `active_threads` INT DEFAULT 0 COMMENT '活跃线程数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_id_timestamp` (`task_id`, `timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='压测性能指标表';

DROP TABLE IF EXISTS `monitor_sys_metrics`;
CREATE TABLE `monitor_sys_metrics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `task_id` BIGINT DEFAULT NULL COMMENT '任务ID(可为空,表示独立监控)',
    `timestamp` DATETIME NOT NULL COMMENT '采样时间',
    `hostname` VARCHAR(128) DEFAULT '' COMMENT '主机名',
    `cpu_usage` DECIMAL(5,2) DEFAULT 0 COMMENT 'CPU使用率(%)',
    `cpu_load` DECIMAL(5,2) DEFAULT 0 COMMENT '系统负载',
    `memory_usage` DECIMAL(5,2) DEFAULT 0 COMMENT '内存使用率(%)',
    `memory_used` BIGINT DEFAULT 0 COMMENT '已用内存(bytes)',
    `memory_total` BIGINT DEFAULT 0 COMMENT '总内存(bytes)',
    `disk_usage` DECIMAL(5,2) DEFAULT 0 COMMENT '磁盘使用率(%)',
    `disk_read_bytes` BIGINT DEFAULT 0 COMMENT '磁盘读取速度(bytes/s)',
    `disk_write_bytes` BIGINT DEFAULT 0 COMMENT '磁盘写入速度(bytes/s)',
    `network_rx_bytes` BIGINT DEFAULT 0 COMMENT '网络接收速度(bytes/s)',
    `network_tx_bytes` BIGINT DEFAULT 0 COMMENT '网络发送速度(bytes/s)',
    `tcp_connections` INT DEFAULT 0 COMMENT 'TCP连接数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_id_timestamp` (`task_id`, `timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统资源指标表';

DROP TABLE IF EXISTS `monitor_alert_rule`;
CREATE TABLE `monitor_alert_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
    `rule_type` VARCHAR(32) NOT NULL COMMENT '规则类型(CPU/MEMORY/DISK/NETWORK/RESPONSE_TIME/ERROR_RATE/QPS)',
    `metric_name` VARCHAR(64) NOT NULL COMMENT '指标名称',
    `condition` VARCHAR(32) NOT NULL COMMENT '条件(GT/GTE/LT/LTE/EQ/NE)',
    `threshold` DECIMAL(10,2) NOT NULL COMMENT '阈值',
    `duration_seconds` INT DEFAULT 60 COMMENT '持续时间(秒)',
    `level` VARCHAR(16) DEFAULT 'WARNING' COMMENT '告警级别(INFO/WARNING/CRITICAL)',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用(0禁用 1启用)',
    `notify_type` VARCHAR(32) DEFAULT 'WEB' COMMENT '通知方式(WEB/EMAIL/SMS)',
    `notify_users` VARCHAR(512) DEFAULT '' COMMENT '通知用户(逗号分隔)',
    `remark` VARCHAR(512) DEFAULT '' COMMENT '备注',
    `user_id` BIGINT NOT NULL COMMENT '创建用户ID',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表';

DROP TABLE IF EXISTS `monitor_alert_record`;
CREATE TABLE `monitor_alert_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `rule_id` BIGINT NOT NULL COMMENT '规则ID',
    `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
    `alert_type` VARCHAR(32) NOT NULL COMMENT '告警类型',
    `metric_name` VARCHAR(64) NOT NULL COMMENT '指标名称',
    `metric_value` DECIMAL(10,2) NOT NULL COMMENT '指标值',
    `threshold` DECIMAL(10,2) NOT NULL COMMENT '阈值',
    `condition` VARCHAR(32) NOT NULL COMMENT '条件',
    `level` VARCHAR(16) DEFAULT 'WARNING' COMMENT '告警级别',
    `message` TEXT DEFAULT NULL COMMENT '告警消息',
    `task_id` BIGINT DEFAULT NULL COMMENT '关联任务ID',
    `status` VARCHAR(16) DEFAULT 'PENDING' COMMENT '状态(PENDING/SOLVED/IGNORED)',
    `solve_time` DATETIME DEFAULT NULL COMMENT '解决时间',
    `solve_user_id` BIGINT DEFAULT NULL COMMENT '解决用户ID',
    `solve_remark` VARCHAR(512) DEFAULT '' COMMENT '解决备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

INSERT INTO `monitor_alert_rule` (`rule_name`, `rule_type`, `metric_name`, `condition`, `threshold`, `duration_seconds`, `level`, `notify_type`, `remark`, `user_id`) VALUES
('CPU使用率过高', 'CPU', 'cpu_usage', 'GT', 80, 60, 'WARNING', 'WEB', 'CPU使用率超过80%告警', 1),
('CPU使用率严重过高', 'CPU', 'cpu_usage', 'GT', 95, 30, 'CRITICAL', 'WEB', 'CPU使用率超过95%告警', 1),
('内存使用率过高', 'MEMORY', 'memory_usage', 'GT', 85, 120, 'WARNING', 'WEB', '内存使用率超过85%告警', 1),
('内存使用率严重过高', 'MEMORY', 'memory_usage', 'GT', 95, 30, 'CRITICAL', 'WEB', '内存使用率超过95%告警', 1),
('响应时间过高', 'RESPONSE_TIME', 'p99_rt', 'GT', 5000, 60, 'WARNING', 'WEB', 'P99响应时间超过5秒告警', 1),
('错误率过高', 'ERROR_RATE', 'error_rate', 'GT', 5, 30, 'WARNING', 'WEB', '错误率超过5%告警', 1),
('错误率严重过高', 'ERROR_RATE', 'error_rate', 'GT', 20, 10, 'CRITICAL', 'WEB', '错误率超过20%告警', 1);

-- ----------------------------
-- yuan_analysis AI分析服务
-- ----------------------------
USE `yuan_analysis`;

DROP TABLE IF EXISTS `analysis_report`;
CREATE TABLE `analysis_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '报告ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `report_name` VARCHAR(128) NOT NULL COMMENT '报告名称',
    `performance_rating` CHAR(1) DEFAULT 'C' COMMENT '性能评级(A/B/C/D/E)',
    `summary` TEXT DEFAULT NULL COMMENT '分析摘要',
    `target_url` VARCHAR(512) DEFAULT '' COMMENT '目标URL',
    `total_requests` BIGINT DEFAULT 0 COMMENT '总请求数',
    `avg_qps` DECIMAL(10,2) DEFAULT 0 COMMENT '平均QPS',
    `max_qps` DECIMAL(10,2) DEFAULT 0 COMMENT '最大QPS',
    `avg_rt` DECIMAL(10,2) DEFAULT 0 COMMENT '平均响应时间(ms)',
    `p50_rt` DECIMAL(10,2) DEFAULT 0 COMMENT 'P50响应时间(ms)',
    `p90_rt` DECIMAL(10,2) DEFAULT 0 COMMENT 'P90响应时间(ms)',
    `p99_rt` DECIMAL(10,2) DEFAULT 0 COMMENT 'P99响应时间(ms)',
    `max_rt` DECIMAL(10,2) DEFAULT 0 COMMENT '最大响应时间(ms)',
    `error_rate` DECIMAL(5,2) DEFAULT 0 COMMENT '错误率(%)',
    `throughput` DECIMAL(10,2) DEFAULT 0 COMMENT '吞吐量(MB/s)',
    `avg_cpu` DECIMAL(5,2) DEFAULT 0 COMMENT '平均CPU使用率(%)',
    `max_cpu` DECIMAL(5,2) DEFAULT 0 COMMENT '最大CPU使用率(%)',
    `avg_memory` DECIMAL(5,2) DEFAULT 0 COMMENT '平均内存使用率(%)',
    `max_memory` DECIMAL(5,2) DEFAULT 0 COMMENT '最大内存使用率(%)',
    `llm_full_response` TEXT DEFAULT NULL COMMENT 'LLM完整响应',
    `status` VARCHAR(16) DEFAULT 'PENDING' COMMENT '状态(PENDING/ANALYZING/COMPLETED/FAILED)',
    `analyze_duration` INT DEFAULT 0 COMMENT '分析耗时(秒)',
    `user_id` BIGINT NOT NULL COMMENT '创建用户ID',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析报告表';

DROP TABLE IF EXISTS `analysis_bottleneck`;
CREATE TABLE `analysis_bottleneck` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '瓶颈ID',
    `report_id` BIGINT NOT NULL COMMENT '报告ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `bottleneck_type` VARCHAR(64) NOT NULL COMMENT '瓶颈类型',
    `bottleneck_name` VARCHAR(128) NOT NULL COMMENT '瓶颈名称',
    `severity` VARCHAR(16) DEFAULT 'MEDIUM' COMMENT '严重程度(LOW/MEDIUM/HIGH/CRITICAL)',
    `description` TEXT DEFAULT NULL COMMENT '描述',
    `evidence` TEXT DEFAULT NULL COMMENT '证据(指标数据)',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `correlation` DECIMAL(5,2) DEFAULT 0 COMMENT '与性能下降相关性(0-1)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_report_id` (`report_id`),
    KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='瓶颈记录表';

DROP TABLE IF EXISTS `analysis_rule`;
CREATE TABLE `analysis_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
    `rule_type` VARCHAR(64) NOT NULL COMMENT '规则类型',
    `bottleneck_type` VARCHAR(64) NOT NULL COMMENT '关联瓶颈类型',
    `conditions` TEXT NOT NULL COMMENT '条件配置(JSON)',
    `severity` VARCHAR(16) DEFAULT 'MEDIUM' COMMENT '默认严重程度',
    `description` TEXT DEFAULT NULL COMMENT '规则描述',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用(0禁用 1启用)',
    `priority` INT DEFAULT 100 COMMENT '优先级(越小越先)',
    `user_id` BIGINT NOT NULL COMMENT '创建用户ID',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析规则表';

DROP TABLE IF EXISTS `analysis_suggestion`;
CREATE TABLE `analysis_suggestion` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '建议ID',
    `report_id` BIGINT NOT NULL COMMENT '报告ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `bottleneck_id` BIGINT DEFAULT NULL COMMENT '关联瓶颈ID',
    `suggestion_type` VARCHAR(64) NOT NULL COMMENT '建议类型',
    `title` VARCHAR(256) NOT NULL COMMENT '建议标题',
    `content` TEXT NOT NULL COMMENT '建议内容',
    `priority` INT DEFAULT 0 COMMENT '优先级(1-5,1最高)',
    `impact` VARCHAR(16) DEFAULT 'MEDIUM' COMMENT '影响程度(LOW/MEDIUM/HIGH)',
    `effort` VARCHAR(16) DEFAULT 'MEDIUM' COMMENT '投入成本(LOW/MEDIUM/HIGH)',
    `status` VARCHAR(16) DEFAULT 'PENDING' COMMENT '状态(PENDING/IMPLEMENTED/IGNORED)',
    `implement_remark` VARCHAR(512) DEFAULT '' COMMENT '实施备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_report_id` (`report_id`),
    KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优化建议表';

INSERT INTO `analysis_rule` (`rule_name`, `rule_type`, `bottleneck_type`, `conditions`, `severity`, `description`, `priority`, `user_id`) VALUES
('CPU瓶颈-持续高负载', 'RESOURCE', 'CPU_BOTTLENECK', '{"conditions": [{"metric": "avg_cpu", "operator": "GT", "value": 80}, {"metric": "p99_rt", "operator": "GT", "value": 2000, "relation": "AND"}]}', 'HIGH', 'CPU使用率持续超过80%且响应时间明显上升', 10, 1),
('CPU瓶颈-瞬时峰值', 'RESOURCE', 'CPU_BOTTLENECK', '{"conditions": [{"metric": "max_cpu", "operator": "GT", "value": 95}]}', 'MEDIUM', 'CPU瞬时峰值超过95%', 20, 1),
('内存瓶颈-高使用率', 'RESOURCE', 'MEMORY_BOTTLENECK', '{"conditions": [{"metric": "avg_memory", "operator": "GT", "value": 85}]}', 'HIGH', '内存使用率持续超过85%', 15, 1),
('内存瓶颈-持续增长', 'RESOURCE', 'MEMORY_LEAK', '{"conditions": [{"metric": "memory_trend", "operator": "EQ", "value": "increasing"}]}', 'CRITICAL', '内存使用量持续增长疑似内存泄漏', 5, 1),
('数据库瓶颈-慢查询', 'DATABASE', 'DATABASE_BOTTLENECK', '{"conditions": [{"metric": "slow_query_count", "operator": "GT", "value": 10}, {"metric": "qps_trend", "operator": "EQ", "value": "decreasing"}]}', 'HIGH', '慢查询增多且QPS下降', 12, 1),
('连接池瓶颈', 'DATABASE', 'CONNECTION_POOL_EXHAUSTION', '{"conditions": [{"metric": "active_connections", "operator": "GTE", "value": 0.9, "reference": "max_connections"}]}', 'HIGH', '数据库连接池使用率超过90%', 8, 1),
('网络瓶颈-带宽打满', 'NETWORK', 'NETWORK_BOTTLENECK', '{"conditions": [{"metric": "network_usage", "operator": "GT", "value": 90}]}', 'MEDIUM', '网络带宽使用率超过90%', 25, 1),
('响应时间瓶颈-稳定高延迟', 'PERFORMANCE', 'RESPONSE_TIME_HIGH', '{"conditions": [{"metric": "avg_rt", "operator": "GT", "value": 3000}]}', 'MEDIUM', '平均响应时间超过3秒', 30, 1),
('响应时间瓶颈-P99恶化', 'PERFORMANCE', 'RESPONSE_TIME_P99_DEGRADATION', '{"conditions": [{"metric": "p99_p50_ratio", "operator": "GT", "value": 10}]}', 'HIGH', 'P99与P50比值超过10倍,长尾严重', 18, 1),
('错误率瓶颈', 'ERROR', 'ERROR_RATE_HIGH', '{"conditions": [{"metric": "error_rate", "operator": "GT", "value": 5}]}', 'HIGH', '错误率超过5%', 22, 1);

-- ----------------------------
-- yuan_report 报告服务
-- ----------------------------
USE `yuan_report`;

DROP TABLE IF EXISTS `report_record`;
CREATE TABLE `report_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '报告ID',
    `report_name` VARCHAR(128) NOT NULL COMMENT '报告名称',
    `report_type` VARCHAR(32) NOT NULL COMMENT '报告类型(SINGLE单次/COMPARE对比/TREND趋势)',
    `task_ids` VARCHAR(512) DEFAULT '' COMMENT '关联任务ID(逗号分隔)',
    `analysis_report_ids` VARCHAR(512) DEFAULT '' COMMENT '关联分析报告ID(逗号分隔)',
    `content` LONGTEXT DEFAULT NULL COMMENT '报告内容(JSON)',
    `summary` TEXT DEFAULT NULL COMMENT '报告摘要',
    `file_url` VARCHAR(512) DEFAULT '' COMMENT '导出文件URL',
    `file_type` VARCHAR(16) DEFAULT 'HTML' COMMENT '文件类型(HTML/PDF)',
    `status` VARCHAR(16) DEFAULT 'DRAFT' COMMENT '状态(DRAFT/GENERATING/COMPLETED/FAILED)',
    `user_id` BIGINT NOT NULL COMMENT '创建用户ID',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告记录表';

DROP TABLE IF EXISTS `report_template`;
CREATE TABLE `report_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_name` VARCHAR(128) NOT NULL COMMENT '模板名称',
    `template_type` VARCHAR(32) NOT NULL COMMENT '模板类型',
    `template_content` LONGTEXT NOT NULL COMMENT '模板内容(JSON/HTML)',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否为默认模板(0否 1是)',
    `remark` VARCHAR(512) DEFAULT '' COMMENT '备注',
    `user_id` BIGINT NOT NULL COMMENT '创建用户ID',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告模板表';

DROP TABLE IF EXISTS `report_compare`;
CREATE TABLE `report_compare` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '对比ID',
    `compare_name` VARCHAR(128) NOT NULL COMMENT '对比名称',
    `task_ids` VARCHAR(256) NOT NULL COMMENT '对比的任务ID(逗号分隔)',
    `metrics_comparison` LONGTEXT DEFAULT NULL COMMENT '指标对比(JSON)',
    `trend_analysis` LONGTEXT DEFAULT NULL COMMENT '趋势分析',
    `conclusion` TEXT DEFAULT NULL COMMENT '对比结论',
    `user_id` BIGINT NOT NULL COMMENT '创建用户ID',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标志(0正常 1删除)',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告对比记录表';

INSERT INTO `report_template` (`template_name`, `template_type`, `template_content`, `is_default`, `remark`, `user_id`) VALUES
('标准压测报告', 'SINGLE', '{"sections": [{"title": "概述", "fields": ["target_url", "test_duration", "concurrency"]}, {"title": "性能指标", "fields": ["qps", "avg_rt", "p50_rt", "p90_rt", "p99_rt", "error_rate"]}, {"title": "系统资源", "fields": ["cpu_usage", "memory_usage", "disk_io", "network_io"]}, {"title": "AI分析", "fields": ["performance_rating", "bottlenecks", "suggestions"]}]}', 1, '默认压测报告模板', 1),
('对比分析报告', 'COMPARE', '{"sections": [{"title": "对比概览", "fields": ["task_names", "test_time"]}, {"title": "指标对比", "fields": ["qps_compare", "rt_compare", "error_rate_compare"]}, {"title": "趋势分析", "fields": ["trend_chart", "conclusion"]}]}', 1, '默认对比报告模板', 1);

-- ============================================
-- 初始化完成
-- ============================================
SELECT '数据库初始化完成!' AS message;
