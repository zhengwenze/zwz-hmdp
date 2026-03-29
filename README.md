# 高并发电商秒杀平台

<!-- badges -->

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.4-green?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3-blue?style=flat-square&logo=vuedotjs)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7-red?style=flat-square&logo=redis)](https://redis.io/)
[![Java](https://img.shields.io/badge/Java-8-orange?style=flat-square&logo=openjdk)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-Apache--2.0-blue?style=flat-square)](LICENSE)

前后端分离的高并发电商秒杀系统，基于 Redis 实现高并发流量控制，涵盖用户、商铺、优惠券、博客探店、社交关注等核心业务场景。

---

## 特性亮点

- **异步下单**：Redis Stream 消息队列实现秒杀订单异步处理，流量削峰
- **Lua 原子操作**：秒杀校验与库存扣减在 Lua 脚本中完成，保证原子性避免超卖
- **分布式锁**：Redisson 实现可靠分布式锁，保护临界资源
- **高并发缓存**：多级缓存策略 + 逻辑过期 + 异步重建，防止缓存击穿与穿透
- **全局 ID**：基于 Redis 的高性能分布式 ID 生成器
- **GEO 查询**：基于 Redis GEO 实现附近商铺查询
- **前端状态管理**：Pinia 统一管理全店状态，Vue 3 Composition API

---

## 技术栈

### 后端

| 分类     | 技术                           | 说明                        |
| -------- | ------------------------------ | --------------------------- |
| 框架     | Spring Boot 2.7.4 / Spring MVC | 核心 Web 框架               |
| ORM      | MyBatis Plus 3.5.2             | CRUD 与分页                 |
| 数据库   | MySQL 8.0                      | 持久化存储                  |
| 缓存     | Redis 7 + Spring Data Redis    | 缓存、分布式锁、Stream、Geo |
| 分布式锁 | Redisson 3.17.7                | 可靠分布式锁实现            |
| 工具     | Hutool 5.8.8 / Lombok          | 工具库                      |
| 构建     | Maven                          | 依赖管理                    |

### 前端

| 分类     | 技术                                           | 说明           |
| -------- | ---------------------------------------------- | -------------- |
| 框架     | Vue 3 (Composition API)                        | 响应式 UI      |
| 构建     | Vite 8                                         | 快速构建工具   |
| 路由     | Vue Router 5                                   | SPA 路由管理   |
| 状态     | Pinia 3                                        | 统一状态管理   |
| UI       | Element Plus 2.13                              | UI 组件库      |
| 网络     | Axios                                          | HTTP 客户端    |
| 自动导入 | unplugin-auto-import / unplugin-vue-components | 零配置按需引入 |

### 部署

Docker · Docker Compose · Nginx · 多阶段镜像构建

---

## 功能模块

| 模块       | 功能                                                    |
| ---------- | ------------------------------------------------------- |
| **用户**   | 登录、登出、签到、用户信息查询                          |
| **商铺**   | 分类列表、商铺详情、商铺列表、附近商铺（Geo）           |
| **博客**   | 发布、点赞、详情、个人博客、探店笔记                    |
| **社交**   | 关注、粉丝、共同关注、关注流                            |
| **优惠券** | 普通优惠券领取、秒杀优惠券、限时抢购                    |
| **秒杀**   | Lua 原子校验、库存扣减、异步下单、Redis Stream 队列消费 |
| **图片**   | 上传、删除                                              |

---

## 项目结构

```
.
├── backend/                    # Spring Boot 后端
│   └── src/main/java/com/hmdp/
│       ├── controller/         # REST API 控制器
│       ├── service/           # 业务逻辑层
│       ├── mapper/            # MyBatis Plus 数据访问层
│       ├── entity/            # 实体类
│       ├── dto/               # 数据传输对象
│       ├── interceptor/       # 登录拦截、Token 刷新
│       ├── config/            # Redis / MVC / 异常处理配置
│       └── utils/             # 缓存客户端、ID 生成器、分布式锁
├── frontend/                   # Vue 3 前端
│   └── src/
│       ├── pages/             # 页面组件（老版）
│       ├── new_pages/         # 页面组件（Element Plus 化）
│       ├── layout/            # 布局组件
│       ├── components/        # 通用业务组件
│       ├── services/          # API 封装
│       ├── stores/            # Pinia 状态管理
│       └── styles/            # 全局样式与主题
├── docker-compose.yml          # 容器编排（MySQL + Redis + App + Frontend）
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

## 高并发核心设计

### 秒杀下单流程

```
用户请求 → Lua 脚本原子校验（验证码 + 库存 + 每人限单）
       → 库存预扣（Redis DECR）
       → 写入 Redis Stream 消息队列
       → 立即返回"排队中"
       → 消费者异步下单 → MySQL 持久化
```

### 缓存策略

| 场景     | 策略                          |
| -------- | ----------------------------- |
| 商铺分类 | Cache-Aside + 定时刷新        |
| 商铺详情 | 逻辑过期 + 异步重建（防击穿） |
| 验证码   | 缓存穿透：存空值拦截          |
| 秒杀库存 | Lua 脚本原子扣减              |

---

## API 概览

| 路径                          | 说明       |
| ----------------------------- | ---------- |
| `POST /user/login`            | 用户登录   |
| `GET /shop/list`              | 商铺列表   |
| `GET /shop/of/nearby`         | 附近商铺   |
| `GET /blog/of/user`           | 用户博客   |
| `POST /blog/like`             | 点赞       |
| `POST /follow`                | 关注用户   |
| `GET /voucher/list`           | 优惠券列表 |
| `POST /voucher-order/seckill` | 秒杀下单   |
| `POST /upload`                | 图片上传   |

> 完整接口文档可导入 `backend/db/hmdp.http` 至 IDE 或 Apifox。

---

## License

Apache License 2.0 - see [LICENSE](LICENSE)
