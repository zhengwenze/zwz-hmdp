# Git 提交规范

> 本仓库统一使用 **Conventional Commits** 提交规范。
> 目标：让提交记录清晰、可检索、可生成变更日志，并让 Codex、Claude Code 等代理工具可以稳定理解和执行。

---

## 目录

- [1. 提交格式](#1-提交格式)
- [2. type 类型](#2-type-类型)
- [3. scope 范围](#3-scope-范围)
- [4. subject 书写规则](#4-subject-书写规则)
- [5. body 书写规则](#5-body-书写规则)
- [6. footer 书写规则](#6-footer-书写规则)
- [7. 一个 commit 应该提交什么](#7-一个-commit-应该提交什么)
- [8. 代理工具提交规则](#8-代理工具提交规则)
- [9. 提交前最低要求](#9-提交前最低要求)
- [10. PR / MR 规范](#10-pr--mr-规范)
- [11. 分支规范](#11-分支规范)
- [12. 推荐示例](#12-推荐示例)
- [13. 推荐工具配置](#13-推荐工具配置)
- [14. 禁忌](#14-禁忌)

---

## 1. 提交格式

### 1.1 格式结构

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

### 1.2 各部分说明

| 部分      | 必填 | 说明                                 |
| --------- | ---- | ------------------------------------ |
| `type`    | ✅   | 表示本次提交的主要类型               |
| `scope`   | ❌   | 表示影响范围或模块                   |
| `subject` | ✅   | 简洁说明本次提交"做了什么"           |
| `body`    | ❌   | 补充"为什么改""怎么改""影响什么"     |
| `footer`  | ❌   | 用于破坏性变更、关联 Issue、关联工单 |

---

## 2. type 类型

> 仅允许使用以下类型：

| type       | 含义                         |
| ---------- | ---------------------------- |
| `feat`     | 新增功能                     |
| `fix`      | 修复缺陷                     |
| `docs`     | 文档变更                     |
| `style`    | 仅代码格式调整，不影响功能   |
| `refactor` | 重构，不新增功能也不修复缺陷 |
| `perf`     | 性能优化                     |
| `test`     | 测试新增或调整               |
| `build`    | 构建、依赖、打包相关变更     |
| `ci`       | CI/CD 配置变更               |
| `chore`    | 杂项维护、工程性调整         |
| `revert`   | 回滚提交                     |

---

## 3. scope 范围

### 3.1 推荐填写场景

scope 可选，但**推荐**在以下情况填写：

- 涉及明确模块
- 涉及前后端某一侧
- 仅修改某个业务域

> ⚠️ 提交标题如果没有 scope 会变得模糊，难以快速识别影响范围。

### 3.2 推荐 scope 示例

```
user, shop, shop-type, blog, follow, voucher, voucher-order,
upload, frontend, backend, api, redis, docker, docs
```

### 3.3 示例

```text
feat(user): 增加手机号验证码登录
fix(voucher-order): 修复重复下单校验失效
docs(api): 补充博客接口示例
```

---

## 4. subject 书写规则

> subject 的目标：**一行说清这次提交做了什么**

### 4.1 基本规则

- 使用中文或英文均可，但不要故意混杂无意义中英拼接
- 只写"做了什么"，不要写背景故事
- **不以句号结尾**
- 尽量保持单行，建议控制在 50–72 字符内
- 避免含糊表达
- 避免无意义标题

### 4.2 推荐 vs 不推荐

**✅ 推荐写法：**

```text
feat(user): 增加手机号验证码登录
fix(shop): 修复商铺缓存未命中导致的空指针问题
refactor(blog): 拆分点赞逻辑与博客查询逻辑
docs(readme): 补充本地启动说明
```

**❌ 不推荐写法：**

```text
feat: 修改功能
fix: 修复bug
chore: update
tmp
test
```

---

## 5. body 书写规则

body 不是必填，但以下情况**建议补充**：

- 改动跨多个模块
- 改动原因不明显
- 改动影响旧行为
- 改动涉及兼容性
- 改动需要说明验证方式

### 5.1 建议内容

```
为什么改
关键实现方式
与旧行为相比有什么变化
是否需要前端 / 后端 / 运维同步处理
```

### 5.2 示例

```text
fix(voucher-order): 修复重复下单校验失效

将重复下单判断前移到 Lua 脚本阶段，避免高并发下重复创建订单。
同时保留服务层兜底校验，降低并发穿透风险。
```

---

## 6. footer 书写规则

### 6.1 关联 Issue / 工单

可以使用以下格式：

```
Refs: #123
Closes: #123
Refs: PROJ-123
Closes: PROJ-123
```

| 标记      | 含义           |
| --------- | -------------- |
| `Refs:`   | 关联，但不关闭 |
| `Closes:` | 修复完成并关闭 |

### 6.2 破坏性变更

如果提交包含不兼容修改，**必须显式标注**。

**推荐写法一：**

```text
feat(api)!: 调整用户详情接口返回结构

BREAKING CHANGE: 用户详情接口中的 name 字段改为 username，前端需同步修改
```

**推荐写法二：**

```text
refactor(api): 调整用户详情接口返回结构

BREAKING CHANGE: 用户详情接口中的 name 字段改为 username，前端需同步修改
```

> 📌 **规则：**
>
> - 有破坏性变更时，必须写 `BREAKING CHANGE:`
> - 推荐同时使用 `!`，让工具和人都能快速识别

---

## 7. 一个 commit 应该提交什么

### 7.1 基本原则

> 一个 commit 只做**一个主要逻辑变更**。

**✅ 允许：**

- 一个完整功能的前后端同步修改
- 一个缺陷修复涉及多个文件
- 一个 API 调整同时修改后端、前端、文档

**❌ 不允许：**

- 把多个无关功能混在一个 commit
- 把纯格式化和业务修改混在一起
- 把顺手做的重构塞进功能提交
- 把文档更新、依赖升级、业务代码修改混在一个 commit

### 7.2 拆分标准

**应该拆成多个 commit 的情况：**

| 场景                    | 说明                 |
| ----------------------- | -------------------- |
| 业务功能 + 纯格式化     | 格式化应该单独提交   |
| 缺陷修复 + 大范围重构   | 重构应该单独提交     |
| 接口变更 + 无关页面美化 | 美化应该单独提交     |
| 依赖升级 + 业务逻辑修改 | 依赖升级应该单独提交 |
| 文档补充 + 代码逻辑变更 | 文档应该单独提交     |

**可以放同一个 commit 的情况：**

- 同一功能的前后端联动修改
- 同一缺陷修复涉及 controller/service/mapper/frontend
- 同一接口契约调整引起的配套更新

---

## 8. 代理工具提交规则

对于 Codex、Claude Code 等代理工具，默认要求如下：

| 情况                       | 使用 type                |
| -------------------------- | ------------------------ |
| 只改文档                   | `docs`                   |
| 只改测试                   | `test`                   |
| 只改格式                   | `style`                  |
| 功能新增                   | `feat`                   |
| 缺陷修复                   | `fix`                    |
| 不改变功能的代码整理       | `refactor`               |
| 依赖、脚手架、工程维护调整 | `build`、`ci` 或 `chore` |

**其他要求：**

- 不要把多个无关修改合并成一个提交
- 若本次修改包含破坏性变更，**必须**增加 `BREAKING CHANGE:` 说明
- 若无法判断 scope，可省略 scope；不要为了凑格式乱写 scope

---

## 9. 提交前最低要求

提交前至少应满足以下要求：

- [ ] 提交内容与标题一致
- [ ] 代码处于可运行、可构建、或满足仓库最低校验要求的状态
- [ ] 不包含密钥、凭证、token、私钥、数据库账号等敏感信息
- [ ] 不提交无关的大文件、压缩包、构建产物
- [ ] 不提交 `node_modules`、IDE 私有配置、临时文件
- [ ] 不提交明显无意义或未完成的临时代码，除非任务明确要求

---

## 10. PR / MR 规范

- 通过 PR / MR 合并代码，**禁止直接推送**到受保护分支
- PR / MR 标题应符合 Commit Header 规范
- 描述建议包含：
  - 变更原因
  - 变更内容
  - 测试情况
  - 影响范围
  - 截图或录屏（如有 UI 变更）
- 合并前应通过项目要求的自动化检查
- 至少满足团队既定的 Review 要求后再合并

---

## 11. 分支规范

### 11.1 分支命名

| 分支              | 说明                             |
| ----------------- | -------------------------------- |
| `main` / `master` | 主分支，**受保护**，禁止直接提交 |
| `develop`         | 开发分支                         |
| `feature/*`       | 功能开发分支                     |
| `bugfix/*`        | 常规缺陷修复分支                 |
| `hotfix/*`        | 紧急线上修复分支                 |
| `release/*`       | 预发布分支                       |

### 11.2 示例

```text
feature/user-login
bugfix/voucher-order-repeat
hotfix/payment-timeout
release/v1.2.0
```

---

## 12. 推荐示例

### 12.1 新功能

```text
feat(user): 增加手机号验证码登录

补充验证码校验流程，并在登录成功后返回 token
Closes: PROJ-1024
```

### 12.2 修复缺陷

```text
fix(voucher-order): 修复重复下单校验失效

将重复下单判断前移到 Lua 脚本阶段，避免高并发下重复创建订单
```

### 12.3 文档更新

```text
docs(api): 补充用户登录和秒杀下单接口示例
```

### 12.4 重构

```text
refactor(shop): 拆分商铺缓存与数据库查询逻辑
```

### 12.5 性能优化

```text
perf(shop): 优化商铺分页查询的 Redis 命中逻辑
```

### 12.6 破坏性变更

```text
feat(api)!: 调整用户信息返回字段

BREAKING CHANGE: 用户详情接口中的 name 字段已重命名为 username，前端需同步调整
```

---

## 13. 推荐工具配置

### 13.1 commitlint

**安装：**

```bash
npm install @commitlint/cli @commitlint/config-conventional husky --save-dev
```

**commitlint.config.js：**

```javascript
module.exports = {
  extends: ["@commitlint/config-conventional"],
  rules: {
    "type-enum": [
      2,
      "always",
      [
        "feat",
        "fix",
        "docs",
        "style",
        "refactor",
        "perf",
        "test",
        "build",
        "ci",
        "chore",
        "revert",
      ],
    ],
    "type-empty": [2, "never"],
    "subject-empty": [2, "never"],
    "subject-full-stop": [2, "never", "."],
    "subject-case": [0],
    "header-max-length": [2, "always", 100],
  },
};
```

### 13.2 Husky

**配置 commit-msg 钩子：**

```bash
npx husky add .husky/commit-msg 'npx --no-install commitlint --edit "$1"'
```

---

## 14. 禁忌

> 以下行为**禁止出现**：

- 使用无意义提交信息，如 `update`、`fix bug`、`tmp`
- 一个 commit 混合多个无关变更
- 将格式化、重构、功能修改混成一条提交
- 强制 push 到公共主干分支
- 提交无法运行、明显破坏功能、未完成且无说明的代码
- 提交敏感信息、私密配置、无关大文件
