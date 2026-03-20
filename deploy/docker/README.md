# Docker Infra

这个目录只负责基础设施，不直接启动业务服务。

## 包含的依赖

- MySQL 8
- Redis 7
- RabbitMQ 3 Management
- Nacos 2（standalone）

## 启动命令

```powershell
docker compose -f deploy/docker/docker-compose.infra.yml up -d
```

## 端口约定

- MySQL：`3306`
- Redis：`6379`
- RabbitMQ：`5672`
- RabbitMQ 管理台：`15672`
- Nacos：`8848`
- Nacos gRPC：`9848`

## 首次启动效果

- MySQL 会自动执行 `deploy/sql/00-create-databases.sql`
- MySQL 会自动执行 `deploy/sql/01-schema-v1.sql`
- MySQL 会自动执行 `deploy/sql/02-seed-v1.sql`
- RabbitMQ 会自动创建：
  - 管理员：`admin / 123456`
  - vhost：`test_monitor`
  - 用户：`producer / 123456`
  - 用户：`consumer / 123456`

## 注意

如果 MySQL 数据卷已经存在，初始化 SQL 不会再次自动执行。此时如果你想重置到首发状态，需要先删除 MySQL volume 再重新启动。
