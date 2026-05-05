-- =========================================================
-- HMDP database review index migration
-- Target: MySQL 8.0
--
-- Use this script for existing databases initialized from backend/db/hmdp.sql.
-- For a fresh database, prefer backend/db/hmdp-new.sql instead.
-- =========================================================

-- 1. Check duplicate follow relationships before adding the unique key.
-- If this query returns rows, resolve them manually before running the DDL below.
SELECT user_id, follow_user_id, COUNT(*) AS cnt
FROM tb_follow
GROUP BY user_id, follow_user_id
HAVING cnt > 1;

-- Optional duplicate cleanup template. Review the SELECT result above before enabling it.
-- This keeps the smallest id for each (user_id, follow_user_id) pair.
-- DELETE f
-- FROM tb_follow f
-- JOIN (
--   SELECT id
--   FROM (
--     SELECT id,
--            ROW_NUMBER() OVER (
--              PARTITION BY user_id, follow_user_id
--              ORDER BY id
--            ) AS rn
--     FROM tb_follow
--   ) ranked
--   WHERE rn > 1
-- ) duplicated ON duplicated.id = f.id;

-- 2. Apply review-approved constraints and indexes.
ALTER TABLE tb_follow
  ADD UNIQUE KEY uk_follow_user_pair (user_id, follow_user_id),
  ADD KEY idx_follow_follow_user (follow_user_id, user_id);

ALTER TABLE tb_blog
  ADD KEY idx_blog_user_time (user_id, create_time DESC, id DESC),
  ADD KEY idx_blog_liked_id (liked DESC, id DESC);

ALTER TABLE tb_voucher
  ADD KEY idx_voucher_shop_status (shop_id, status, id);

ALTER TABLE tb_kb_document
  ADD KEY idx_kb_document_status (status),
  ADD KEY idx_kb_document_update_time (update_time DESC, id DESC),
  ADD KEY idx_kb_document_file_id (file_path, id);

ALTER TABLE tb_kb_ingest_job
  ADD KEY idx_kb_job_started_time (started_time DESC, id DESC);

-- Rollback template. Enable only if you intentionally need to remove these indexes.
-- ALTER TABLE tb_kb_ingest_job DROP INDEX idx_kb_job_started_time;
-- ALTER TABLE tb_kb_document
--   DROP INDEX idx_kb_document_file_id,
--   DROP INDEX idx_kb_document_update_time,
--   DROP INDEX idx_kb_document_status;
-- ALTER TABLE tb_voucher DROP INDEX idx_voucher_shop_status;
-- ALTER TABLE tb_blog
--   DROP INDEX idx_blog_liked_id,
--   DROP INDEX idx_blog_user_time;
-- ALTER TABLE tb_follow
--   DROP INDEX idx_follow_follow_user,
--   DROP INDEX uk_follow_user_pair;
