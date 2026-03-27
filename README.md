# hmdp - 高并发电商秒杀平台

## 项目结构

```
hmdp/
├── backend/                          # 后端工程
│   ├── src/main/java/com/hmdp/       # Java 源码
│   │   ├── config/                   # 配置类
│   │   │   ├── MvcConfig.java
│   │   │   ├── MybatisConfig.java
│   │   │   ├── RedissonConfig.java
│   │   │   └── WebExceptionAdvice.java
│   │   ├── controller/               # 控制器层
│   │   ├── dto/                      # 数据传输对象
│   │   ├── entity/                   # 实体类
│   │   ├── interceptor/             # 拦截器
│   │   ├── mapper/                  # MyBatis Mapper 接口
│   │   ├── service/                 # 服务层
│   │   │   └── impl/                # 服务实现类
│   │   └── utils/                   # 工具类
│   │   └── HmDianPingApplication.java
│   ├── src/main/resources/          # 资源配置
│   │   ├── mapper/                  # MyBatis XML 映射文件
│   │   ├── application.yaml         # 应用配置
│   │   ├── seckill.lua              # 秒杀 Lua 脚本
│   │   └── unlock.lua               # 解锁 Lua 脚本
│   ├── src/test/java/               # 测试代码
│   ├── db/                          # 数据库脚本
│   │   └── hmdp.sql
│   ├── docker/                      # Docker 配置
│   │   └── redis/
│   │       └── redis.conf
│   ├── pom.xml                      # Maven 配置
│   └── Dockerfile                   # 后端镜像构建
├── docker-compose.yml               # Docker Compose 编排文件
└── README.md
```
