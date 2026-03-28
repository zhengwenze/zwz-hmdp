# Git 提交规范

## 1. 格式规范

遵循 **Angular 提交规范**，提交信息由 Header、Body、Footer 三部分组成。

### 1.1 结构

```
<type>(<scope>): <subject>
<body>
<footer>
```

### 1.2 Header（必填）

| 字段      | 说明                                                             |
| --------- | ---------------------------------------------------------------- |
| `type`    | 类型，限用下方标识                                               |
| `scope`   | 范围（可选），如 `user`、`order`、`global`                       |
| `subject` | 简短描述，不超过50字符，动词开头，第一人称现在时，小写，不加句号 |

**type 类型：**

| 类型       | 说明                   |
| ---------- | ---------------------- |
| `feat`     | 新功能                 |
| `fix`      | 修复 Bug               |
| `docs`     | 文档变更               |
| `style`    | 代码格式（不影响功能） |
| `refactor` | 重构                   |
| `perf`     | 性能优化               |
| `test`     | 测试相关               |
| `build`    | 构建相关               |
| `ci`       | CI 配置                |
| `chore`    | 其他变更               |
| `revert`   | 回滚                   |

### 1.3 Body（可选）

- 说明代码变动动机及与以前行为的对比
- 使用第一人称现在时，可分多行

### 1.4 Footer（可选）

- **不兼容变动**：`BREAKING CHANGE: 描述`
- **关闭 Issue**：`Closes #123` 或 `Closes PROJ-123`

---

## 2. 提交示例

### 新功能

```
feat(user): 新增手机号登录功能
1. 集成短信验证码 SDK
2. 添加验证码校验逻辑
3. 调整登录页面 UI 布局
Closes #JIRA-1024
```

### 修复 Bug

```
fix(order): 修复订单支付金额精度丢失问题
将支付金额计算逻辑由 Float 改为 BigDecimal，解决浮点数计算精度问题。
```

### 破坏性更新

```
refactor(api): 重构用户接口返回结构
BREAKING CHANGE: /api/v1/user 返回字段由 `name` 改为 `username`，前端需同步修改。
```

---

## 3. 分支管理

| 分支              | 说明                                    |
| ----------------- | --------------------------------------- |
| `master` / `main` | 主分支（受保护，禁止直接提交）          |
| `develop`         | 开发分支                                |
| `feature/*`       | 功能分支，如 `feature/user-login`       |
| `bugfix/*`        | Bug 修复分支                            |
| `hotfix/*`        | 紧急修复分支，如 `hotfix/payment-error` |
| `release/*`       | 预发布分支，如 `release/v1.2.0`         |

---

## 4. MR/PR 规范

- 必须通过 MR/PR 合并代码，禁止直接 push 到受保护分支
- MR 标题需符合 Commit Header 规范
- 描述需包含：变更原因、测试情况、影响范围、截图（如有 UI 变更）
- 至少 1-2 位开发人员 Approve 后方可合并
- 必须通过 CI 自动化测试和 Lint 检查

---

## 5. 工具配置（推荐）

### Commitlint + Husky

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
    "subject-full-stop": [2, "never", "."],
    "subject-case": [0],
  },
};
```

**配置 Husky：**

```bash
npx husky add .husky/commit-msg 'npx --no-install commitlint --edit "$1"'
```

---

## 6. 禁忌

- ❌ 禁止无意义 Message（如 `update`、`fix bug`、`tmp`）
- ❌ 禁止提交超大文件（大型压缩包、`node_modules`、IDE 配置）
- ❌ 禁止强制 push 到公共分支（`master`、`develop`）
- ❌ 禁止一个 Commit 混合多个不相关变更
- ❌ 严禁提交编译不通过或导致功能崩溃的代码

---

## 7. 自动生成 CHANGELOG

```bash
npm install standard-version --save-dev
npm run release
```
