# 高并发电商秒杀平台

**后端技术栈：** Java、Spring Boot、Spring Data Redis、MyBatis Plus、Redisson、MySQL、Redis、Redis Stream、Lombok、Hutool

**前端技术栈：** Vue、Vite、JS、CSS

**基础设施：** Docker、Docker Compose、Maven

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
## 后端接口调用文档

### 服务信息

- Base URL：`http://localhost:8081`
- 接口前缀：无统一 `/api` 前缀，以下路径均直接挂在根路径
- 默认响应类型：`application/json`
- 时间字段：后端使用 `LocalDateTime`，JSON 中通常是 `2022-10-09T00:00:00`
- 常见分页：`current` 默认 `1`，单页大小实际固定为 `10`

### 鉴权说明

- 登录成功后，`/user/login` 的 `data` 会返回 token 字符串
- 后续请求把 token 放到请求头：`authorization: <token>`
- 未登录访问受保护接口时，后端直接返回 HTTP `401`
- 免登录接口：
  - `/user/code`
  - `/user/login`
  - `/blog/hot`
  - `/shop/**`
  - `/shop-type/**`
  - `/upload/**`
  - `/voucher/**`
- 其余接口默认需要登录

### 通用返回结构

```json
{
  "success": true,
  "errorMsg": null,
  "data": {},
  "total": null
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `success` | `boolean` | 是否成功 |
| `errorMsg` | `string \| null` | 失败时的错误信息 |
| `data` | `any` | 真实业务数据 |
| `total` | `number \| null` | 仅少数分页接口可能使用，本项目大多数接口为 `null` |

### 前端对接注意

- `Blog.images`、`Shop.images` 都是逗号分隔字符串，不是数组
- `Blog.name`、`Blog.icon`、`Blog.isLike` 是后端查询时动态补充字段
- `Shop.distance` 只有按坐标查询商铺时才会返回
- `Voucher.stock`、`Voucher.beginTime`、`Voucher.endTime` 只在券列表接口里用于秒杀券展示
- `Result.ok()` 返回时 `data` 为 `null`
- 部分“空结果”返回 `[]`，部分返回 `null`，下文已按实际行为标注

### 接口总览

> 以下“接口数”按 `HTTP 方法 + 路径` 统计。

| 模块 | 基础路径 | 接口数 | 默认鉴权 |
| --- | --- | ---: | --- |
| 用户 | `/user` | 8 | 部分免登录 |
| 商铺 | `/shop` | 5 | 免登录 |
| 商铺分类 | `/shop-type` | 1 | 免登录 |
| 博客 | `/blog` | 8 | 仅 `/blog/hot` 免登录 |
| 关注 | `/follow` | 3 | 需要登录 |
| 优惠券 | `/voucher` | 3 | 免登录 |
| 秒杀下单 | `/voucher-order` | 1 | 需要登录 |
| 上传 | `/upload` | 2 | 免登录 |

### 用户接口 `user`

| 方法 | 路径 | 需要登录 | 主要参数 | 返回 `data` | 说明 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/user/code` | 否 | `Form/Query: phone` | `null` | 发送 6 位验证码，手机号不合法会返回失败 |
| `POST` | `/user/login` | 否 | `Body: { phone, code, password }` | `string` | 返回 token。当前实现只校验 `phone + code`，`password` 未实际使用 |
| `POST` | `/user/logout` | 是 | 无 | `null` | 接口已暴露，但当前固定返回失败：`功能未完成` |
| `GET` | `/user/me` | 是 | 无 | `UserDTO` | 获取当前登录用户 |
| `GET` | `/user/info/{id}` | 是 | `Path: id` | `UserInfo \| null` | 查询用户详情，不存在时返回 `null` |
| `GET` | `/user/{id}` | 是 | `Path: id` | `UserDTO \| null` | 查询用户基础信息，不存在时返回 `null` |
| `POST` | `/user/sign` | 是 | 无 | `null` | 今日签到 |
| `GET` | `/user/sign/count` | 是s | 无 | `number` | 返回当月截至今天的连续签到天数 |

#### 登录最小示例

```http
POST /user/login
Content-Type: application/json

{
  "phone": "13800138000",
  "code": "123456"
}
```

```json
{
  "success": true,
  "data": "0f4c0f8f0b5c4c6b9f54b2a1b3e8d123"
}
```

### 商铺接口 `shop`

| 方法 | 路径 | 登录 | 主要参数 | 返回 `data` | 说明 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/shop/{id}` | 否 | `Path: id` | `Shop` | 查单个商铺，找不到时返回失败：`店铺不存在` |
| `POST` | `/shop` | 否 | `Body: Shop` | `null` | 新增商铺。成功后不返回新商铺 ID |
| `PUT` | `/shop` | 否 | `Body: Shop` | `null` | 更新商铺，`id` 不能为空 |
| `GET` | `/shop/of/type` | 否 | `Query: typeId, current=1, x?, y?` | `Shop[] \| null` | 不传坐标时普通分页；传 `x/y` 时按 GEO 距离排序并补 `distance` |
| `GET` | `/shop/of/name` | 否 | `Query: name?, current=1` | `Shop[]` | 按名称模糊搜索 |

#### 商铺按类型查询最小示例

```http
GET /shop/of/type?typeId=1&current=1&x=120.149192&y=30.316078
```

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "103茶餐厅",
      "typeId": 1,
      "distance": 12.3
    }
  ]
}
```

### 商铺分类接口 `shop-type`

