# AI 驱动的性能智能压测与瓶颈分析平台

## 一、项目概述

### 1.1 项目背景

随着互联网应用规模不断扩大，系统性能问题日益成为影响用户体验和业务发展的关键因素。传统的性能压测工具（如 JMeter、Locust）虽然能够执行压力测试，但在测试结果分析、瓶颈定位和优化建议方面仍依赖人工经验，存在分析效率低、定位不精准等问题。

本项目旨在构建一个 **AI 驱动的性能智能压测与瓶颈分析平台**，将压力测试执行与 AI 智能分析深度结合，实现从"压测 → 数据采集 → 异常检测 → 瓶颈定位 → 优化建议"的全链路自动化闭环。

### 1.2 核心目标

| 目标 | 描述 |
|------|------|
| 智能压测 | 支持 HTTP 接口压测、场景编排、阶梯加压，实时采集性能指标 |
| AI 分析 | 基于规则引擎 + LLM 的混合分析架构，自动识别性能瓶颈 |
| 可视化 | 实时监控看板、历史趋势对比、可交互的分析报告 |
| 微服务架构 | 基于 Spring Cloud 的微服务体系，服务独立部署、弹性扩展 |

### 1.3 目标用户

- 后端开发工程师：对自己负责的接口进行性能验证
- 测试工程师：执行系统级性能压测，输出测试报告
- 运维工程师：监控系统性能指标，识别资源瓶颈
- 技术管理者：查看性能趋势报告，辅助容量规划决策

---

## 二、系统架构设计

### 2.1 整体架构

```
                              ┌──────────────┐
                              │   用户浏览器  │
                              │  (React SPA) │
                              └──────┬───────┘
                                     │ HTTPS
                              ┌──────▼───────┐
                              │ Nginx 反向代理 │
                              └──────┬───────┘
                                     │
                    ┌────────────────▼────────────────┐
                    │      yuan-gateway (8080)         │
                    │      Spring Cloud Gateway        │
                    │   路由转发 | JWT鉴权 | 限流熔断    │
                    └────────────────┬────────────────┘
                                     │
              ┌──────────────────────┼──────────────────────┐
              │                      │                      │
    ┌─────────▼──────┐   ┌──────────▼─────┐   ┌───────────▼────┐
    │ yuan-auth      │   │ yuan-test      │   │ yuan-monitor   │
    │ 认证服务 (8081) │   │ 压测服务 (8082)│   │ 监控服务 (8083) │
    │ 用户|角色|权限  │   │ 计划|任务|执行  │   │ 指标|采集|告警  │
    └────────────────┘   └───────┬────────┘   └───────┬────────┘
                                 │                     │
                    ┌────────────▼─────────────────────▼────┐
                    │            RabbitMQ 消息队列           │
                    │    压测事件 | 指标数据 | 分析触发       │
                    └────────────┬──────────────────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                                     │
    ┌─────────▼──────────┐            ┌─────────────▼──────┐
    │ yuan-analysis      │            │ yuan-report        │
    │ AI分析服务 (8084)   │            │ 报告服务 (8085)     │
    │ 规则引擎|LLM|瓶颈   │            │ 生成|对比|导出      │
    └────────────────────┘            └────────────────────┘

    ┌───────────────────────────────────────────────────────┐
    │                    基础设施层                          │
    │  Nacos(注册/配置)  MySQL 8.0  Redis 8.4  RabbitMQ        │
    └───────────────────────────────────────────────────────┘
```

### 2.2 技术选型

