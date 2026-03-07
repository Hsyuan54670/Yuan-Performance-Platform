# Frontend-Driven Backend Contract (v1)

本文档按当前前端实现（`yuan-web`）反推后端最小可交付内容，目标是让你可以直接开始后端开发并快速联调。

## 1. 开发目标

- 与前端现有 TypeScript 类型 **100% 对齐**。
- 先实现最小可用接口（M0），再补齐完整设计稿能力（M1/M2）。
- 统一经由 Gateway 暴露：`/api/**`。

## 2. 统一约定

### 2.1 时间与时区

- 后端统一输出 ISO-8601 字符串（示例：`2026-03-07T10:03:11`）。
- 数据库存 UTC，返回时按用户时区转换（前端当前可直接显示本地时区）。

### 2.2 认证

- 登录后返回 `token` + `refreshToken`。
- 除登录外接口需校验 `Authorization: Bearer <token>`。

### 2.3 返回格式

前端当前 mock 是“直接返回业务对象”，建议后端先按此返回，避免前端额外改造。

## 3. 数据模型（前端强依赖）

### 3.1 Auth

```ts
interface UserInfo {
  id: number;
  username: string;
  nickname: string;
  role: string;
}

interface LoginRequest {
  username: string;
  password: string;
}

interface LoginResponse {
  token: string;
  refreshToken: string;
  user: UserInfo;
}
```

### 3.2 Test

```ts
type TaskStatus = "RUNNING" | "PENDING" | "SUCCESS" | "FAILED" | "STOPPED";

interface TestPlan {
  id: number;
  name: string;
  targetUrl: string;
  concurrency: number;
  duration: number;
  rampType: "STAIR" | "LINEAR";
  createdAt: string;
}

interface SceneStep {
  id: number;
  name: string;
  method: "GET" | "POST" | "PUT" | "DELETE";
  path: string;
  weight: number;
}

interface TestScene {
  id: number;
  name: string;
  steps: SceneStep[];
}

interface TestTask {
  id: number;
  planName: string;
  sceneName: string;
  status: TaskStatus;
  startTime: string;
  duration: number;
  qps: number;
  p99: number;
  errorRate: number;
}

interface RealtimeMetricPoint {
  time: string;
  qps: number;
  p50: number;
  p90: number;
  p99: number;
  errorRate: number;
}
```

### 3.3 Monitor

```ts
interface SystemMetric {
  cpu: number;
  memory: number;
  disk: number;
  networkIn: number;
  networkOut: number;
}

interface AlertRule {
  id: number;
  name: string;
  metric: "CPU" | "MEMORY" | "P99" | "ERROR_RATE";
  op: ">" | ">=";
  threshold: number;
  level: "INFO" | "WARN" | "CRITICAL";
  enabled: boolean;
}

interface AlertRecord {
  id: number;
  taskId: number;
  ruleName: string;
  level: "INFO" | "WARN" | "CRITICAL";
  currentValue: number;
  createdAt: string;
}
```

### 3.4 Analysis

```ts
type Grade = "A" | "B" | "C" | "D" | "E";

interface BottleneckItem {
  time: string;
  type: "CPU" | "MEMORY" | "DATABASE" | "NETWORK" | "CONNECTION_POOL";
  reason: string;
  severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
}

interface SuggestionItem {
  id: number;
  priority: "P0" | "P1" | "P2";
  title: string;
  detail: string;
}

interface AnalysisReport {
  taskId: number;
  grade: Grade;
  score: number;
  summary: string;
  bottlenecks: BottleneckItem[];
  suggestions: SuggestionItem[];
}

interface AnalysisRule {
  id: number;
  name: string;
  expression: string;
  bottleneckType: string;
  severity: string;
  enabled: boolean;
}
```

### 3.5 Report

```ts
interface ReportItem {
  id: number;
  taskId: number;
  title: string;
  createdAt: string;
  grade: "A" | "B" | "C" | "D" | "E";
  summary: string;
}

interface ComparePoint {
  label: string;
  baseline: number;
  current: number;
}
```

## 4. M0 接口清单（前端立即可用）

### 4.1 Auth + System

- `POST /api/auth/login`
  - req: `LoginRequest`
  - resp: `LoginResponse`
- `POST /api/auth/logout`
  - resp: `boolean`
- `GET /api/auth/user/info`
  - resp: `UserInfo`
- `GET /api/auth/user/list`
  - resp: `UserRow[]`
- `GET /api/auth/role/list`
  - resp: `RoleRow[]`
- `GET /api/auth/menu/tree`
  - resp: `MenuNode[]`

### 4.2 Test

- `GET /api/test/plan/list`
  - resp: `TestPlan[]`
- `GET /api/test/scene/list`
  - resp: `TestScene[]`
- `GET /api/test/task/list`
  - resp: `TestTask[]`
- `POST /api/test/task/start`
  - req: `{ planId?: number; sceneId?: number }`
  - resp: `{ success: boolean }`
- `POST /api/test/task/{id}/stop`
  - resp: `{ success: boolean }`
- `GET /api/test/task/{id}/metrics/history`
  - resp: `RealtimeMetricPoint[]`

### 4.3 Monitor

- `GET /api/monitor/sys-metrics/{taskId}`
  - resp: `SystemMetric`
- `GET /api/monitor/alert-rule/list`
  - resp: `AlertRule[]`
