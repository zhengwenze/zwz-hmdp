# 代码使用说明(本项目来自b站[黑马程序员](https://space.bilibili.com/37974444)[redis教程](https://www.bilibili.com/video/BV1cr4y1671t)，仅供参考)

## 当前目录结构

- `backend/`：后端 Java 工程，包含 `pom.xml`、`src/`、Dockerfile、`db/` SQL 脚本和 Redis 配置
- 根目录：当前作为工作区根目录，保留 `docker-compose.yml`，后续可以继续新增 `frontend/`

项目代码包含2个分支：
- master : 主分支，包含完整版代码，作为大家的编码参考使用
- init (包含前端资源): 初始化分支，实战篇的初始代码，建议大家以这个分支作为自己开发的基础代码
  - 前端资源位于init分支src/main/resources/nginx-1.18.0下

视频地址:
- [黑马程序员Redis入门到实战教程，深度透析redis底层原理+redis分布式锁+企业解决方案+redis实战](https://www.bilibili.com/video/BV1cr4y1671t)
- [https://www.bilibili.com/video/BV1cr4y1671t](https://www.bilibili.com/video/BV1cr4y1671t)
  - P24起 实战篇

## 1.下载
克隆完整项目
```git
git clone https://github.com/cs001020/hmdp.git
```
切换分支
```git
git checkout init
```

## 2.常见问题
部分同学直接使用了master分支项目来启动，控制台会一直报错:
```
NOGROUP No such key 'stream.orders' or consumer group 'g1' in XREADGROUP with GROUP option
```
这是因为我们完整版代码会尝试访问Redis，连接Redis的Stream。建议同学切换到init分支来开发，如果一定要运行master分支，请先在Redis运行一下命令：
```text
XGROUP CREATE stream.orders g1 $ MKSTREAM
```

## 3.Docker 一键部署

项目已经补齐 Docker 部署配置。当前根目录负责统一编排，实际后端工程位于 `backend/`，MySQL、Redis 也会随容器一起启动并自动初始化：

- MySQL 会自动导入 `backend/db/hmdp.sql`
- Redis 会自动创建 `stream.orders` 的消费组 `g1`
- Spring Boot 会通过容器内服务名连接 MySQL 和 Redis

### 3.1 启动

在项目根目录执行：

```bash
docker compose up --build -d
```

如果你只想单独运行后端 Maven 工程：

```bash
cd backend
mvn spring-boot:run
```

启动后默认端口：

- 后端接口: `http://localhost:8081`

MySQL 和 Redis 默认只在 Docker 网络内部提供给应用使用，不再暴露到宿主机，避免与本机已有服务或其它项目容器端口冲突。

### 3.2 停止

```bash
docker compose down
```

### 3.3 连同数据一起清空

```bash
docker compose down -v
```

执行后会删除 MySQL 和 Redis 的数据卷，下次启动会重新导入 `hmdp.sql` 并重新初始化 Redis。

### 3.4 配置说明

当前默认账号密码与项目原始配置保持一致：

- MySQL root 密码: `001020`
- Redis 密码: `001020`

应用配置已支持环境变量覆盖，默认值仍兼容本地开发：

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `SERVER_PORT`
