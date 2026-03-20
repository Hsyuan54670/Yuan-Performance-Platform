# YUAN V1 Deploy Guide

这个目录用于还原 `YUAN 性能压测与智能分析平台` 第一版交付时的真实部署形态。

## 目录说明

- `docker/`：基础设施依赖启动文件，包含 MySQL、Redis、RabbitMQ、Nacos。
- `sql/`：数据库初始化脚本，包含建库、建表和 V1 必要种子数据。
- `nacos/`：按 `dev / test / prod` 划分的 Nacos 配置模板。
- `nginx/`：前端静态站点反向代理配置，负责 `/api` 和 `/ws` 转发。
- `jmeter/`：JMeter 目录约定与接入说明。
- `scripts/`：辅助脚本，例如一键导入 Nacos 配置。

## 首次部署后的默认系统状态

这是当前 V1 最接近真实交付的初始化状态：

- 已创建 4 个业务库：`yuan_auth / yuan_test / yuan_monitor / yuan_analysis`
- 已创建系统所需核心表结构
- 已初始化 RBAC 基础数据：权限点、默认角色、默认菜单
- 已创建默认管理员账号
- 已创建少量默认告警规则与分析规则
- 不预置任何压测计划、场景、任务、运行和报告数据

默认管理员账号：

- 用户名：`admin`
- 密码：`admin123`

## 推荐启动顺序

1. 启动基础设施：`deploy/docker/docker-compose.infra.yml`
2. 确认 MySQL 首次启动已自动执行 `deploy/sql` 中的初始化脚本
3. 按需导入 `deploy/nacos/<env>` 下的配置模板
4. 准备 JMeter 并确认 `results/`、`scripts/` 目录存在
5. 启动后端：`yuan-auth / yuan-test / yuan-monitor / yuan-analysis / yuan-report / yuan-gateway`
6. 部署前端静态资源或直接运行 `yuan-web`
7. 使用 `admin / admin123` 登录系统

## 重要说明

- 当前 V1 代码默认仍可直接读取各服务本地 `application.yml`，`deploy/nacos` 主要提供环境化模板，便于后续统一迁入配置中心。
- `deploy/docker/docker-compose.infra.yml` 已把 `deploy/sql` 中的脚本挂载进 MySQL 初始化目录，适合首次启动时自动建库建表。
- AI 分析在部署模板中默认走 `fallback`，这样即使没有外部模型 Key，系统也能完整跑通规则分析主链；接入真实模型后，再把 `provider` 改成 `seed2.0-lite` 即可。
