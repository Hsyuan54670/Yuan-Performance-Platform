# YUAN Server Deploy Guide

这个目录用于放置已经打好的后端 JAR 包，以及统一 compose 启动时所需的应用层配置。

## 目录说明

- `env/`：每个服务的环境变量配置。
- `Dockerfile.jvm`：所有 Spring Boot 服务共用的运行时镜像模板。
- `logs/`、`pids/`：预留目录，方便以后继续扩展宿主机直跑脚本或日志落地。

## 哪些 JAR 需要部署

真正需要部署的是 6 个运行态服务：

- `yuan-auth-0.0.1-SNAPSHOT.jar`
- `yuan-test-0.0.1-SNAPSHOT.jar`
- `yuan-monitor-0.0.1-SNAPSHOT.jar`
- `yuan-analysis-0.0.1-SNAPSHOT.jar`
- `yuan-report-0.0.1-SNAPSHOT.jar`
- `yuan-gateway-0.0.1-SNAPSHOT.jar`

下面两个 JAR 是共享库，不是独立服务，不需要单独启动：

- `yuan-common-0.0.1-SNAPSHOT.jar`
- `yuan-api-0.0.1-SNAPSHOT.jar`

## 推荐启动方式

当前推荐直接进入 `deploy` 目录执行：

```powershell
docker compose up -d
```

统一 compose 会自动读取根目录 `.env`，并加载 `server/env/` 下的服务配置。

## 关键约束

- `env/common.env` 里已经把基础设施地址改成容器服务名：
  - `mysql`
  - `redis`
  - `rabbitmq`
  - `nacos`
- `yuan-test` 依赖宿主机 JMeter 安装目录，所以必须正确设置根目录 `.env` 中的宿主机路径：
  - `JMETER_HOME`
  - `JMETER_RESULTS_DIR`
  - `JMETER_SCRIPTS_DIR`
- `server/env/yuan-test.env` 中的 `JMETER_HOME / JMETER_RESULTS_DIR / JMETER_SCRIPTS_DIR` 是容器内路径，默认固定为 `/opt/jmeter`、`/data/jmeter/results`、`/data/jmeter/scripts`，不要再填宿主机路径。`docker-compose.yml` 会负责把根 `.env` 中的宿主机目录挂进去。
- `yuan-analysis` 默认仍可在没有外部模型 Key 的情况下运行；如需真模型，再补根目录 `.env` 中的 `ARK_API_KEY`。