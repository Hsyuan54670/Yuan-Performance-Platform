# YUAN Deploy Guide

这个目录用于还原 `YUAN 性能压测与智能分析平台` 的完整部署形态，包括基础设施、后端运行态、前端静态资源和初始化脚本。

## 目录说明

- `docker-compose.yml`：统一部署入口。
- `.env.example`：部署环境变量示例，复制为 `deploy/.env` 后再填写真实值。
- `sql/`：建库、Nacos 库表、业务表和 V1 种子数据。
- `nacos/`：`dev / test / prod` 三套配置模板。
- `nginx/`：前端反向代理配置。
- `rabbitmq/`：RabbitMQ 初始化脚本。
- `web/`：已构建好的前端静态资源。
- `server/`：后端 JAR 与应用级环境配置。
- `scripts/`：Nacos 配置导入等辅助脚本。
- `jmeter/`：JMeter 接入说明与目录约定。

## 推荐启动顺序

1. 复制 [.env.example](.env.example) 为 `deploy/.env`。
2. 准备 `server/` 下 6 个后端 JAR。
3. 进入 `deploy` 目录启动统一环境：

```bash
cd deploy
docker compose up -d
```

4. Nacos 健康后导入配置：

```bash
sh scripts/import-nacos-config.sh --env prod --server-addr 127.0.0.1:8848 --username nacos --password nacos
```

Windows 下可用：

```powershell
pwsh -File scripts/import-nacos-config.ps1 -Env prod -ServerAddr 127.0.0.1:8848 -Username nacos -Password nacos
```

## 启动前要确认的关键项

- `JMETER_HOME / JMETER_RESULTS_DIR / JMETER_SCRIPTS_DIR` 必须是宿主机真实路径。
- `ARK_API_KEY` 为空时也能启动，默认建议先走 `fallback`。
- 若 MySQL 数据卷已初始化过，新增 SQL 不会自动重跑，需要手动导入或重建数据卷。

## 默认访问入口

- 前端：`http://<server-ip>/`
- Gateway：`http://<server-ip>:8080`
- Nacos：`http://<server-ip>:8848/nacos`
- RabbitMQ 管理台：`http://<server-ip>:15672`

## 默认账号

- 平台管理员：`admin / admin123`
- Nacos 管理员：`nacos / nacos`

## 关联文档

- [sql/README.md](sql/README.md)
- [server/README.md](server/README.md)
- [scripts/README.md](scripts/README.md)
- [nginx/README.md](nginx/README.md)
- [jmeter/README.md](jmeter/README.md)

