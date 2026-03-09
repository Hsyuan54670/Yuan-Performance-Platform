-- =====================================================
-- Yuan Platform - M0 Init SQL (Split Databases)
-- Databases: yuan_auth, yuan_test, yuan_monitor, yuan_analysis, yuan_report
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `yuan_auth` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `yuan_test` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `yuan_monitor` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `yuan_analysis` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `yuan_report` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- =====================================================
-- 1) yuan_auth
-- =====================================================
USE `yuan_auth`;

DROP TABLE IF EXISTS auth_role_menu;
DROP TABLE IF EXISTS auth_menu;
DROP TABLE IF EXISTS auth_user_role;
DROP TABLE IF EXISTS auth_role;
DROP TABLE IF EXISTS auth_user;

CREATE TABLE auth_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_auth_user_username (username),
  KEY idx_auth_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE auth_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(64) NOT NULL,
  description VARCHAR(255) DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_auth_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE auth_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, role_id),
  KEY idx_auth_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE auth_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  path VARCHAR(128) NOT NULL,
  parent_id BIGINT NOT NULL DEFAULT 0,
  sort INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_auth_menu_parent_sort (parent_id, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE auth_role_menu (
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (role_id, menu_id),
  KEY idx_auth_role_menu_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO auth_role (id, code, name, description) VALUES
  (1, 'ADMIN', 'Administrator', 'Full system permission'),
  (2, 'DEVELOPER', 'Developer', 'Plan and run performance tests'),
  (3, 'TESTER', 'Tester', 'Manage tasks and reports');

INSERT INTO auth_user (id, username, password_hash, nickname, status) VALUES
  (1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Ops Lead', 'ACTIVE'),
  (2, 'dev_01', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Backend Dev', 'ACTIVE'),
  (3, 'qa_01', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'QA Owner', 'ACTIVE');

INSERT INTO auth_user_role (user_id, role_id) VALUES
  (1,1), (2,2), (3,3);

INSERT INTO auth_menu (id, name, path, parent_id, sort) VALUES
  (1, 'Dashboard', '/dashboard', 0, 1),
  (2, 'Test', '/test/plan', 0, 2),
  (3, 'Monitor', '/monitor/realtime', 0, 3),
  (4, 'Analysis', '/analysis/report', 0, 4),
  (5, 'Report', '/report', 0, 5),
  (6, 'System', '/system/user', 0, 6);

INSERT INTO auth_role_menu (role_id, menu_id)
SELECT 1, id FROM auth_menu;

-- =====================================================
-- 2) yuan_test
-- =====================================================
USE `yuan_test`;

DROP TABLE IF EXISTS test_metric_second;
DROP TABLE IF EXISTS test_task;
DROP TABLE IF EXISTS test_scene_step;
DROP TABLE IF EXISTS test_scene;
DROP TABLE IF EXISTS test_plan;

CREATE TABLE test_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  target_url VARCHAR(512) NOT NULL,
  concurrency INT NOT NULL,
  duration INT NOT NULL,
  ramp_type VARCHAR(16) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_test_plan_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE test_scene (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_test_scene_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE test_scene_step (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene_id BIGINT NOT NULL,
  step_order INT NOT NULL,
  name VARCHAR(128) NOT NULL,
  method VARCHAR(16) NOT NULL,
  path VARCHAR(256) NOT NULL,
  weight INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_test_scene_step_scene_order (scene_id, step_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE test_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  plan_id BIGINT,
  scene_id BIGINT,
  status VARCHAR(16) NOT NULL,
  start_time DATETIME,
  end_time DATETIME,
  duration INT NOT NULL DEFAULT 0,
  qps DECIMAL(10,2) NOT NULL DEFAULT 0,
  p99 DECIMAL(10,2) NOT NULL DEFAULT 0,
  error_rate DECIMAL(6,2) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_test_task_user_created (user_id, created_at),
  KEY idx_test_task_status (status),
  KEY idx_test_task_plan_scene (plan_id, scene_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE test_metric_second (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  ts DATETIME NOT NULL,
  qps DECIMAL(10,2) NOT NULL,
  p50 DECIMAL(10,2) NOT NULL,
  p90 DECIMAL(10,2) NOT NULL,
  p99 DECIMAL(10,2) NOT NULL,
  error_rate DECIMAL(6,2) NOT NULL,
  KEY idx_test_metric_second_task_ts (task_id, ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO test_plan (id, user_id, name, target_url, concurrency, duration, ramp_type, created_at) VALUES
  (101, 1, 'Checkout Peak Hour', 'https://api.demo.local/checkout', 200, 600, 'STAIR', '2026-03-07 09:00:00'),
  (102, 1, 'Search Latency Baseline', 'https://api.demo.local/search', 120, 480, 'LINEAR', '2026-03-06 14:30:00');

INSERT INTO test_scene (id, user_id, name, created_at) VALUES
  (201, 1, 'Purchase Flow', '2026-03-07 09:10:00'),
  (202, 1, 'Read-Heavy Search', '2026-03-06 14:40:00');

INSERT INTO test_scene_step (id, scene_id, step_order, name, method, path, weight) VALUES
  (1, 201, 1, 'Create Cart', 'POST', '/cart', 2),
  (2, 201, 2, 'Set Address', 'PUT', '/cart/address', 1),
  (3, 201, 3, 'Pay', 'POST', '/order/pay', 1),
  (4, 202, 1, 'Search List', 'GET', '/search', 5),
  (5, 202, 2, 'Detail', 'GET', '/product/detail', 2);

INSERT INTO test_task (id, user_id, plan_id, scene_id, status, start_time, end_time, duration, qps, p99, error_rate, created_at) VALUES
  (3001, 1, 101, 201, 'RUNNING', '2026-03-07 10:00:00', NULL, 600, 1320.00, 1420.00, 1.78, '2026-03-07 10:00:00'),
  (3000, 1, 102, 202, 'SUCCESS', '2026-03-06 18:00:00', '2026-03-06 18:08:00', 480, 910.00, 980.00, 0.35, '2026-03-06 18:00:00');

INSERT INTO test_metric_second (task_id, ts, qps, p50, p90, p99, error_rate) VALUES
  (3001, '2026-03-07 10:03:10', 1288.00, 120.00, 430.00, 1390.00, 1.50),
  (3001, '2026-03-07 10:03:11', 1312.00, 118.00, 440.00, 1420.00, 1.70),
  (3001, '2026-03-07 10:03:12', 1335.00, 122.00, 452.00, 1458.00, 1.90);

-- =====================================================
-- 3) yuan_monitor
-- =====================================================
USE `yuan_monitor`;

DROP TABLE IF EXISTS monitor_alert_record;
DROP TABLE IF EXISTS monitor_alert_rule;
DROP TABLE IF EXISTS monitor_sys_metric_second;

CREATE TABLE monitor_sys_metric_second (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  ts DATETIME NOT NULL,
  cpu DECIMAL(6,2) NOT NULL,
  memory DECIMAL(6,2) NOT NULL,
  disk DECIMAL(6,2) NOT NULL,
  network_in DECIMAL(10,2) NOT NULL,
  network_out DECIMAL(10,2) NOT NULL,
  KEY idx_monitor_sys_metric_task_ts (task_id, ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE monitor_alert_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  metric VARCHAR(32) NOT NULL,
  op VARCHAR(8) NOT NULL,
  threshold DECIMAL(10,2) NOT NULL,
  level VARCHAR(16) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_monitor_alert_rule_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE monitor_alert_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  rule_id BIGINT,
  rule_name VARCHAR(128) NOT NULL,
  level VARCHAR(16) NOT NULL,
  current_value DECIMAL(10,2) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_monitor_alert_record_task_created (task_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO monitor_sys_metric_second (task_id, ts, cpu, memory, disk, network_in, network_out) VALUES
  (3001, '2026-03-07 10:03:10', 72.80, 67.50, 58.90, 18.20, 14.60),
  (3001, '2026-03-07 10:03:11', 82.40, 69.10, 59.10, 19.00, 15.20),
  (3001, '2026-03-07 10:05:44', 79.80, 70.00, 59.80, 20.50, 16.10);

INSERT INTO monitor_alert_rule (id, user_id, name, metric, op, threshold, level, enabled) VALUES
  (401, 1, 'CPU High', 'CPU', '>=', 80.00, 'WARN', 1),
  (402, 1, 'Error Burst', 'ERROR_RATE', '>', 5.00, 'CRITICAL', 1),
  (403, 1, 'P99 Slow', 'P99', '>', 2000.00, 'WARN', 0);

INSERT INTO monitor_alert_record (id, task_id, rule_id, rule_name, level, current_value, created_at) VALUES
  (501, 3001, 401, 'CPU High', 'WARN', 82.40, '2026-03-07 10:03:11'),
  (502, 3001, 402, 'Error Burst', 'CRITICAL', 6.90, '2026-03-07 10:05:44');

-- =====================================================
-- 4) yuan_analysis
-- =====================================================
USE `yuan_analysis`;

DROP TABLE IF EXISTS analysis_suggestion;
DROP TABLE IF EXISTS analysis_bottleneck;
DROP TABLE IF EXISTS analysis_report;
DROP TABLE IF EXISTS analysis_rule;

CREATE TABLE analysis_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  expression TEXT NOT NULL,
  bottleneck_type VARCHAR(64) NOT NULL,
  severity VARCHAR(16) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_analysis_rule_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE analysis_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  grade CHAR(1) NOT NULL,
  score INT NOT NULL,
  summary TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_analysis_report_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE analysis_bottleneck (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  report_id BIGINT NOT NULL,
  time_point VARCHAR(32) NOT NULL,
  type VARCHAR(64) NOT NULL,
  reason TEXT NOT NULL,
  severity VARCHAR(16) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_analysis_bottleneck_report (report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE analysis_suggestion (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  report_id BIGINT NOT NULL,
  priority VARCHAR(8) NOT NULL,
  title VARCHAR(255) NOT NULL,
  detail TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_analysis_suggestion_report (report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO analysis_rule (id, user_id, name, expression, bottleneck_type, severity, enabled) VALUES
  (601, 1, 'CPU bottleneck', 'avg_cpu_usage > 80 and p99_response_time > 2000', 'CPU_BOTTLENECK', 'HIGH', 1),
  (602, 1, 'Connection pool', 'active_connections >= max_connections * 0.9 and error_rate > 5', 'CONNECTION_POOL_EXHAUSTION', 'HIGH', 1);

INSERT INTO analysis_report (id, task_id, grade, score, summary, created_at) VALUES
  (801, 3001, 'B', 83, 'Throughput is healthy but response-time tail and occasional error spikes need tuning.', '2026-03-07 10:10:00');

INSERT INTO analysis_bottleneck (report_id, time_point, type, reason, severity) VALUES
  (801, '10:03:15', 'CPU', 'CPU exceeded 82% while P99 climbed above 1500 ms.', 'HIGH'),
  (801, '10:05:40', 'CONNECTION_POOL', 'Connection pool saturation caused transient 5xx errors.', 'CRITICAL');

INSERT INTO analysis_suggestion (report_id, priority, title, detail) VALUES
  (801, 'P0', 'Increase DB connection pool with back-pressure', 'Raise max active connections and add queue timeout to avoid immediate failure under burst traffic.'),
  (801, 'P1', 'Optimize checkout hot query', 'Add composite index for order lookup and verify query plan to reduce P99 latency.'),
  (801, 'P2', 'Tune JVM GC and CPU limits', 'Adjust heap sizing and CPU quota to avoid contention during peak stage.');

-- =====================================================
-- 5) yuan_report
-- =====================================================
USE `yuan_report`;

DROP TABLE IF EXISTS report_record;

CREATE TABLE report_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  grade CHAR(1) NOT NULL,
  summary TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_report_record_task_created (task_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO report_record (id, task_id, title, grade, summary, created_at) VALUES
  (701, 3001, 'Checkout peak benchmark', 'B', 'Stable throughput with tail-latency risk.', '2026-03-07 10:12:00'),
  (702, 3000, 'Search baseline benchmark', 'A', 'Fast and consistent under configured load.', '2026-03-06 18:10:00');

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Split databases initialized: yuan_auth/yuan_test/yuan_monitor/yuan_analysis/yuan_report' AS message;
