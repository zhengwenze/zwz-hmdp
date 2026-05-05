# Database Review Notes

## SQL 文件用途

- `backend/db/hmdp.sql` 是原始教学/历史版本，用于对照和回溯，不在本次 review 中覆盖。
- `backend/db/hmdp-new.sql` 是合并数据库 review 修复后的推荐初始化版本，适合全新环境直接建库导入。
- `backend/db/migrations/20260505_review_indexes.sql` 是已有数据库的增量修复脚本，适合已经从原始 `hmdp.sql` 初始化过的环境。

新环境推荐直接执行 `backend/db/hmdp-new.sql`。老环境推荐先检查重复关注数据，再执行 migration SQL。

## 已落地的索引和约束

`tb_follow`

- `uk_follow_user_pair(user_id, follow_user_id)` 防止并发重复关注。
- `idx_follow_follow_user(follow_user_id, user_id)` 支持按被关注用户查询粉丝，匹配发布博客时查询粉丝并推送 feed 的路径。

`tb_blog`

- `idx_blog_user_time(user_id, create_time DESC, id DESC)` 支持用户主页、我的笔记列表查询。
- `idx_blog_liked_id(liked DESC, id DESC)` 支持热门博客列表排序。
- 暂不添加 `tb_blog_shop_time`，因为当前代码没有明确店铺博客列表查询路径。

`tb_voucher`

- `idx_voucher_shop_status(shop_id, status, id)` 支持店铺详情页查询可用优惠券列表。

`tb_kb_document`

- `idx_kb_document_status(status)` 支持 RAG 状态页统计 READY / FAILED 文档。
- `idx_kb_document_update_time(update_time DESC, id DESC)` 支持文档列表按更新时间倒序展示。
- `idx_kb_document_file_id(file_path, id)` 支持按文件路径查找最新文档记录。

`tb_kb_ingest_job`

- `idx_kb_job_started_time(started_time DESC, id DESC)` 支持查询最新 RAG 导入任务。

## 暂缓项

- 不添加 `FULLTEXT`。RAG 语义检索继续交给 Milvus，MySQL 只承担元数据、状态和 chunk 映射。
- 不修改 `tb_user_info.level`、`tb_blog_comments.status` 对应 Java 实体类型。本问题应作为独立任务处理，避免和索引迁移混在同一次变更中。
- 不清理 `tb_sign`。当前签到逻辑走 Redis Bitmap，该表属于历史遗留持久化表。
- 不修改 `tb_blog.liked/comments`、`tb_shop.sold/comments/score`、`tb_user_info.fans/followee` 等统计冗余字段。本类字段需要单独设计维护和对账策略。

## 老环境执行前检查

添加 `tb_follow` 唯一约束前，必须先检查重复关注关系：

```sql
SELECT user_id, follow_user_id, COUNT(*) AS cnt
FROM tb_follow
GROUP BY user_id, follow_user_id
HAVING cnt > 1;
```

如果返回结果不为空，不要直接执行迁移。建议人工确认后保留每组最小 `id`，删除其余重复记录，再添加唯一约束。

## Docker MySQL 8.0 验证

```bash
docker compose up -d mysql
docker exec -i hmdp-mysql mysql -uroot -proot -e "DROP DATABASE IF EXISTS hmdp_new_verify; CREATE DATABASE hmdp_new_verify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
docker exec -i hmdp-mysql mysql -uroot -proot hmdp_new_verify < backend/db/hmdp-new.sql
docker exec -it hmdp-mysql mysql -uroot -proot hmdp_new_verify
```

进入 MySQL 后检查索引：

```sql
SHOW INDEX FROM tb_follow;
SHOW INDEX FROM tb_blog;
SHOW INDEX FROM tb_voucher;
SHOW INDEX FROM tb_kb_document;
SHOW INDEX FROM tb_kb_ingest_job;
```

关键查询的 `EXPLAIN`：

```sql
EXPLAIN SELECT * FROM tb_blog
WHERE user_id = 1
ORDER BY create_time DESC, id DESC
LIMIT 10;

EXPLAIN SELECT * FROM tb_blog
ORDER BY liked DESC, id DESC
LIMIT 10;

EXPLAIN SELECT * FROM tb_voucher
WHERE shop_id = 1 AND status = 1;

EXPLAIN SELECT * FROM tb_kb_document
ORDER BY update_time DESC, id DESC
LIMIT 20;

EXPLAIN SELECT * FROM tb_kb_ingest_job
ORDER BY started_time DESC, id DESC
LIMIT 1;
```
