# AGENTS.md

## Purpose

这份文档定义 AI coding agent 在本仓库中的工作方式。

目标是让 agent 做出安全、最小、可验证、便于评审的改动，并且不破坏项目已有的高并发设计。

### Instruction Priority

发生冲突时，按以下优先级执行：

1. 用户当前任务与明确要求
2. 本文件中的安全约束和架构约束
3. 仓库事实源：`README.md`、`backend/pom.xml`、`frontend/package.json`、`docker-compose.yml`
4. 外部资料、示例实现、第三方文档

---

## How To Work

### Default workflow

1. 先理解任务，再查看相关代码和配置
2. 优先复用仓库内已有模式、工具类、命名和分层
3. 仅在本仓库无法回答问题时，补充查官方文档或成熟Github开源实现
4. 做最小且完整的改动，不做顺手重构
5. 改完后做与风险匹配的验证
6. 向用户说明改了什么、为什么这么改、如何验证

### Reuse and research

- 先看仓库现有实现，再看外部资料
- 优先复用已有 service、DTO、Redis key 约定、前端 services 和 stores
- 引入新依赖前，先确认现有框架或当前代码库无法解决问题
- 借鉴外部模式时要适配本仓库，不要直接照搬

### When planning is required

以下情况先出计划，再实施：

- 任务跨多个模块或多个文件，且行为耦合明显
- 同时改后端和前端
- 修改 API 行为、持久化逻辑、缓存策略、Redis 结构、消息流或部署配置
- 任务本身含糊、风险高，或性能敏感

简单的单点修复、小范围文案或样式调整，可以直接实现。

### When to stop and ask the user

遇到以下情况，不要自行拍板，先和用户确认：

- 改秒杀语义、一人一单语义、库存扣减语义
- 改缓存 key、TTL、失效策略或重建策略
- 改 Redis Stream 的 ACK、重试、Pending List 或消费者行为
- 改前后端接口字段、字段类型或响应语义
- 引入新依赖、替换已有依赖，或切换包管理器
- 需要做明显超出当前任务范围的重构

---

## Repo Facts

### Tech stack

- Backend: Java 8, Spring Boot 2.7.4, Spring MVC, MyBatis Plus 3.5.2, Redis, Redisson, Maven
- Frontend: Vue 3, Vite, Vue Router, Pinia, Element Plus, Axios
- Runtime: Docker Compose, MySQL 8, Redis 7.4, Nginx

### Effective structure

- `backend/src/main/java/com/hmdp/config/`：Redis、Redisson、MVC 等配置
- `backend/src/main/java/com/hmdp/controller/`：REST controllers
- `backend/src/main/java/com/hmdp/service/`：service 接口与实现
- `backend/src/main/java/com/hmdp/mapper/`：MyBatis Plus mappers
- `backend/src/main/java/com/hmdp/dto/`：DTOs
- `backend/src/main/java/com/hmdp/entity/`：entities
- `backend/src/main/java/com/hmdp/interceptor/`：登录和 token 相关拦截器
- `backend/src/main/java/com/hmdp/utils/`：`CacheClient`、`RedisIdWorker` 等工具
- `backend/src/main/resources/seckill.lua`：秒杀原子脚本
- `backend/src/main/resources/unlock.lua`：解锁脚本
- `backend/db/`：初始化 SQL
- `frontend/src/new_pages/`：新版 Element Plus 页面
- `frontend/src/pages/`：现有页面入口与旧页面
- `frontend/src/components/`：复用组件
- `frontend/src/layout/`：布局组件
- `frontend/src/router/`：路由定义
- `frontend/src/services/`：API wrappers
- `frontend/src/stores/`：Pinia stores
- `frontend/src/config/`：前端配置常量
- `frontend/src/utils/`：前端工具函数
- `frontend/src/styles/`：主题与布局样式
- `docker-compose.yml`：本地编排
- `frontend/nginx.conf`：前端路由回退与反向代理

### Behavior-critical facts

