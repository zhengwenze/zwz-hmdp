# Git 提交规范

> 本仓库统一使用 **Conventional Commits**。
> 目标：让提交记录清晰、可检索、可生成变更日志，并让 Codex、Claude Code 等 AI 代理稳定理解和执行。

---

## 1. 标准格式

```text
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

### 示例

```text
feat(user): 增加手机号验证码登录
fix(voucher-order): 修复重复下单校验失效
docs(readme): 补充本地启动说明
```

---

## 2. type 取值

仅允许使用以下类型：

| type     | 含义                         |
| -------- | ---------------------------- |
| feat     | 新增功能                     |
| fix      | 修复缺陷                     |
| docs     | 文档变更                     |
| style    | 仅格式调整，不影响功能       |
| refactor | 重构，不新增功能也不修复缺陷 |
| perf     | 性能优化                     |
| test     | 测试新增或调整               |
| build    | 构建、依赖、打包相关变更     |
| ci       | CI/CD 配置变更               |
| chore    | 杂项维护、工程性调整         |
| revert   | 回滚提交                     |

### 快速选择规则

| 场景                   | type               |
| ---------------------- | ------------------ |
| 改功能                 | feat               |
| 修 bug                 | fix                |
| 只改文档               | docs               |
| 只改测试               | test               |
| 只改格式               | style              |
| 不改行为的代码整理     | refactor           |
| 依赖、脚手架、配置调整 | build / ci / chore |

---

## 3. scope 规则

scope 可选，但推荐在能明确影响范围时填写。

### 推荐场景

- 修改了明确模块
- 只影响前端或后端某一侧
- 只涉及某个业务域

### 推荐 scope 示例

```
user, shop, shop-type, blog, follow, voucher, voucher-order, upload, frontend, backend, api, redis, docker, readme, docs
```

### 规则

- 能明确模块时，写 scope
- 不确定时，可以省略
- 不要为了凑格式乱写 scope

### 示例

```text
feat(user): 增加手机号验证码登录
fix(voucher-order): 修复重复下单校验失效
docs(api): 补充接口示例
```

---

## 4. subject 书写规则

subject 的目标是：一行说清这次提交做了什么。

### 必须遵守

- 只写"做了什么"，不要写背景故事
- 不以句号结尾
- 尽量保持单行
- 避免含糊标题
- 标题必须和提交内容一致

### 推荐写法

```text
feat(user): 增加手机号验证码登录
fix(shop): 修复商铺缓存未命中导致的空指针问题
refactor(blog): 拆分点赞逻辑与博客查询逻辑
docs(readme): 补充本地启动说明
```

### 禁止写法

```text
feat: 修改功能
fix: 修复bug
chore: update
tmp
test
```

---

## 5. body / footer 规则

body 和 footer 不是必填，但以下情况建议补充：

- 改动跨多个模块
- 改动原因不明显
- 旧行为发生变化
- 需要说明验证方式
- 需要关联 Issue / 工单
- 包含破坏性变更

### body 示例

```text
fix(voucher-order): 修复重复下单校验失效

将重复下单判断前移到 Lua 脚本阶段，避免高并发下重复创建订单。
同时保留服务层兜底校验，降低并发穿透风险。
```

### 关联 Issue / 工单

```text
Refs: #123
Closes: #123
Refs: PROJ-123
Closes: PROJ-123
```

### 破坏性变更

有不兼容修改时，必须显式标注：

```text
feat(api)!: 调整用户详情接口返回结构

BREAKING CHANGE: 用户详情接口中的 name 字段改为 username，前端需同步修改
```

**规则：**

- 有破坏性变更时，必须写 `BREAKING CHANGE:`
- 推荐同时使用 `!`

---

## 6. 一个 commit 应该提交什么

### 核心原则

一个 commit 只做一个主要逻辑变更。

### 可以放在同一个 commit 的情况

- 同一功能的前后端联动修改
- 同一缺陷修复涉及多个文件
- 同一接口契约调整引起的配套更新

### 必须拆分的情况

- 功能修改 + 纯格式化
- 缺陷修复 + 大范围重构
- 接口变更 + 无关页面美化
- 依赖升级 + 业务逻辑修改
- 文档更新 + 无关代码修改

---

## 7. AI 代理专用规则

对于 Codex、Claude Code 等 AI 代理，默认要求如下：

### 7.1 先判断"本次改动的主要目的"

按"主要目的"选择 type，不要因为顺手改了别的内容就换 type。

| 场景                     | type               |
| ------------------------ | ------------------ |
| 主要是新增能力           | feat               |
| 主要是修问题             | fix                |
| 主要是整理代码但不改行为 | refactor           |
| 主要是改文档             | docs               |
| 主要是改测试             | test               |
| 主要是改格式             | style              |
| 主要是工程维护           | build / ci / chore |

### 7.2 不要把多个无关修改合并成一个提交

如果同时做了以下无关事情，应拆成多个 commit：

- 文档更新
- 业务逻辑修改
- 依赖升级
- 纯格式化
- 重构整理

### 7.3 scope 的处理规则

- 能明确模块时写 scope
- 不确定时省略 scope
- 不要乱写一个模糊 scope

### 7.4 破坏性变更必须显式说明

如果修改会导致旧接口、旧行为、旧配置不兼容，必须写：

```text
BREAKING CHANGE:
```

### 7.5 提交前最低检查

提交前至少确认：

- [ ] 提交内容与标题一致
- [ ] 代码可运行、可构建，或满足仓库最低校验要求
- [ ] 不包含密钥、token、私钥、数据库账号等敏感信息
- [ ] 不提交无关大文件、构建产物、临时文件
- [ ] 不提交明显未完成且无说明的临时代码

---

## 8. 推荐示例

### 新功能

```text
feat(user): 增加手机号验证码登录
```

### 修复缺陷

```text
fix(voucher-order): 修复重复下单校验失效
```

### 文档更新

```text
docs(readme): 补充本地启动说明
```

### 重构

```text
refactor(shop): 拆分商铺缓存与数据库查询逻辑
```

### 工程维护

```text
chore(deps): 升级 Redis 客户端版本
```

### 破坏性变更

```text
feat(api)!: 调整用户信息返回字段

BREAKING CHANGE: 用户详情接口中的 name 字段已重命名为 username，前端需同步调整
```

---

## 9. 禁止事项

### 禁止使用的提交信息

```text
update
fix bug
tmp
test
其他无法体现改动目的的标题
```

### 禁止出现的提交方式

- 一个 commit 混合多个无关变更
- 将格式化、重构、功能修改混成一条提交
- 提交敏感信息、私有配置、无关大文件
- 提交明显未完成且无说明的代码
