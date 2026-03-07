-- ============================================
-- yuan_monitor 监控服务数据库
-- ============================================

CREATE DATABASE IF NOT EXISTS `yuan_monitor` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `yuan_monitor`;

-- ----------------------------
-- 压测指标数据表 (时序数据)
-- ----------------------------
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

-- ----------------------------
-- 系统资源指标表
-- ----------------------------
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

-- ----------------------------
-- 告警规则表
-- ----------------------------
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

-- ----------------------------
-- 告警记录表
-- ----------------------------
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

-- ----------------------------
-- 初始告警规则数据
-- ----------------------------
INSERT INTO `monitor_alert_rule` (`rule_name`, `rule_type`, `metric_name`, `condition`, `threshold`, `duration_seconds`, `level`, `notify_type`, `remark`, `user_id`) VALUES
('CPU使用率过高', 'CPU', 'cpu_usage', 'GT', 80, 60, 'WARNING', 'WEB', 'CPU使用率超过80%告警', 1),
('CPU使用率严重过高', 'CPU', 'cpu_usage', 'GT', 95, 30, 'CRITICAL', 'WEB', 'CPU使用率超过95%告警', 1),
('内存使用率过高', 'MEMORY', 'memory_usage', 'GT', 85, 120, 'WARNING', 'WEB', '内存使用率超过85%告警', 1),
('内存使用率严重过高', 'MEMORY', 'memory_usage', 'GT', 95, 30, 'CRITICAL', 'WEB', '内存使用率超过95%告警', 1),
('响应时间过高', 'RESPONSE_TIME', 'p99_rt', 'GT', 5000, 60, 'WARNING', 'WEB', 'P99响应时间超过5秒告警', 1),
('错误率过高', 'ERROR_RATE', 'error_rate', 'GT', 5, 30, 'WARNING', 'WEB', '错误率超过5%告警', 1),
('错误率严重过高', 'ERROR_RATE', 'error_rate', 'GT', 20, 10, 'CRITICAL', 'WEB', '错误率超过20%告警', 1);
