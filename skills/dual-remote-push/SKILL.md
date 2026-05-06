---
name: dual-remote-push
description: "Commit the current repository changes and push the same resulting commit history to two configured remotes: Gitee `master` and GitHub `main`. Use when the user asks to keep both `https://gitee.com/zwz050418/zwz-hmdp.git` and `https://github.com/zhengwenze/zwz-hmdp.git` synchronized, to push to both repositories, or to publish local changes to both Gitee and GitHub branches at once."
---

# Dual Remote Push

## Scope

Use this workflow only for `/Users/zhengwenze/Desktop/codex/zwz-hmdp`.

Target remotes and branches:

- Gitee: `origin` -> `https://gitee.com/zwz050418/zwz-hmdp.git`, branch `master`
- GitHub: `github` -> `https://github.com/zhengwenze/zwz-hmdp.git`, branch `main`

The goal is that both remote branches point to the same final commit as the local publish commit.

## Workflow

1. Confirm repository and remotes.
   - Run `pwd`, `git status --short --branch`, and `git remote -v`.
   - Prefer `scripts/check-dual-remote-status.sh` for a compact status report.
   - If either remote is missing or points to a different URL, stop and report the mismatch before pushing.

2. Inspect the full change set before staging.
   - Run `git diff --stat` and `git diff --name-only`.
   - If the user asked for "all current changes", include every tracked and untracked file that belongs to the task.
   - If unrelated or risky changes appear, call them out before committing unless the user explicitly requested all local changes.

3. Verify before commit.
   - Frontend-only or frontend-involved changes: run `cd frontend && yarn build`.
   - Backend-involved changes: run `cd backend && mvn test` when Maven is available; if the repo has no tests or Maven is unavailable, try `mvn -q -DskipTests package` and report the exact limitation.
   - Do not claim backend verification passed when `mvn` or `mvnw` is unavailable.

4. Check remote divergence.
   - Run `git fetch origin master` and `git fetch github main`.
   - Run `git rev-list --left-right --count HEAD...origin/master`.
   - Run `git rev-list --left-right --count HEAD...github/main`.
   - If the right-side count is nonzero for either remote, stop and explain that a pull/rebase/merge decision is needed.
   - If the local branch is ahead of GitHub `main` by many commits and not behind, pushing `HEAD:main` is acceptable when the user asked for full synchronization.

5. Stage and commit.
   - Use explicit paths or `git add -A` only when "all current changes" is intended.
   - Commit with a concise conventional message, for example `fix: restore sign calendar display`.
   - If there are no changes to commit, skip commit and proceed to push only if remotes are behind local HEAD.

6. Push both remotes.
   - Push Gitee: `git push origin HEAD:master`
   - Push GitHub: `git push github HEAD:main`
   - Do not use force push unless the user explicitly asks and understands the consequence.

7. Confirm final state.
   - Run `git status --short --branch`.
   - Run `git ls-remote origin refs/heads/master`.
   - Run `git ls-remote github refs/heads/main`.
   - Run `git log -1 --oneline --decorate`.
   - Verify both remote hashes equal local `HEAD`.

## Handoff

Report:

- Commit hash and message.
- Which files were included.
- Verification commands and outcomes.
- Exact remote branch hashes for Gitee `master` and GitHub `main`.
- Any limitations, especially skipped backend verification.

If staging, committing, or pushing succeeded, emit the appropriate Codex git directives in the final answer.
