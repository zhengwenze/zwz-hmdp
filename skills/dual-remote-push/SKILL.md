---
name: dual-remote-push
description: "将本地代码提交并同步推送到 Gitee 和 GitHub 双仓库，确保两个远程仓库保持一致。适用于需要同时维护 Gitee 和 GitHub 镜像仓库的场景。"
---

# 双仓库同步推送技能

## 描述

本技能用于将本地代码提交并同步推送到两个配置好的远程仓库：

- Gitee: `origin` -> `https://gitee.com/zwz050418/zwz-hmdp.git`，分支 `master`
- GitHub: `github` -> `https://github.com/zhengwenze/zwz-hmdp.git`，分支 `main`

目标是确保两个远程分支指向相同的提交，保持代码仓库的同步性。

## 使用场景

当用户提出以下需求时触发本技能：

- "推送代码到双仓库"
- "同步 Gitee 和 GitHub"
- "发布到两个仓库"
- "提交代码并推送到远程"
- "保持双仓库同步"

## 前置检查

1. **仓库验证**：确认当前工作目录为 `/Users/zhengwenze/Desktop/codex/zwz-hmdp`
2. **远程配置**：验证两个远程仓库 URL 是否正确配置
3. **分支状态**：检查本地分支与远程分支的差异

## 执行指令

### 步骤 1：状态检查

```bash
# 检查当前目录和分支状态
pwd
git status --short --branch

# 检查远程仓库配置
git remote -v

# 使用脚本进行完整状态检查
bash skills/dual-remote-push/scripts/check-dual-remote-status.sh
```

### 步骤 2：变更审查

```bash
# 查看变更摘要
git diff --stat

# 查看变更文件列表
git diff --name-only

# 查看未追踪文件
git ls-files --others --exclude-standard
```

**注意**：如果发现无关或风险变更，应在提交前与用户确认。

### 步骤 3：代码验证

- **前端变更**：执行 `cd frontend && yarn build`
- **后端变更**：执行 `cd backend && mvn test`（如 Maven 不可用，尝试 `mvn -q -DskipTests package`）
- **无测试环境**：明确报告验证限制

### 步骤 4：远程差异检查

```bash
# 获取远程最新提交
git fetch origin master
git fetch github main

# 检查本地与远程的差异
git rev-list --left-right --count HEAD...origin/master
git rev-list --left-right --count HEAD...github/main
```

**处理规则**：

- 如果远程有新提交（右侧计数 > 0），停止执行并提示需要拉取/合并
- 如果本地领先远程，可以继续推送

### 步骤 5：提交代码

```bash
# 分类提交（推荐）
git add <指定文件或目录>
git commit -m "描述：简洁的中文提交说明"

# 或提交所有变更（当用户明确要求时）
git add -A
git commit -m "描述：简洁的中文提交说明"
```

**提交信息规范**：

- 使用中文描述
- 清晰说明变更内容
- 可使用前缀：`优化：`、`修复：`、`新增：`、`清理：`、`适配：`

### 步骤 6：推送双仓库

```bash
# 推送到 Gitee
git push origin HEAD:master

# 推送到 GitHub
git push github HEAD:main
```

**注意**：除非用户明确要求，否则禁止使用 `--force` 强制推送。

### 步骤 7：结果确认

```bash
# 检查推送状态
git status --short --branch

# 验证远程分支哈希
git ls-remote origin refs/heads/master
git ls-remote github refs/heads/main

# 查看最新提交
git log -1 --oneline --decorate
```

## 输出报告

完成后输出以下信息：

- 提交哈希和提交信息
- 包含的变更文件列表
- 验证命令及结果
- Gitee `master` 和 GitHub `main` 的远程哈希
- 任何限制或跳过的验证步骤

## 安全约束

1. **禁止强制推送**：除非用户明确确认，否则不使用 `--force` 参数
2. **冲突检测**：远程有新提交时必须停止并提示用户
3. **变更审查**：提交前必须向用户展示变更内容
4. **敏感信息保护**：不记录或输出任何敏感配置信息

## 示例

### 输入

```
将当前修改推送到双仓库
```

### 输出

```
## 提交记录

**Commit**: `a1b2c3d`
```

优化：修复用户登录逻辑

**修改文件**:

- backend/src/main/java/com/hmdp/service/UserServiceImpl.java

## 推送结果

| 远程仓库 | 分支   | 提交哈希                                   |
| -------- | ------ | ------------------------------------------ |
| Gitee    | master | a1b2c3d4567890abcdef1234567890abcdef123456 |
| GitHub   | main   | a1b2c3d4567890abcdef1234567890abcdef123456 |

两个仓库已同步完成！

```

```
