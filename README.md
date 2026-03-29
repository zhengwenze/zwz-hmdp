# 高并发电商秒杀平台

## 项目简介

本项目是一个前后端分离的高并发电商秒杀平台，包含商铺查询、优惠券管理、博客探店、关注关系、图片上传、用户登录与秒杀下单等能力。

## 技术栈

### 后端

- Java 8
- Spring Boot 2.7.4
- Spring MVC
- Spring Data Redis
- MyBatis Plus 3.5.2
- MySQL 8.0
- Redis 7
- Redisson 3.17.7
- Hutool 5.8.8
- Lombok
- Maven

### 前端

- Vue 3
- Vite 8
- Vue Router 5
- Pinia 3
- Element Plus 2.13
- Axios
- unplugin-auto-import
- unplugin-vue-components
- JavaScript
- CSS

### 中间件与高并发相关能力

- Redis 缓存
- Redis GEO 附近商铺查询
- Redis Stream 异步下单队列
- Lua 脚本秒杀校验与原子操作
- Redisson 分布式锁
- 自定义 Redis 全局 ID 生成器
- 缓存穿透处理
- 缓存击穿处理（逻辑过期 + 异步重建）

### 工程化与部署

- Docker
- Docker Compose
- Nginx
- Node.js 20
- Yarn
- Eclipse Temurin JDK 8 / JRE 8
- 多阶段镜像构建

## 当前项目实际使用到的模块

- 用户登录、登出、签到、用户信息查询
- 商铺列表、分类、详情、附近商铺查询
- 博客发布、点赞、详情、个人博客、关注流
- 关注与共同关注
- 普通优惠券与秒杀优惠券
- 秒杀下单与异步订单处理
- 图片上传与删除
