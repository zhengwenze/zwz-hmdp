# HMDP 高并发秒杀与探店平台

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-green?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5.30-42b883?style=flat-square&logo=vuedotjs)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.4-CB2027?style=flat-square&logo=redis)](https://redis.io/)
[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-Apache--2.0-blue?style=flat-square)](LICENSE)

一个围绕电商秒杀链路、探店内容分发和高频商铺查询构建的全栈项目。后端使用 Spring Boot + MyBatis Plus，前端使用 Vue 3 + Vite，重点解决高并发秒杀中的超卖、一人一单、异步下单可靠性、热点缓存击穿，以及基于地理位置的商铺查询性能问题。

## 项目定位

- 不是纯展示型项目，而是把秒杀链路、高频缓存、关注推送流、签到统计这些典型高并发场景真正落到了代码里。
- 不是“堆技术名词”，README 中提到的能力都能在仓库中找到对应实现，例如 `seckill.lua`、`VoucherOrderServiceImpl`、`CacheClient`、`ShopServiceImpl`、`BlogServiceImpl`。
- 前后端已经打通，既有 Spring Boot 接口实现，也有 Vue 3 页面、接口封装、路由和容器化部署配置。

## 技术栈

### 后端

| 分类 | 技术 | 实际用途 |
| --- | --- | --- |
| 核心框架 | Spring Boot 3.5.0 | Web 服务、IOC、事务管理 |
| Web | Spring MVC | REST API、拦截器、参数绑定 |
| 持久层 | MyBatis Plus 3.5.7 | CRUD、条件查询、分页 |
| 数据库 | MySQL 8.0 | 用户、商铺、博客、优惠券、订单等持久化 |
| 缓存 / 中间件 | Spring Data Redis | 登录态、缓存、签到 Bitmap、点赞 ZSet、关注 Set、Feed、GEO、Stream |
| 分布式锁 | Redisson 3.17.7 | 秒杀订单创建兜底锁 |
| 工具库 | Hutool 5.8.8、Lombok | Bean 转换、随机数、工具方法、样板代码精简 |
| 构建 | Maven | 依赖管理、打包 |

### 前端

| 分类 | 技术 | 实际用途 |
| --- | --- | --- |
| 核心框架 | Vue 3.5.30 | 页面渲染、响应式状态 |
| 构建工具 | Vite 8.0.1 | 本地开发与构建 |
| 路由 | Vue Router 5.0.4 | `createWebHashHistory()` 路由管理 |
| 状态管理 | Pinia 3.0.4 | 会话、页面流转和共享状态 |
| UI 组件库 | Element Plus 2.13.6 | 新版页面 UI 组件 |
| HTTP 客户端 | Axios 1.14.0 | 接口请求封装 |
| 工程化 | `unplugin-auto-import` 21、`unplugin-vue-components` 32 | Element Plus 自动导入、减少模板代码 |

### 部署与运行

| 分类 | 技术 | 实际用途 |
| --- | --- | --- |
| 容器编排 | Docker Compose | 一键拉起 MySQL、Redis、后端、前端 |
| 后端镜像 | Maven 多阶段构建 + Eclipse Temurin 21 JRE | 构建并运行 Spring Boot Jar |
| 前端镜像 | Node 20 Alpine + Nginx Alpine | 构建静态资源并通过 Nginx 提供服务 |
| 反向代理 | Nginx | 静态资源托管、前端路由回退、接口转发到后端 |

## 项目亮点功能

### 1. 高并发秒杀链路

- 秒杀入口是 `POST /voucher-order/seckill/{id}`，先执行 Redis Lua 脚本，再返回订单号，避免请求直接压到数据库。
- Lua 脚本在 Redis 侧原子完成 4 件事：库存校验、用户防重、库存扣减、订单消息写入 `stream.orders`。
- 订单不在请求线程里直接落库，而是交给 `Redis Stream + Consumer Group` 异步消费，天然具备削峰能力。
- 订单 ID 由 `RedisIdWorker` 生成，使用时间戳加日内自增序列，保证全局唯一且趋势递增。
- 真正入库前再通过 Redisson 锁做一次兜底，进一步避免并发下重复创建订单。

### 2. 高可用缓存体系

- 店铺详情查询封装在 `CacheClient`，同时实现了缓存穿透防护和逻辑过期缓存重建。
- `queryWithPassThrough()` 通过空值缓存拦截不存在数据，减少无效请求反复击穿数据库。
- `queryWithLogicalExpire()` 通过逻辑过期 + 互斥锁 + 线程池异步重建，保证热点数据过期时系统仍然可读。
- 店铺更新时先写数据库再删缓存，保持缓存与数据库的最终一致性。