- `POST /api/monitor/alert-rule`
  - req: `AlertRule`（忽略 id 也可）
  - resp: `AlertRule`
- `GET /api/monitor/alert-record/list`
  - resp: `AlertRecord[]`
- `WS /ws/monitor/{taskId}`
  - message payload: `RealtimeMetricPoint`

### 4.4 Analysis

- `POST /api/analysis/analyze/{taskId}`
  - resp: `{ taskId: number; triggered: boolean }`
- `GET /api/analysis/report/{taskId}`
  - resp: `AnalysisReport`
- `GET /api/analysis/rule/list`
  - resp: `AnalysisRule[]`
- `POST /api/analysis/rule`
  - req: `AnalysisRule`
  - resp: `AnalysisRule`

### 4.5 Report

- `GET /api/report/list`
  - resp: `ReportItem[]`
- `GET /api/report/compare?baselineTaskId={id}&currentTaskId={id}`
  - resp: `ComparePoint[]`
- `GET /api/report/{id}/export?format=pdf|html`
  - resp: `{ success: true, downloadUrl?: string }`

## 5. 请求/响应示例

### 5.1 登录

`POST /api/auth/login`

```json
{
  "username": "admin",
  "password": "123456"
}
```

```json
{
  "token": "jwt-token",
  "refreshToken": "refresh-token",
  "user": {
    "id": 1,
    "username": "admin",
    "nickname": "Ops Lead",
    "role": "ADMIN"
  }
}
```

### 5.2 任务列表

`GET /api/test/task/list`

```json
[
  {
    "id": 3001,
    "planName": "Checkout Peak Hour",
    "sceneName": "Purchase Flow",
    "status": "RUNNING",
    "startTime": "2026-03-07T10:00:00",
    "duration": 600,
    "qps": 1320,
    "p99": 1420,
    "errorRate": 1.78
  }
]
```

### 5.3 WebSocket 实时指标

`WS /ws/monitor/3001`

```json
{
  "time": "10:03:15",
  "qps": 1258,
  "p50": 120,
  "p90": 420,
  "p99": 1450,
  "errorRate": 1.9
}
```

## 6. 数据库设计建议（M0）

### 6.1 auth

- `auth_user(id, username, password_hash, nickname, status, created_at, updated_at)`
- `auth_role(id, code, name, description)`
- `auth_user_role(user_id, role_id)`
- `auth_menu(id, name, path, parent_id, sort)`
- `auth_role_menu(role_id, menu_id)`

### 6.2 test

- `test_plan(id, user_id, name, target_url, concurrency, duration, ramp_type, created_at)`
- `test_scene(id, user_id, name, created_at)`
- `test_scene_step(id, scene_id, step_order, name, method, path, weight)`
- `test_task(id, user_id, plan_id, scene_id, status, start_time, end_time, duration, qps, p99, error_rate)`
- `test_metric_second(id, task_id, ts, qps, p50, p90, p99, error_rate)`

### 6.3 monitor

- `monitor_sys_metric_second(id, task_id, ts, cpu, memory, disk, network_in, network_out)`
- `monitor_alert_rule(id, user_id, name, metric, op, threshold, level, enabled)`
- `monitor_alert_record(id, task_id, rule_id, rule_name, level, current_value, created_at)`

### 6.4 analysis/report

- `analysis_rule(id, user_id, name, expression, bottleneck_type, severity, enabled)`
- `analysis_report(id, task_id, grade, score, summary, created_at)`
- `analysis_bottleneck(id, report_id, time_point, type, reason, severity)`
- `analysis_suggestion(id, report_id, priority, title, detail)`
- `report_record(id, task_id, title, grade, summary, created_at)`

## 7. 消息队列与异步流程（建议）

- Exchange: `yuan.test`
- Routing Keys:
  - `test.metrics`（每秒指标）
  - `test.task.status`（状态变更）
  - `test.task.completed`（任务结束）
- Analysis 订阅 `test.task.completed` 自动触发分析。
- Report 订阅 `analysis.completed` 自动生成报告。

## 8. 开发优先级

### M0（先做）

- 所有 `GET list` 接口 + 登录接口 + 任务 start/stop + 分析触发 + WebSocket 推送。

### M1（第二阶段）

- 真正接入 JMeter 执行、指标持久化、告警规则实时计算。

### M2（第三阶段）

- LLM 真实调用、报告导出（HTML/PDF）、历史基线对比。

## 9. 后端实现建议（Spring Cloud）

- `yuan-gateway`：JWT 校验、路由转发。
- `yuan-auth`：登录、用户/角色/菜单接口。
- `yuan-test`：计划/场景/任务 + JMeter 执行 + MQ 发布。
- `yuan-monitor`：指标消费、系统指标、告警、WebSocket。
- `yuan-analysis`：规则引擎 + LLM + 分析报告。
- `yuan-report`：报告列表/对比/导出。

## 10. 联调验收标准

- 登录后可进入系统，右上角用户信息正常显示。
- 任务页可启动/停止并每秒看到曲线更新。
- 监控页可看到 CPU/内存/磁盘/网络指标。
- 分析页能看到报告列表、瓶颈时间线、优化建议。
- 报告页可展示列表、对比图，并触发导出。

---

如果你愿意，我可以下一步直接给你一份可导入 Postman 的接口集合（按上面 M0 全部接口生成）。
