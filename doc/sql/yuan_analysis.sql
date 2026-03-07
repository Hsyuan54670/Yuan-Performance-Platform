-- ============================================
-- yuan_analysis AI分析服务数据库
-- ============================================

CREATE DATABASE IF NOT EXISTS `yuan_analysis` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `yuan_analysis`;

-- ----------------------------
-- 分析报告表
-- ----------------------------
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

-- ----------------------------
-- 瓶颈记录表
-- ----------------------------
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

-- ----------------------------
-- 分析规则表
-- ----------------------------
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

-- ----------------------------
-- 优化建议表
-- ----------------------------
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

-- ----------------------------
-- 初始分析规则数据
-- ----------------------------
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