| 层次 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **前端** | React | 18.x | UI 框架 |
| | TypeScript | 5.x | 类型安全 |
| | Ant Design | 5.x | UI 组件库 |
| | ECharts | 5.x | 数据可视化 |
| | Axios | 1.x | HTTP 请求 |
| | React Router | 6.x | 路由管理 |
| **网关** | Spring Cloud Gateway | 4.x | API 网关 |
| **后端** | Java | 21 | 开发语言 |
| | Spring Boot | 3.5.x | 应用框架 |
| | Spring Cloud | 2025.0.x | 微服务框架 |
| | Spring Cloud Alibaba | 2021.0.4.0 | Nacos/Sentinel 集成 |
| | MyBatis-Plus | 3.5.x | ORM 框架 |
| | Sa-Token | 1.37.x | 权限认证框架 |
| **注册/配置中心** | Nacos | 2.3.x | 服务注册与配置管理 |
| **消息队列** | RabbitMQ | 4.2.x | 异步消息通信 |
| **缓存** | Redis | 8.4.x | 缓存 + 分布式锁 |
| **数据库** | MySQL | 8.0 | 关系型数据存储 |
| **压测引擎** | Apache JMeter Core | 5.6.3 | 压测执行引擎(Java库集成) |
| **AI** | Qwen | - | LLM 智能分析 |
| **部署** | Docker + Docker Compose | - | 容器化部署 |

### 2.3 微服务划分

#### 2.3.1 yuan-gateway（API 网关服务）

**端口**：8080

**职责**：
- 统一入口，路由转发到各微服务
- JWT Token 鉴权过滤（白名单放行登录/注册接口）
- 全局限流（Sentinel 集成）
- 跨域处理（CORS）
- 请求日志记录

**核心配置**：
```yaml
routes:
  - id: auth-service
    uri: lb://yuan-auth
    predicates:
      - Path=/api/auth/**
  - id: test-service
    uri: lb://yuan-test
    predicates:
      - Path=/api/test/**
  - id: monitor-service
    uri: lb://yuan-monitor
    predicates:
      - Path=/api/monitor/**
  - id: analysis-service
    uri: lb://yuan-analysis
    predicates:
      - Path=/api/analysis/**
  - id: report-service
    uri: lb://yuan-report
    predicates:
      - Path=/api/report/**
```

#### 2.3.2 yuan-auth（认证授权服务）

**端口**：8081

**职责**：
- 用户注册、登录、退出
- JWT Token 签发与校验
- 角色权限管理（RBAC 模型）
- 菜单权限管理

**核心接口**：
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 用户登录 |
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/logout | 用户退出 |
| GET | /api/auth/user/info | 获取当前用户信息 |
| GET | /api/auth/user/list | 用户列表（管理员） |
| POST | /api/auth/role | 创建角色 |
| GET | /api/auth/menu/tree | 获取菜单树 |

#### 2.3.3 yuan-test（压测服务）— 核心服务

**端口**：8082

**职责**：
- 压测计划（Test Plan）的 CRUD 管理
- 压测场景编排（多接口按顺序执行）
- 压测任务的创建、调度与执行
- 集成 JMeter 核心库执行压力测试
- 实时采集压测指标并推送到消息队列
- 支持阶梯加压模式

**核心接口**：
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/test/plan | 创建压测计划 |
| GET | /api/test/plan/{id} | 获取计划详情 |
| GET | /api/test/plan/list | 计划列表（分页） |
| PUT | /api/test/plan/{id} | 更新计划 |
| DELETE | /api/test/plan/{id} | 删除计划 |
| POST | /api/test/scene | 创建压测场景 |
| POST | /api/test/scene/{id}/steps | 配置场景步骤 |
| POST | /api/test/task/start | 启动压测任务 |
| POST | /api/test/task/{id}/stop | 停止压测任务 |
| GET | /api/test/task/{id}/status | 查询任务状态 |
| GET | /api/test/task/{id}/metrics | 获取实时指标 (WebSocket) |
| GET | /api/test/task/list | 任务列表 |

