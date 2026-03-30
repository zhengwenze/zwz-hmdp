# 高并发电商秒杀平台

<!-- badges -->

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.4-green?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3-blue?style=flat-square&logo=vuedotjs)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7-red?style=flat-square&logo=redis)](https://redis.io/)
[![Java](https://img.shields.io/badge/Java-8-orange?style=flat-square&logo=openjdk)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-Apache--2.0-blue?style=flat-square)](LICENSE)

一个围绕秒杀下单链路进行高并发优化的电商项目，重点解决超卖、一人一单、请求削峰、缓存击穿与消息可靠性问题。

---

## 我的职责

- 负责秒杀下单链路开发与优化
- 编写 Lua 脚本实现库存校验、库存扣减与一人一单控制
- 基于 Redis Stream 实现异步下单与请求削峰
- 实现热点数据缓存与缓存击穿防护
- 完成部分前端页面开发与接口联调

---

## 项目亮点

> 以下每一项均对应项目中的真实实现，而非概念罗列。

### 高并发秒杀核心

| 亮点             | 实现方式                                                                                         |
| ---------------- | ------------------------------------------------------------------------------------------------ |
| **Lua 原子操作** | `seckill.lua` 在 Redis 端原子完成库存判断 + 用户防重 + 库存扣减 + 消息入队，无并发竞争           |
| **异步订单队列** | Redis Stream + Consumer Group，消费者线程独立运行，支持 Pending List 异常恢复                     |
| **全局 ID 生成** | `RedisIdWorker` 基于时间戳 + 机器 ID + 序列号生成 64 位趋势递增 ID，跨节点唯一、分库友好        |
| **分布式锁**     | Redisson + `unlock.lua`，锁持有者才能释放；`@Lazy` 自注入解决循环依赖                            |

### 高可用缓存体系

| 亮点             | 实现方式                                                                       |
| ---------------- | ------------------------------------------------------------------------------ |
| **防缓存穿透**   | `CacheClient.queryWithPassThrough()`：空值写入拦截数据库不存在的请求             |
| **防缓存击穿**   | `CacheClient.queryWithLogicalExpire()`：逻辑过期 + 互斥锁 + 异步重建          |
| **附近商铺查询** | Redis GEO 命令实现经纬度存储与距离排序，无需全表扫描                            |

### 前端工程化

| 亮点                       | 实现方式                                                                         |
| -------------------------- | -------------------------------------------------------------------------------- |
| **Pinia 统一状态**         | 全部 store 收敛到 `defineStore()`，兼容导出保证页面层 import 零改动              |
| **Element Plus 按需导入**  | `unplugin-auto-import` + `unplugin-vue-components` 实现零配置按需引入            |
| **Hash 路由**              | `createWebHashHistory`，刷新不 404，部署无需服务器配置 fallback                  |

---

## 技术难点

### 1. 秒杀超卖与一人一单

**问题**：高并发下多个请求同时读到库存 > 0，全部执行导致超发；同一用户多次提交导致重复下单。

**解法**：将库存校验、用户防重、库存扣减、消息入队全部封装进 Lua 脚本，Redis 单线程原子执行，一条请求从头到尾，无竞争窗口。

```lua
-- seckill.lua 核心逻辑
if (tonumber(redis.call('get', stockKey)) <= 0) then return 1 end  -- 库存不足
if (tonumber(redis.call('sismember', orderKey, userId)) == 1) then return 2 end  -- 重复下单
redis.call('incrby', stockKey, -1)  -- 原子扣减
redis.call('sadd', orderKey, userId)  -- 写入用户订单集合
redis.call('xadd', 'stream.orders', '*', ...)  -- 写入消息队列
return 0
```

### 2. 消息队列消费可靠性

**问题**：消费者处理消息时崩溃，已读但未确认的消息会丢失订单。

**解法**：Redis Stream 的 Pending List（PEL）记录每个消费者已读未确认的消息，宕机重启后从 PEL 重新读取并处理；配合 XACK 确认机制保证"至少消费一次"。

### 3. 缓存击穿防护

**问题**：热点数据缓存过期瞬间，所有请求同时穿透到数据库。

**解法**：逻辑过期策略（数据永不过期，字段记录过期时间），过期后抢互斥锁异步重建，其余请求直接返回旧数据。

```
请求 → Redis 命中 → 逻辑过期？ → 否 → 返回旧数据
                          ↓ 是
                    抢锁成功？ → 否 → 返回旧数据
                          ↓ 是
                    异步查 DB 写回 → 释放锁 → 返回新数据
```

> **补充实现细节**
>
> - **自循环依赖**：`@PostConstruct` 启动消费者线程，需调用同 Service 代理方法做事务入库。使用 `@Lazy` 延迟注入自身代理，运行时通过代理调用，避免循环依赖。
> - **全局 ID**：Redis 原子递增保证唯一，时间戳确保趋势递增，64 位结构兼容 Java long，适合分库分表场景。

---

## 运行结果 / 验证方式

- 秒杀请求先在 Redis 中完成原子校验，再异步落库，数据库瞬时压力大幅降低
- Lua 脚本保证库存校验、扣减与用户防重原子执行，超卖与重复下单风险降低
- Redis Stream 消费者异常后可从 Pending List 恢复处理，订单不丢失
- 热点商铺查询结合缓存与逻辑过期，高频访问不直接打到数据库

---

## 技术栈

### 后端

| 分类     | 技术                          | 说明                        |
| -------- | ----------------------------- | --------------------------- |
| 框架     | Spring Boot 2.7.4 / Spring MVC | 核心 Web 框架               |
| ORM      | MyBatis Plus 3.5.2            | CRUD 与分页                 |
| 数据库   | MySQL 8.0                     | 持久化存储                  |
| 缓存     | Redis 7 + Spring Data Redis   | 缓存、分布式锁、Stream、Geo |
| 分布式锁 | Redisson 3.17.7               | 可靠分布式锁实现             |
| 工具     | Hutool 5.8.8 / Lombok         | 工具库                      |
| 构建     | Maven                         | 依赖管理                    |

### 前端

| 分类     | 技术                                            | 说明           |
| -------- | ----------------------------------------------- | -------------- |
| 框架     | Vue 3 (Composition API)                       | 响应式 UI      |
| 构建     | Vite 8                                          | 快速构建工具   |
| 路由     | Vue Router 5                                   | SPA 路由管理   |
| 状态     | Pinia 3                                         | 统一状态管理   |
| UI       | Element Plus 2.13                             | UI 组件库      |
| 网络     | Axios                                           | HTTP 客户端    |
| 自动导入 | unplugin-auto-import / unplugin-vue-components | 零配置按需引入 |

### 部署

Docker · Docker Compose · Nginx · 多阶段镜像构建

---

## 功能模块

| 模块       | 功能                                                     |
| ---------- | -------------------------------------------------------- |
| **用户**   | 登录、登出、签到、用户信息查询                           |
| **商铺**   | 分类列表、商铺详情、商铺列表、附近商铺（Geo）            |
| **博客**   | 发布、点赞、详情、个人博客、探店笔记                     |
| **社交**   | 关注、粉丝、共同关注、关注流                             |
| **优惠券** | 普通优惠券领取、秒杀优惠券、限时抢购                     |
| **秒杀**   | Lua 原子校验、库存扣减、异步下单、Redis Stream 队列消费 |
| **图片**   | 上传、删除                                               |

---

## 项目结构

```
.
├── backend/                       # Spring Boot 后端
│   └── src/main/java/com/hmdp/
│       ├── controller/            # REST API 控制器
│       ├── service/               # 业务逻辑层
│       ├── mapper/                # MyBatis Plus 数据访问层
│       ├── entity/                # 实体类
│       ├── dto/                   # 数据传输对象
│       ├── interceptor/           # 登录拦截、Token 刷新
│       ├── config/                # Redis / MVC / 异常处理配置
│       └── utils/                 # 缓存客户端、ID 生成器、分布式锁
├── frontend/                      # Vue 3 前端
│   └── src/
│       ├── pages/                 # 页面组件（老版）
│       ├── new_pages/             # 页面组件（Element Plus 化）
│       ├── layout/                # 布局组件
│       ├── components/            # 通用业务组件
│       ├── services/              # API 封装
│       ├── stores/                # Pinia 状态管理
│       └── styles/               # 全局样式与主题
├── docker-compose.yml             # 容器编排（MySQL + Redis + App + Frontend）
└── README.md
```

---

## 快速启动

### 环境要求

- Docker 与 Docker Compose
- JDK 8+ / Node.js 20+ / Yarn

### 启动全部服务

```bash
# 启动数据库、缓存、后端、前端（自动构建）
docker compose up -d --build

# 启动完成后访问
# 前端：http://localhost:8080
# 后端：http://localhost:8081
```

### 本地开发启动

**后端：**

```bash
cd backend
# 确保 MySQL 和 Redis 已运行，修改 application-dev.yaml 中的连接信息
mvn spring-boot:run
```

**前端：**

```bash
cd frontend
yarn install
yarn dev
```

---

## 秒杀下单核心流程

```
用户请求 → Lua 脚本原子完成库存校验、用户防重、库存扣减与消息入队
       → 返回"排队中"
       → 消费者从 Redis Stream 取出消息
       → 异步创建订单 → MySQL 持久化
```

---

## 核心链路接口

| 路径                          | 说明                                                          |
| ----------------------------- | ------------------------------------------------------------- |
| `POST /voucher-order/seckill` | 秒杀下单入口，Lua 原子校验后写入 Redis Stream                  |
| `GET /shop/of/nearby`         | 基于 Redis GEO 的附近商铺查询                                 |
| `GET /shop/{id}`              | 热点商铺查询，结合逻辑过期策略防缓存击穿                       |
| `POST /user/login`            | 用户登录                                                      |
| `GET /voucher/list`           | 优惠券列表（含普通券与秒杀券）                                 |
| `POST /blog/like`             | 博客点赞                                                      |
| `POST /follow`                | 关注用户                                                      |
| `POST /upload`                | 图片上传                                                      |

> 完整接口文档可导入 `backend/db/hmdp.http` 至 IDE 或 Apifox。

---

## License

Apache License 2.0 - see [LICENSE](LICENSE)
