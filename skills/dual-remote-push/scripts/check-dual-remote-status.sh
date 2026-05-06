#!/usr/bin/env bash
set -euo pipefail

expected_gitee="https://gitee.com/zwz050418/zwz-hmdp.git"
expected_github="https://github.com/zhengwenze/zwz-hmdp.git"

echo "Repository: $(git rev-parse --show-toplevel)"
echo "Branch: $(git branch --show-current)"
echo

echo "Working tree:"
git status --short --branch
echo

origin_url="$(git remote get-url origin 2>/dev/null || true)"
github_url="$(git remote get-url github 2>/dev/null || true)"

echo "Remotes:"
printf 'origin  %s\n' "${origin_url:-<missing>}"
printf 'github  %s\n' "${github_url:-<missing>}"
echo

if [[ "$origin_url" != "$expected_gitee" ]]; then
  echo "ERROR: origin does not match expected Gitee URL: $expected_gitee" >&2
  exit 1
fi

if [[ "$github_url" != "$expected_github" ]]; then
  echo "ERROR: github does not match expected GitHub URL: $expected_github" >&2
  exit 1
fi

echo "Changed files:"
git diff --name-only
git ls-files --others --exclude-standard
echo

echo "Remote divergence:"
git fetch origin master >/dev/null
git fetch github main >/dev/null
printf 'HEAD...origin/master  '
git rev-list --left-right --count HEAD...origin/master
printf 'HEAD...github/main    '
git rev-list --left-right --count HEAD...github/main