**压测执行流程**：
```
创建计划 → 配置接口/场景 → 启动任务
                                │
                    ┌───────────▼───────────┐
                    │   JMeter Engine 执行    │
                    │  (线程组 + 采样器)       │
                    └───────────┬───────────┘
                                │ 每秒聚合
                    ┌───────────▼───────────┐
                    │ 指标数据发送到 RabbitMQ  │
                    │ (QPS, RT, 错误率...)    │
                    └───────────┬───────────┘
                                │
                    ┌───────────▼───────────┐
                    │   Monitor 服务消费存储   │
                    │   前端 WebSocket 实时展示 │
                    └───────────────────────┘
```

#### 2.3.4 yuan-monitor（监控采集服务）

**端口**：8083

**职责**：
- 消费 RabbitMQ 中的压测指标数据并持久化
- 系统资源指标采集（CPU、内存、磁盘、网络）
- 指标数据的聚合查询（按时间窗口）
- 告警规则配置与触发
- WebSocket 推送实时数据到前端

**核心接口**：
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/monitor/metrics/{taskId} | 查询任务的性能指标 |
| GET | /api/monitor/metrics/{taskId}/summary | 获取指标汇总 |
| GET | /api/monitor/sys-metrics/{taskId} | 查询系统资源指标 |
| POST | /api/monitor/alert-rule | 创建告警规则 |
| GET | /api/monitor/alert-rule/list | 告警规则列表 |
| GET | /api/monitor/alert-record/list | 告警记录列表 |
| WS | /ws/monitor/{taskId} | WebSocket 实时指标推送 |

#### 2.3.5 yuan-analysis（AI 分析服务）— 核心亮点

**端口**：8084

**职责**：
- **规则引擎分析**：基于预定义规则进行初步瓶颈判断
- **LLM 深度分析**：将指标数据 + 规则判断结果构造 Prompt，调用 LLM 生成诊断报告
- 瓶颈分类与定位（CPU / 内存 / 数据库 / 网络 / 连接池）
- 生成优化建议
- 管理自定义分析规则

**核心接口**：
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/analysis/analyze/{taskId} | 触发 AI 分析 |
| GET | /api/analysis/report/{taskId} | 获取分析报告 |
| GET | /api/analysis/report/list | 分析报告列表 |
| GET | /api/analysis/bottleneck/{taskId} | 获取瓶颈详情 |
| POST | /api/analysis/rule | 创建分析规则 |
| GET | /api/analysis/rule/list | 规则列表 |
| PUT | /api/analysis/rule/{id} | 更新规则 |
| GET | /api/analysis/suggestion/{taskId} | 获取优化建议 |

**AI 分析流水线**：
```
压测结束 → MQ 触发分析
                │
    ┌───────────▼───────────┐
    │  Step 1: 数据预处理    │
    │  聚合指标、计算统计值   │
    │  (P50/P90/P99/Max)    │
    └───────────┬───────────┘
                │
    ┌───────────▼───────────┐
    │  Step 2: 规则引擎分析  │
    │  匹配预定义瓶颈规则     │
    │  输出: 初步诊断标签     │
    └───────────┬───────────┘
                │
    ┌───────────▼───────────┐
    │  Step 3: LLM 深度分析  │
    │  构造 Prompt:          │
    │  - 指标摘要            │
    │  - 规则匹配结果         │
    │  - 历史基线对比         │
    │  输出: 结构化诊断报告   │
    └───────────┬───────────┘
                │
    ┌───────────▼───────────┐
    │  Step 4: 结果结构化    │
    │  解析 LLM 输出         │
    │  存储瓶颈记录 + 建议    │
    └───────────────────────┘
```

**规则引擎示例**：
```java
// CPU 瓶颈规则
IF avg_cpu_usage > 80% AND p99_response_time > 2000ms
THEN bottleneck_type = "CPU_BOTTLENECK"
     severity = "HIGH"
     description = "CPU 使用率持续超过 80%，响应时间显著上升"

// 内存泄漏规则
IF memory_usage 持续增长 AND 无回落趋势 AND gc_frequency 增加
THEN bottleneck_type = "MEMORY_LEAK"
     severity = "CRITICAL"

// 数据库瓶颈规则
IF slow_query_count > 10/min AND qps 下降趋势
THEN bottleneck_type = "DATABASE_BOTTLENECK"
     severity = "HIGH"

// 连接池耗尽规则
IF active_connections >= max_connections * 0.9 AND error_rate > 5%
THEN bottleneck_type = "CONNECTION_POOL_EXHAUSTION"
     severity = "HIGH"
```

