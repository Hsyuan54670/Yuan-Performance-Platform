-- ============================================
-- yuan_report 报告服务数据库
-- ============================================

CREATE DATABASE IF NOT EXISTS `yuan_report` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `yuan_report`;

-- ----------------------------
-- 报告记录表
-- ----------------------------
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

-- ----------------------------
-- 报告模板表
-- ----------------------------
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

-- ----------------------------
-- 报告对比记录表
-- ----------------------------
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

-- ----------------------------
-- 初始报告模板数据
-- ----------------------------
INSERT INTO `report_template` (`template_name`, `template_type`, `template_content`, `is_default`, `remark`, `user_id`) VALUES
('标准压测报告', 'SINGLE', '{"sections": [{"title": "概述", "fields": ["target_url", "test_duration", "concurrency"]}, {"title": "性能指标", "fields": ["qps", "avg_rt", "p50_rt", "p90_rt", "p99_rt", "error_rate"]}, {"title": "系统资源", "fields": ["cpu_usage", "memory_usage", "disk_io", "network_io"]}, {"title": "AI分析", "fields": ["performance_rating", "bottlenecks", "suggestions"]}]}', 1, '默认压测报告模板', 1),
('对比分析报告', 'COMPARE', '{"sections": [{"title": "对比概览", "fields": ["task_names", "test_time"]}, {"title": "指标对比", "fields": ["qps_compare", "rt_compare", "error_rate_compare"]}, {"title": "趋势分析", "fields": ["trend_chart", "conclusion"]}]}', 1, '默认对比报告模板', 1);
