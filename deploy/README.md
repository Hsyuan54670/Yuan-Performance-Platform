# YUAN V1 Deploy Guide

这个目录用于还原 `YUAN 性能压测与智能分析平台` 第一版交付时的真实部署形态。

## 目录说明

- `docker/`：基础设施拆分 compose 与说明。
- `sql/`：数据库初始化脚本，包含建库、Nacos 库表、业务表和 V1 必要种子数据。
- `nacos/`：按 `dev / test / prod` 划分的 Nacos 配置模板。
- `nginx/`：前端反向代理配置，统一把 `/api` 和 `/ws` 转给 gateway。
- `rabbitmq/`：RabbitMQ 初始化脚本，负责创建 vhost、管理员和生产/消费账号。
- `web/`：前端已构建静态资源，统一 compose 会直接挂载到 nginx。
- `jmeter/`：JMeter 目录约定与接入说明。
- `server/`：后端 JAR 运行态部署目录，包含环境配置和 JVM 镜像模板。
- `scripts/`：辅助脚本，例如统一启动、停止和导入 Nacos 配置。
- `docker-compose.yml`：统一部署入口，合并了基础设施、后端服务和前端服务。
- `.env`：统一 compose 默认环境变量，进入 `deploy` 后会被 `docker compose` 自动读取。

## 一条命令启动

进入 `deploy` 目录后，默认启动命令就是：

```powershell
docker compose up -d
```

如果你希望首次构建镜像时强制重建，可以用：

```powershell
docker compose up -d --build
```

## 启动前你只需要确认 3 件事

1. `server/` 下 6 个后端 JAR 已经准备好。
2. `.env` 中的 JMeter 路径已经改成目标服务器真实宿主机路径：
   - `JMETER_HOME`
   - `JMETER_RESULTS_DIR`
   - `JMETER_SCRIPTS_DIR`
3. 如需真实 AI，补上 `.env` 里的 `ARK_API_KEY`。

## 当前默认访问入口

- 前端：`http://<server-ip>/`
- Gateway：`http://<server-ip>:8080`
- Nacos：`http://<server-ip>:8848/nacos`
- RabbitMQ 管理台：`http://<server-ip>:15672`

## 默认系统状态

这是当前 V1 最接近真实交付的初始化状态：

- 已创建 4 个业务库：`yuan_auth / yuan_test / yuan_monitor / yuan_analysis`
- 已创建 `nacos_config` 配置库和 Nacos 3.1.0 所需核心表结构
- 已创建系统所需核心业务表结构
- 已初始化 Nacos 默认管理员账号 `nacos / nacos`
- 已初始化 RBAC 基础数据：权限点、默认角色、默认菜单
- 已创建默认管理员账号 `admin / admin123`
- 已创建少量默认告警规则与分析规则
- RabbitMQ 会自动创建 `test_monitor` vhost，以及 `admin / producer / consumer` 账号
- RabbitMQ 数据会持久化到命名卷 `yuan_rabbitmq_data`
- 所有基础设施与应用服务都带健康检查，启动顺序按 `service_healthy` 控制
- 不预置任何压测计划、场景、任务、运行和报告数据

## 默认账号

- 平台管理员：`admin / admin123`
- Nacos 管理员：`nacos / nacos`

## 重要说明

- 统一 compose 已经把基础设施和应用服务串起来，默认使用容器服务名互相访问。`yuan-test` 这一项要特别注意：根 `.env` 里的 `JMETER_HOME / JMETER_RESULTS_DIR / JMETER_SCRIPTS_DIR` 是宿主机路径，容器内实际固定映射为 `/opt/jmeter`、`/data/jmeter/results`、`/data/jmeter/scripts`。
- 当前默认地址已经统一成：
  - `MYSQL_HOST=mysql`
  - `REDIS_HOST=redis`
  - `RABBITMQ_HOST=rabbitmq`
  - `NACOS_SERVER_ADDR=nacos:8848`
  - `SERVER_URL=nacos`
  - `BASE_URL=nacos`
- `server/env/common.env` 已经下发了 Nacos 配置中心和服务发现的账号密码，默认使用 `nacos / nacos`。
- 由于加入了健康检查，首次启动会比单纯拉起容器更慢一些，这是正常现象。
- 如果 `yuan_mysql_data` 已经初始化过，新增的 `01-nacos-schema.sql` 和 `02-seed-v1.sql` 里的 Nacos 账号初始化不会自动补跑；这时需要手动导入脚本，或重建 MySQL 数据卷。
- `deploy/nacos` 下的配置模板不会自动导入 Nacos；请在 Nacos 启动成功后执行：
  - Linux：`sh deploy/scripts/import-nacos-config.sh --env prod --server-addr 127.0.0.1:8848 --username nacos --password nacos`
  - PowerShell：`pwsh -File deploy/scripts/import-nacos-config.ps1 -Env prod -ServerAddr 127.0.0.1:8848 -Username nacos -Password nacos`
- `deploy/docker/docker-compose.infra.yml` 仍然保留，方便只启动 infra 或单独排障。
- AI 分析在部署模板中默认走 `fallback`；如需真实模型，再补 `.env` 中的 `ARK_API_KEY` 并调整 `server/env/yuan-analysis.env`。