- 秒杀入口依赖 `seckill.lua` 做库存校验、一人一单校验和消息投递
- 订单异步链路依赖 Redis Stream `stream.orders`
- 秒杀下单在数据库落库前还有 Redisson 锁兜底
- 热点店铺缓存依赖 `CacheClient.queryWithLogicalExpire()`
- 附近商铺查询依赖 Redis GEO 和 `order by field(id, ...)` 保序回表
- 登录 token 通过请求头 `authorization` 传递，不使用 Cookie Session
- 前端路由当前使用 Hash 模式

### Package manager note

前端目录中同时存在 `yarn.lock` 和 `package-lock.json`。默认不要擅自切换包管理器；如果任务涉及依赖安装、锁文件更新或脚本调整，先确认当前任务应遵循的标准。

---

## Critical Path Guardrails

以下路径是高风险区域，处理时优先保守：

- 不要把秒杀资格判断退回数据库热路径
- 不要绕过 `seckill.lua`、Redis Stream 或 Redisson 兜底锁
- 不要随意改变消息确认、重试和 Pending List 恢复逻辑
- 不要把 Redis 热路径改成高频数据库循环查询
- 不要混用缓存穿透防护、逻辑过期和缓存失效策略
- 不要改动 GEO 查询分页和顺序保持逻辑，除非任务明确要求
- 不要静默改变 token 刷新行为或 `authorization` 头约定
- 不要破坏 Hash 路由和 Nginx 回退假设

如果必须改这些区域，要在结果里明确说明：

- 改了什么行为
- 为什么必须这样改
- 如何验证没有破坏关键链路

---

## Change Rules

### Backend

- 保持 `controller -> service -> mapper/persistence` 分层
- 业务逻辑放在 service，不要堆到 controller
- 修改事务、异步线程或代理调用前，先确认 Spring 事务边界
- 改 Redis key、Lua、Stream、锁逻辑时，补充关键影响说明
- 不要吞异常；要保留足够的日志和可观测性

### Frontend

- 页面逻辑留在 pages，复用逻辑抽到 components、services、stores
- 统一通过现有 API wrapper 调后端，避免重复请求封装
- UI 保持当前 Vue 3 + Element Plus 风格，不引入新的 UI 框架
- 变更路由、代理或部署方式时，同时检查开发和容器场景

### API and dependency changes

- 改接口时，同时更新后端 controller/service/DTO 和前端 services/调用方
- 不要静默改字段名、字段类型、状态码语义或返回结构
- 新增依赖必须说明必要性和替代方案为何不合适

### Documentation and completion

行为、结构、启动方式或关键路径有变化时，更新相关文档，通常至少检查 `README.md` 是否需要同步。

任务可以视为完成，至少要满足：

- 改动范围与任务一致
- 复用了现有模式，没有引入无关重构
- 前后端契约保持一致
- 关键路径没有被绕过
- 已做与风险相称的验证
- 已说明未验证项、风险或假设

---

## Validation Matrix

按改动类型选择最低验证，不要机械地全跑一遍。

| 改动类型                                      | 最低验证                                                                                           |
| --------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| 仅后端普通逻辑                                | `cd backend && mvn test`；若仓库暂无可用测试，则至少做 `mvn -q -DskipTests package` 或等价编译验证 |
| 后端关键路径：秒杀、缓存、Redis、Stream、事务 | 相关命令验证 + 明确写出手工验证思路                                                                |
| 仅前端页面或交互                              | `cd frontend && yarn dev`                                                                          |
| 前端构建、路由、资源或部署相关                | `cd frontend && yarn build`，必要时补 `yarn dev`                                                   |
| 同时改前后端接口                              | 至少分别验证后端和前端，并确认联调路径                                                             |
| Docker、环境、代理、容器编排                  | `docker compose up -d --build`                                                                     |

### Validation notes

- 当前仓库下未发现 `backend/src/test`，因此后端变更不要只写“已跑测试”，要说明实际跑的是测试、编译、启动烟测还是手工验证
- 前端验证优先沿用 README 中现有命令：`yarn install`、`yarn dev`
- 若任务涉及依赖或锁文件，先确认包管理器后再执行安装命令
- Bug 修复优先复现问题，再验证修复

---

## Handoff Template

结束任务时，给出简洁交接，至少包含：

- Summary of changes
- Files changed
- Key design decisions
- Verification performed
- Risks / follow-ups
- Assumptions or limitations