| 方法 | 路径 | 登录 | 主要参数 | 返回 `data` | 说明 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/shop-type/list` | 否 | 无 | `ShopType[]` | 按 `sort` 升序返回 |

### 博客接口 `blog`

| 方法 | 路径 | 登录 | 主要参数 | 返回 `data` | 说明 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/blog` | 是 | `Body: Blog` | `number` | 发布探店笔记，返回博客 ID |
| `PUT` | `/blog/like/{id}` | 是 | `Path: id` | `null` | 点赞/取消点赞，同一路径切换状态 |
| `GET` | `/blog/of/me` | 是 | `Query: current=1` | `Blog[]` | 当前登录用户的笔记 |
| `GET` | `/blog/hot` | 否 | `Query: current=1` | `Blog[]` | 热门笔记。若携带 token，会补充 `isLike` |
| `GET` | `/blog/{id}` | 是 | `Path: id` | `Blog` | 查询笔记详情，不存在时返回失败：`博客不存在` |
| `GET` | `/blog/likes/{id}` | 是 | `Path: id` | `UserDTO[]` | 点赞用户列表，当前实现最多返回 7 条 |
| `GET` | `/blog/of/user` | 是 | `Query: id, current=1` | `Blog[]` | 查看某个用户的笔记 |
| `GET` | `/blog/of/follow` | 是 | `Query: lastId, offset=0` | `ScrollResult \| null` | 关注推送流，当前每次最多拉取 2 条 |

#### 发布博客最小示例

```http
POST /blog
authorization: <token>
Content-Type: application/json

{
  "shopId": 1,
  "title": "周末探店",
  "images": "/imgs/1.jpg,/imgs/2.jpg",
  "content": "环境很好，值得再来。"
}
```

```json
{
  "success": true,
  "data": 17
}
```

#### 关注推送流最小示例

```http
GET /blog/of/follow?lastId=1711500000000&offset=0
authorization: <token>
```

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "id": 17,
        "title": "周末探店"
      }
    ],
    "minTime": 1711499999000,
    "offset": 1
  }
}
```

### 关注接口 `follow`

| 方法 | 路径 | 登录 | 主要参数 | 返回 `data` | 说明 |
| --- | --- | --- | --- | --- | --- |
| `PUT` | `/follow/{id}/{isFollow}` | 是 | `Path: id, isFollow` | `null` | `isFollow=true` 表示关注，`false` 表示取关 |
| `GET` | `/follow/or/not/{id}` | 是 | `Path: id` | `boolean` | 是否已关注该用户 |
| `GET` | `/follow/common/{id}` | 是 | `Path: id` | `UserDTO[]` | 共同关注列表，空时返回 `[]` |

### 优惠券接口 `voucher`

| 方法 | 路径 | 登录 | 主要参数 | 返回 `data` | 说明 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/voucher` | 否 | `Body: Voucher` | `number` | 新增普通券，返回券 ID |
| `POST` | `/voucher/seckill` | 否 | `Body: Voucher` | `number` | 新增秒杀券，返回券 ID，需同时传 `stock / beginTime / endTime` |
| `GET` | `/voucher/list/{shopId}` | 否 | `Path: shopId` | `Voucher[]` | 查询店铺可用券列表；秒杀券会混合返回 `stock / beginTime / endTime` |

### 秒杀下单接口 `voucher-order`

| 方法 | 路径 | 登录 | 主要参数 | 返回 `data` | 说明 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/voucher-order/seckill/{id}` | 是 | `Path: id` | `number` | 发起秒杀下单，成功返回订单 ID；失败常见文案：`库存不足`、`禁止重复下单` |

#### 秒杀下单最小示例

```http
POST /voucher-order/seckill/10
authorization: <token>
```

```json
{
  "success": true,
  "data": 1624774384923137
}
```

### 上传接口 `upload`

| 方法 | 路径 | 登录 | 主要参数 | 返回 `data` | 说明 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/upload/blog` | 否 | `FormData: file` | `string` | 上传博客图片，返回文件相对路径，如 `/blogs/1/2/uuid.png` |
| `GET` | `/upload/blog/delete` | 否 | `Query: name` | `null` | 删除博客图片，`name` 必须是返回的文件名/相对路径 |

#### 图片上传最小示例

```http
POST /upload/blog
Content-Type: multipart/form-data

file=<二进制文件>
```

```json
{
  "success": true,
  "data": "/blogs/7/14/4771fefb-1a87-4252-816c-9f7ec41ffa4a.jpg"
}
```

### 关键数据结构速查

#### `UserDTO`

```json
{
  "id": 1,
  "nickName": "小鱼同学",
  "icon": "/imgs/blogs/blog1.jpg"
}
```

#### `ScrollResult`

```json
{
  "list": [],
  "minTime": 1711499999000,
  "offset": 1
}
```

#### `Voucher` 列表中的秒杀字段

```json
{
  "id": 10,
  "shopId": 1,
  "title": "100元代金券",
  "type": 1,
  "stock": 100,
  "beginTime": "2022-10-09T00:00:00",
  "endTime": "2022-10-10T20:00:00"
}
```

### 已知坑点与未完成项

- `BlogCommentsController` 已存在，但当前没有任何可调用接口
- `/user/logout` 当前未实现，调用后会返回：

```json
{
  "success": false,
  "errorMsg": "功能未完成"
}
```

- `/user/login` 虽然请求体里有 `password` 字段，但当前代码只校验验证码登录
- `/shop/of/type` 在坐标分页模式下，如果超过可分页范围，后端可能返回 `data: null`，不是空数组
- `/blog/of/follow` 没有传统 `page/size`，要使用 `lastId + offset` 做滚动分页
- 图片上传接口只返回相对文件路径，前端展示图片时需要按实际部署环境拼接静态资源访问地址
