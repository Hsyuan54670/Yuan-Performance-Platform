-- ============================================
-- yuan_test 压测服务数据库
-- ============================================

CREATE DATABASE IF NOT EXISTS `yuan_test` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `yuan_test`;

-- ----------------------------
-- 压测计划表
-- ----------------------------
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

-- ----------------------------
-- 压测计划接口配置表
-- ----------------------------
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

-- ----------------------------
-- 压测场景表
-- ----------------------------
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

-- ----------------------------
-- 压测场景步骤表
-- ----------------------------
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

-- ----------------------------
-- 压测任务表
-- ----------------------------
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
    `deleted` TINYINT DEFAULT 0(0正常  COMMENT '删除标志1删除)',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='压测任务表';

-- ----------------------------
-- 压测任务结果详情表
-- ----------------------------
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

-- ----------------------------
-- 压测任务HTTP状态码统计表
-- ----------------------------
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