**LLM Prompt 模板**：
```
你是一个资深的性能工程专家。请根据以下压测数据分析系统性能瓶颈：

## 压测配置
- 目标URL: {target_url}
- 并发数: {concurrency}
- 持续时间: {duration}秒

## 性能指标摘要
- QPS: 平均 {avg_qps}, 最大 {max_qps}
- 响应时间: P50={p50}ms, P90={p90}ms, P99={p99}ms, Max={max_rt}ms
- 错误率: {error_rate}%
- 吞吐量: {throughput} MB/s

## 系统资源
- CPU: 平均 {avg_cpu}%, 最大 {max_cpu}%
- 内存: 平均 {avg_mem}%, 最大 {max_mem}%
- 磁盘IO: 读 {disk_read} MB/s, 写 {disk_write} MB/s

## 规则引擎初步判断
{rule_engine_results}

请输出以下内容（JSON 格式）：
1. 性能评级 (A/B/C/D/E)
2. 主要瓶颈类型及原因分析
3. 风险点识别
4. 具体可执行的优化建议（按优先级排序）
```

#### 2.3.6 yuan-report（报告服务）

**端口**：8085

**职责**：
- 压测报告的生成与管理
- 多次压测结果的对比分析
- 报告导出（HTML / PDF）
- 报告模板管理

**核心接口**：
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/report/generate/{taskId} | 生成压测报告 |
| GET | /api/report/{id} | 获取报告详情 |
| GET | /api/report/list | 报告列表 |
| DELETE | /api/report/{id} | 删除报告 |
| POST | /api/report/compare | 多次压测对比 |
| GET | /api/report/{id}/export | 导出报告 |
| POST | /api/report/template | 创建报告模板 |
| GET | /api/report/template/list | 模板列表 |

### 2.4 公共模块

#### 2.4.1 yuan-common（公共工具模块）

提供所有服务共享的基础能力：

```
yuan-common/
├── exception/          # 全局异常定义
│   ├── BusinessException.java
│   └── GlobalExceptionHandler.java
├── result/             # 统一响应封装
│   ├── R.java          # 统一返回结果
│   └── PageResult.java # 分页结果
├── constant/           # 全局常量
│   ├── HttpStatus.java
│   └── CommonConstant.java
├── util/               # 工具类
│   ├── JwtUtil.java
│   ├── RedisUtil.java
│   └── DateUtil.java
├── entity/             # 公共实体
│   └── BaseEntity.java # 公共字段 (id, createTime, updateTime, deleted)
└── config/             # 公共配置
    ├── MyBatisPlusConfig.java
    ├── RedisConfig.java
    └── CorsConfig.java
```

#### 2.4.2 yuan-api（Feign 接口模块）

定义服务间调用的 Feign 客户端和 DTO：

```
yuan-api/
├── auth/
│   ├── feign/AuthFeignClient.java
│   └── dto/UserDTO.java
├── test/
│   ├── feign/TestFeignClient.java
│   └── dto/TaskDTO.java
├── monitor/
│   ├── feign/MonitorFeignClient.java
│   └── dto/MetricsDTO.java
├── analysis/
│   ├── feign/AnalysisFeignClient.java
│   └── dto/ReportDTO.java
└── report/
    ├── feign/ReportFeignClient.java
    └── dto/ReportRecordDTO.java
```

---

## 三、数据流设计

### 3.1 压测执行数据流

