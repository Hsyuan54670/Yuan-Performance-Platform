# Database Init

这个目录提供 V1 首次部署所需的数据库脚本。

## 文件顺序

1. `00-create-databases.sql`
2. `01-nacos-schema.sql`
3. `01-schema-v1.sql`
4. `02-seed-v1.sql`

## 如果你使用统一 docker compose 首次初始化 MySQL

这些脚本会在 MySQL 数据目录为空时自动执行，无需手动再跑一次。

## 如果你已经启动过 MySQL

MySQL 官方镜像只会在 **第一次初始化数据目录** 时执行 `/docker-entrypoint-initdb.d` 里的脚本。

也就是说，如果你的 `yuan_mysql_data` 已经创建过，而当时还没有 `01-nacos-schema.sql` 或 `02-seed-v1.sql` 里的 Nacos 账号种子，那么这次新增脚本 **不会自动补跑**。

这时你需要手动执行：

```sql
USE nacos_config;
SOURCE /path/to/01-nacos-schema.sql;
SOURCE /path/to/02-seed-v1.sql;
```

对于 Nacos 3.x，除了 `config_info / config_info_gray / his_config_info / tenant_info / tenant_capacity / group_capacity / config_tags_relation`，还需要 `config_info_beta / config_info_tag / migrate_config` 这些表；如果缺失，启动阶段可能报：

- `[migrate] config_info namespace migrate pre check failed`

另外如果 Nacos 开启鉴权，而 `users / roles` 表里没有默认账号，客户端会报：

- `User not found! Please check user exist or password is right!`

## 初始化后会得到什么

- 4 个业务库
- 1 个 `nacos_config` 配置库
- Nacos 3.1.0 所需核心表结构
- Nacos 默认管理员账号 `nacos / nacos`
- 全量业务核心表结构
- 默认管理员账号 `admin / admin123`
- 默认 RBAC 角色 / 权限 / 菜单
- 默认告警规则
- 默认分析规则

## 不会预置什么

- 压测计划
- 场景
- 任务
- 运行记录
- 报告记录

这部分保持空白，更接近真实用户首次拿到系统时的状态。