### 3. 基于 Redis 的社交能力

- 关注关系使用 Redis Set 维护，支持关注、取关和共同关注求交集。
- 博客点赞使用 Redis ZSet 记录点赞用户和时间，便于快速获取点赞 Top 用户。
- 发布博客后会把内容推送到粉丝收件箱 `feed:{userId}`，关注流采用滚动分页查询，适合社交场景的“下拉加载更多”。

### 4. 基于地理位置的商铺查询

- 商铺位置信息按类型写入 Redis GEO，查询时直接用坐标和半径进行检索。
- 结果先按距离排序，再结合分页参数截取，最后按 `order by field(id, ...)` 保证数据库查询结果顺序与 GEO 结果一致。
- 这种做法避免了基于经纬度的全表扫描，更适合“附近商铺”这类高频场景。

### 5. 用户登录与签到链路

- 登录使用手机号 + 验证码模式，验证码存 Redis。
- 登录态不是 JWT，而是随机 token 写入 Redis Hash，请求头使用 `authorization: <token>`。
- `RefreshTokenInterceptor` 会在请求时刷新 token TTL，兼顾无状态调用体验和登录态续期。
- 用户签到基于 Redis Bitmap，连续签到天数通过 `BITFIELD` 统计，适合高频、低存储成本场景。

### 6. 前端工程化落地

- 前端采用 Vue 3 + Element Plus 重构了一套新的工作台式页面，核心页面位于 `frontend/src/new_pages/`。
- 路由使用 Hash 模式，配合 Nginx `try_files`，本地开发和容器部署都不会出现刷新 404。
- Vite 开发环境通过代理把 `/user`、`/shop`、`/blog`、`/voucher-order` 等接口转发到后端，联调成本较低。
- Element Plus 通过自动导入插件按需引入，避免手工逐个注册组件。

## 技术难点与攻坚

### 难点一：秒杀场景下如何避免超卖和重复下单

**问题**  
高并发下，如果先查数据库库存再扣减，多个线程可能同时读到“还有库存”，最终导致超卖；同一用户快速重复点击还会带来一人多单问题。

**攻破方式**  
把“库存校验 + 用户是否已下单 + 扣减库存 + 投递订单消息”全部收口到 `backend/src/main/resources/seckill.lua`，让 Redis 用单线程原子执行整条链路。

**效果**  
秒杀资格判断不再依赖数据库事务竞争，而是在 Redis 端一次完成，大幅缩小并发窗口。

### 难点二：异步下单后，消费者异常如何保证订单不丢

**问题**  
如果消息刚被消费者读到，服务就宕机或处理失败，简单队列模型很容易出现“已读未处理”的丢单问题。

**攻破方式**  
在 `VoucherOrderServiceImpl` 中使用 Redis Stream 的 Consumer Group 读取 `stream.orders`，正常处理后 `XACK` 确认；异常时主动扫描 Pending List，把未确认消息重新拉起处理。

**效果**  
订单消费具备“至少消费一次”的可靠性，不会因为消费者短暂异常就直接丢失订单。

### 难点三：异步线程里如何保证事务生效

**问题**  
消费者线程里如果直接调用当前类的 `createVoucherOrder()`，会绕过 Spring 代理，事务不会生效。

**攻破方式**  
通过 `@Lazy` 自注入 `IVoucherOrderService voucherOrderService`，在异步消费线程中走代理对象调用事务方法，避开自调用失效问题。

**效果**  
异步消费线程中的订单入库仍然处于 Spring 事务管理之下，避免出现库存与订单状态不一致。

### 难点四：热点商铺缓存过期时如何避免瞬时击穿数据库

**问题**  
热门商铺详情一旦缓存失效，海量请求会在同一时刻打到数据库，形成缓存击穿。

**攻破方式**  
在 `CacheClient.queryWithLogicalExpire()` 中采用逻辑过期方案：缓存命中但逻辑过期时，只让拿到锁的线程异步重建缓存，其他线程直接返回旧值。

**效果**  
热点数据即使在重建窗口期也能保持服务可用，数据库不会被同一批请求瞬时打爆。

### 难点五：附近商铺如何兼顾距离排序与分页

**问题**  
地理位置查询不仅要找“附近”，还要按距离排序并分页，单纯使用数据库经纬度计算成本高，排序也不稳定。

