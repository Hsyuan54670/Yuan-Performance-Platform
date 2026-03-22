#!/bin/bash
set -euo pipefail

upsert_user() {
    local username="$1"
    local password="$2"

    if rabbitmqctl list_users | awk '{print $1}' | grep -qx "$username"; then
        rabbitmqctl change_password "$username" "$password"
    else
        rabbitmqctl add_user "$username" "$password"
    fi
}

echo "等待 RabbitMQ 应用完全启动..."
rabbitmqctl await_startup
rabbitmq-diagnostics -q check_running

# 创建业务 vhost
rabbitmqctl add_vhost test_monitor 2>/dev/null || true

# 初始化管理员账号并授权
upsert_user admin "${RABBITMQ_ADMIN_PASSWORD}"
rabbitmqctl set_user_tags admin administrator
rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"
rabbitmqctl set_permissions -p test_monitor admin ".*" ".*" ".*"

# 初始化生产者账号并授权
upsert_user producer "${RABBITMQ_PRODUCER_PASSWORD}"
rabbitmqctl set_permissions -p test_monitor producer ".*" ".*" ".*"

# 初始化消费者账号并授权
upsert_user consumer "${RABBITMQ_CONSUMER_PASSWORD}"
rabbitmqctl set_permissions -p test_monitor consumer ".*" ".*" ".*"

echo "RabbitMQ 初始化完成"