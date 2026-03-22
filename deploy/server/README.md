# YUAN Server Deploy Guide

这个目录用于放置已经打好的后端 JAR，以及统一 compose 启动时读取的服务级环境配置。

## 目录说明

- `env/`：每个服务的环境变量文件。
- `Dockerfile.jvm`：所有 Spring Boot 服务共用的运行镜像模板。
- `logs/`、`pids/`：预留目录，便于后续扩展宿主机直跑或日志落地。

## 需要部署的 JAR

真正需要部署的是 6 个运行态服务：

- `yuan-auth-0.0.1-SNAPSHOT.jar`
- `yuan-test-0.0.1-SNAPSHOT.jar`
- `yuan-monitor-0.0.1-SNAPSHOT.jar`
- `yuan-analysis-0.0.1-SNAPSHOT.jar`
- `yuan-report-0.0.1-SNAPSHOT.jar`
- `yuan-gateway-0.0.1-SNAPSHOT.jar`

下面两个模块是共享库，不需要单独运行：

- `yuan-common-0.0.1-SNAPSHOT.jar`
- `yuan-api-0.0.1-SNAPSHOT.jar`

## 配置来源

- 基础设施密码、Nacos 账号、JMeter 宿主机路径：统一放在 `deploy/.env`。
- 服务级配置：放在 `server/env/*.env`。
- 推荐先参考 [deploy/.env.example](../.env.example) 填写部署环境变量，再启动统一 compose。

## 关键约束

- `env/common.env` 中的基础设施地址已经统一改成容器服务名。
- `yuan-test` 的 `JMETER_HOME / JMETER_RESULTS_DIR / JMETER_SCRIPTS_DIR` 在 `server/env/yuan-test.env` 中必须保持容器内路径，宿主机路径请只写在 `deploy/.env`。
- `yuan-analysis` 即使没有真实模型 Key 也可以先用 `fallback` 跑通完整链路。

## 推荐启动方式

```bash
cd deploy
docker compose up -d
```