**攻破方式**  
先在 Redis GEO 里按坐标和半径查出目标商铺，再根据分页参数截断结果，并用 `order by field(id, ...)` 维持距离排序回表查询。

**效果**  
附近商铺查询能够在较低开销下同时满足“按距离排序”和“按页加载”。

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 用户 | 验证码登录、登出、当前用户查询、昵称修改、签到、连续签到统计 |
| 商铺 | 商铺详情、按类型分页、附近商铺查询、商铺信息更新 |
| 博客 | 热门博客、博客详情、发布博客、点赞、点赞榜 |
| 社交 | 关注/取关、是否关注、共同关注、关注推送流 |
| 优惠券 | 普通优惠券、新增秒杀券、秒杀下单 |
| 图片 | 图片上传与删除 |

## 核心流程

### 秒杀下单流程

```text
用户请求秒杀
  -> Redis Lua 脚本原子校验库存与一人一单
  -> 写入 stream.orders
  -> 返回订单号
  -> 消费者线程读取 Stream
  -> Redisson 锁兜底
  -> 事务创建订单并扣减数据库库存
  -> XACK 确认消息
```

### 商铺查询流程

```text
查询商铺详情
  -> 读 Redis
  -> 未过期：直接返回
  -> 已逻辑过期：返回旧值，同时异步重建缓存
  -> 缓存不存在：按策略回源数据库并写回
```

## 项目结构

```text
.
├── backend
│   ├── db/                         # 初始化 SQL
│   ├── src/main/java/com/hmdp
│   │   ├── config/                # Redis、Redisson、MVC 配置
│   │   ├── controller/            # REST API
│   │   ├── dto/                   # 请求/响应 DTO
│   │   ├── entity/                # 实体
│   │   ├── interceptor/           # 登录校验、Token 刷新
│   │   ├── mapper/                # MyBatis Plus Mapper
│   │   ├── service/               # 业务接口与实现
│   │   └── utils/                 # CacheClient、RedisIdWorker 等工具类
│   ├── src/main/resources
│   │   ├── mapper/                # XML SQL
│   │   ├── seckill.lua            # 秒杀原子脚本
│   │   └── unlock.lua             # 分布式锁解锁脚本
│   └── Dockerfile
├── frontend
│   ├── src
│   │   ├── components/            # 通用组件
│   │   ├── layout/                # 布局
│   │   ├── new_pages/             # Element Plus 重构页面
│   │   ├── router/                # 路由
│   │   ├── services/              # API 封装
│   │   ├── stores/                # Pinia 状态
│   │   └── styles/                # 主题和布局样式
│   ├── Dockerfile
│   └── nginx.conf
├── docker-compose.yml
└── README.md
```

## 快速启动

### 方式一：Docker Compose 一键启动

环境要求：

- Docker
- Docker Compose

启动命令：

```bash
docker compose up -d --build
```

默认访问地址：

- 前端：[http://localhost:8080](http://localhost:8080)
- 后端：[http://localhost:8081](http://localhost:8081)

当前编排会启动以下服务：

- `mysql`：MySQL 8.0
- `redis`：Redis 7.4
- `redis-init`：初始化 `stream.orders` 消费者组
- `app`：Spring Boot 后端
- `frontend`：Nginx 托管的 Vue 前端

### 方式二：本地开发

环境要求：

- JDK 21
- Maven
- Node.js 20
- Yarn
- 本地 MySQL / Redis

后端启动：

```bash
cd backend
mvn spring-boot:run
```

前端启动：

```bash
cd frontend
yarn install
yarn dev
```

## 开发说明

- 默认后端端口为 `8081`，前端开发服务器会把接口代理到该端口。
- 登录 token 通过请求头 `authorization` 传递，不使用 Cookie Session。
- 开发环境下，发送验证码接口会直接回传验证码，便于前端联调。

## 适合在简历或面试中重点展开的点

- 如何用 Lua 脚本把秒杀资格校验从数据库前置到 Redis。
- 为什么 Redis Stream 比简单阻塞队列更适合做可恢复的异步下单链路。
- 逻辑过期和空值缓存分别解决什么问题，为什么不能混为一种方案。
- 关注推送流、点赞榜、签到统计分别为什么适合用 ZSet、Set、Bitmap。
- 前端如何通过 Hash 路由、Nginx `try_files` 和 Vite 代理降低部署与联调复杂度。

## License

Apache License 2.0，详见 [LICENSE](LICENSE)。