```
┌──────┐    ┌─────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 用户  │───▶│ Gateway │───▶│ Test     │───▶│ JMeter   │───▶│ 目标系统  │
│ 前端  │    │ 网关     │    │ 压测服务  │    │ Engine   │    │ (被测)   │
└──┬───┘    └─────────┘    └────┬─────┘    └────┬─────┘    └──────────┘
   │                            │               │
   │   WebSocket 实时展示        │  指标写入 MQ   │  采样结果
   │◀────────────────────────────┤◀──────────────┘
   │                            │
   │                     ┌──────▼──────┐
   │                     │  RabbitMQ   │
   │                     └──────┬──────┘
   │                            │
   │                     ┌──────▼──────┐
   │◀────────────────────│  Monitor    │
   │                     │  监控服务    │──────▶ MySQL (持久化)
   │                     └─────────────┘
```

### 3.2 AI 分析数据流

```
压测任务完成
     │
     ▼ (MQ 事件: TASK_COMPLETED)
┌────────────┐    Feign 调用    ┌────────────┐
│ Analysis   │ ◀────────────── │  Monitor   │
│ AI分析服务  │  获取指标数据     │  监控服务   │
└─────┬──────┘                 └────────────┘
      │
      ▼
┌─────────────┐
│ 规则引擎匹配  │ ──▶ 初步瓶颈标签
└─────┬───────┘
      │
      ▼
┌─────────────┐
│ 构造 Prompt  │
│ 调用 LLM API │ ──▶ 结构化诊断报告
└─────┬───────┘
      │
      ▼
┌─────────────┐    Feign 调用    ┌────────────┐
│ 存储分析结果  │ ──────────────▶ │  Report    │
│ 通知前端展示  │                 │  报告服务   │
└─────────────┘                 └────────────┘
```

### 3.3 消息队列 Topic 设计

| Exchange | Routing Key | 生产者 | 消费者 | 说明 |
|----------|-------------|--------|--------|------|
| yuan.test | test.metrics | yuan-test | yuan-monitor | 实时压测指标 |
| yuan.test | test.task.status | yuan-test | yuan-monitor, yuan-analysis | 任务状态变更 |
| yuan.test | test.task.completed | yuan-test | yuan-analysis | 任务完成触发分析 |
| yuan.analysis | analysis.completed | yuan-analysis | yuan-report | 分析完成触发报告 |
| yuan.monitor | monitor.alert | yuan-monitor | yuan-analysis | 告警触发 |

---

## 四、前端设计

### 4.1 页面结构

```
├── 登录页 /login
├── 仪表盘 /dashboard
│   ├── 系统概览（任务统计、最近压测、性能趋势）
│   └── 快速开始压测入口
├── 压测管理 /test
│   ├── 压测计划 /test/plan
│   │   ├── 计划列表
│   │   └── 创建/编辑计划
│   ├── 场景管理 /test/scene
│   │   ├── 场景列表
│   │   └── 场景编排（拖拽配置接口顺序）
│   └── 任务管理 /test/task
│       ├── 任务列表
│       ├── 任务详情（实时监控）
│       └── 实时看板（QPS/RT/错误率曲线）
├── 监控中心 /monitor
│   ├── 实时监控 /monitor/realtime
│   │   └── 系统资源面板（CPU/MEM/Disk/Net）
│   └── 告警管理 /monitor/alert
│       ├── 告警规则配置
│       └── 告警记录
├── 智能分析 /analysis
│   ├── 分析报告 /analysis/report
│   │   ├── 报告列表
│   │   └── 报告详情（瓶颈定位 + 优化建议）
│   └── 分析规则 /analysis/rule
│       └── 规则配置
├── 报告中心 /report
│   ├── 报告列表
│   ├── 报告详情
│   └── 对比分析
└── 系统管理 /system
    ├── 用户管理
    ├── 角色管理
    └── 菜单管理
```

### 4.2 核心交互设计

**实时压测监控看板**：
- 左侧：QPS 实时曲线 + 响应时间分布图 (P50/P90/P99)
- 右侧：错误率曲线 + HTTP 状态码分布饼图
- 底部：系统资源面板（CPU / 内存 / 网络仪表盘）
- 数据通过 WebSocket 每秒刷新

