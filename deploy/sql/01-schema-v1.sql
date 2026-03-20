USE yuan_auth;

CREATE TABLE IF NOT EXISTS auth_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_auth_user_username (username),
    KEY idx_auth_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_auth_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    module VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_auth_permission_code (code),
    KEY idx_auth_permission_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    path VARCHAR(128) NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    sort INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_auth_menu_path (path),
    KEY idx_auth_menu_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    KEY idx_auth_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    KEY idx_auth_role_permission_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, menu_id),
    KEY idx_auth_role_menu_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE yuan_test;

CREATE TABLE IF NOT EXISTS test_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    target_url VARCHAR(512) NOT NULL,
    concurrency INT NOT NULL,
    duration INT NOT NULL,
    ramp_type VARCHAR(32) NOT NULL DEFAULT 'CONSTANT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_test_plan_user_id (user_id),
    KEY idx_test_plan_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS test_scene (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_test_scene_user_id (user_id),
    KEY idx_test_scene_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS test_scene_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    name VARCHAR(128) NOT NULL,
    method VARCHAR(16) NOT NULL,
    path VARCHAR(512) NOT NULL,
    weight INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_test_scene_step_order (scene_id, step_order),
    KEY idx_test_scene_step_scene_id (scene_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS test_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    scene_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    start_time DATETIME NULL,
    end_time DATETIME NULL,
    duration INT NULL,
    qps DECIMAL(12, 2) NULL,
    p99 DECIMAL(12, 2) NULL,
    error_rate DECIMAL(12, 4) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_test_task_user_id (user_id),
    KEY idx_test_task_status (status),
    KEY idx_test_task_plan_id (plan_id),
    KEY idx_test_task_scene_id (scene_id),
    KEY idx_test_task_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS test_task_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    start_time DATETIME NULL,
    end_time DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_test_task_run_task_id (task_id),
    KEY idx_test_task_run_status (status),
    KEY idx_test_task_run_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS test_metric_second (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    run_id BIGINT NOT NULL,
    ts DATETIME NOT NULL,
    qps DECIMAL(12, 2) NULL,
    p50 DECIMAL(12, 2) NULL,
    p90 DECIMAL(12, 2) NULL,
    p99 DECIMAL(12, 2) NULL,
    error_rate DECIMAL(12, 4) NULL,
    KEY idx_test_metric_second_run_id_ts (run_id, ts),
    KEY idx_test_metric_second_task_id_ts (task_id, ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE yuan_monitor;

CREATE TABLE IF NOT EXISTS monitor_alert_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    metric VARCHAR(32) NOT NULL,
    op VARCHAR(8) NOT NULL,
    threshold DECIMAL(12, 4) NOT NULL,
    level VARCHAR(16) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_monitor_alert_rule_user_enabled (user_id, enabled),
    KEY idx_monitor_alert_rule_metric (metric)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monitor_alert_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    run_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    level VARCHAR(16) NOT NULL,
    event_type VARCHAR(16) NOT NULL,
    current_value DECIMAL(12, 4) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_monitor_alert_record_user_created (user_id, created_at),
    KEY idx_monitor_alert_record_run_created (run_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monitor_alert_state (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    run_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 0,
    latest_value DECIMAL(12, 4) NULL,
    latest_triggered_at DATETIME NULL,
    latest_recovered_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_monitor_alert_state_scope (user_id, task_id, run_id, rule_id),
    KEY idx_monitor_alert_state_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monitor_metric_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    run_id BIGINT NOT NULL,
    qps DECIMAL(12, 2) NULL,
    p50 DECIMAL(12, 2) NULL,
    p90 DECIMAL(12, 2) NULL,
    p99 DECIMAL(12, 2) NULL,
    error_rate DECIMAL(12, 4) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_monitor_metric_record_run_created (run_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monitor_sys_metric_second (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    run_id BIGINT NOT NULL,
    ts DATETIME NOT NULL,
    cpu DECIMAL(10, 2) NULL,
    cpu_max DECIMAL(10, 2) NULL,
    memory DECIMAL(10, 2) NULL,
    memory_max DECIMAL(10, 2) NULL,
    disk DECIMAL(10, 2) NULL,
    network_in DECIMAL(14, 4) NULL,
    network_out DECIMAL(14, 4) NULL,
    sample_count INT NULL,
    missing TINYINT(1) NOT NULL DEFAULT 0,
    KEY idx_monitor_sys_metric_second_run_ts (run_id, ts),
    KEY idx_monitor_sys_metric_second_task_ts (task_id, ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS test_status_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    run_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    message VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_test_status_record_user_run (user_id, run_id),
    KEY idx_test_status_record_task_created (task_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE yuan_analysis;

CREATE TABLE IF NOT EXISTS analysis_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    rule_type VARCHAR(16) NOT NULL,
    name VARCHAR(128) NOT NULL,
    expression TEXT NULL,
    instruction TEXT NULL,
    bottleneck_type VARCHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_analysis_rule_user_type_enabled (user_id, rule_type, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS analysis_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    run_id BIGINT NOT NULL,
    grade VARCHAR(8) NULL,
    score INT NULL,
    summary TEXT NULL,
    status VARCHAR(32) NULL,
    source VARCHAR(32) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_analysis_report_user_run (user_id, run_id),
    KEY idx_analysis_report_user_task_created (user_id, task_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS analysis_bottleneck (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_id BIGINT NOT NULL,
    time_point VARCHAR(64) NULL,
    type VARCHAR(64) NOT NULL,
    reason TEXT NULL,
    evidence TEXT NULL,
    severity VARCHAR(16) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_analysis_bottleneck_report_id (report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS analysis_suggestion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_id BIGINT NOT NULL,
    priority VARCHAR(16) NULL,
    title VARCHAR(128) NOT NULL,
    detail TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_analysis_suggestion_report_id (report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