**AI 分析报告页**：
- 顶部：性能评级徽章 (A/B/C/D/E) + 核心指标卡片
- 中部：瓶颈定位时间线（标注在 RT 曲线上瓶颈出现的时间点）
- 底部：AI 生成的优化建议列表（按优先级排序，可展开详情）

---

## 五、部署架构

### 5.1 Docker Compose 编排

```yaml
services:
  # 基础设施
  nacos:        # 注册/配置中心
  mysql:        # 关系型数据库
  redis:        # 缓存
  rabbitmq:     # 消息队列

  # 应用服务
  yuan-gateway:
  yuan-auth:
  yuan-test:
  yuan-monitor:
  yuan-analysis:
  yuan-report:

  # 前端
  yuan-web:     # Nginx 托管前端静态资源
```

### 5.2 网络规划

| 服务 | 内部端口 | 外部暴露 |
|------|----------|----------|
| Nginx | 80 | 80 |
| Gateway | 8080 | 8080 |
| Nacos | 8848/9848 | 8848 |
| MySQL | 3306 | 3306 |
| Redis | 6379 | - |
| RabbitMQ | 5672/15672 | 15672(管理界面) |
| 各微服务 | 8081-8085 | - (通过 Gateway 访问) |

### 5.3 环境配置管理

通过 Nacos 配置中心统一管理各环境配置：

```
yuan-auth-dev.yml      # 开发环境
yuan-auth-test.yml     # 测试环境
yuan-auth-prod.yml     # 生产环境
yuan-common.yml        # 公共配置（数据库连接、Redis、MQ 等）
```

---

## 六、安全设计

### 6.1 认证鉴权流程

```
登录请求 → yuan-auth 验证用户名密码
                │
                ▼
        签发 JWT Token (Access + Refresh)
                │
                ▼
     前端存储 Token，请求携带 Authorization Header
                │
                ▼
     Gateway 全局过滤器校验 Token
        │                    │
     有效 ──▶ 转发请求        无效 ──▶ 返回 401
        │
     解析用户信息放入请求头传递给下游服务
```

### 6.2 安全措施

| 措施 | 实现方式 |
|------|----------|
| 接口鉴权 | JWT Token + Gateway 全局过滤 |
| 权限控制 | RBAC 模型，Sa-Token 注解式权限 |
| 接口限流 | Sentinel 网关限流 |
| SQL 注入 | MyBatis-Plus 参数化查询 |
| XSS 防护 | 前端输入过滤 + 后端参数校验 |
| CORS | Gateway 统一跨域配置 |

---

## 七、项目模块依赖关系

```
                    ┌─────────────┐
                    │  yuan-common │  (基础工具、实体、配置)
                    └──────┬──────┘
                           │ 被所有模块依赖
                    ┌──────▼──────┐
                    │   yuan-api   │  (Feign 接口、DTO)
                    └──────┬──────┘
                           │ 被服务模块依赖
        ┌──────────────────┼──────────────────┐
        │                  │                  │
 ┌──────▼──────┐  ┌────────▼───────┐ ┌───────▼───────┐
 │ yuan-auth   │  │  yuan-test     │ │ yuan-monitor  │
 │ yuan-gateway│  │  yuan-analysis │ │ yuan-report   │
 └─────────────┘  └────────────────┘ └───────────────┘
```

---

## 八、非功能性需求

| 维度 | 要求 |
|------|------|
| **性能** | 平台自身 API 响应时间 < 200ms；支持同时运行 5 个压测任务 |
| **可用性** | 各服务独立部署，单服务故障不影响其他服务 |
| **可扩展** | 微服务架构，可水平扩展压测执行节点 |
| **可观测** | 统一日志格式，支持链路追踪 (可选集成 SkyWalking) |
| **数据安全** | 压测数据隔离，用户只能看到自己的数据 |
