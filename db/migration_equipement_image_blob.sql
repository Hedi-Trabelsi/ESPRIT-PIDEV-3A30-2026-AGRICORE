-- ─────────────────────────────────────────────────────────────────────────
-- Migration: store equipement images as BLOB in DB (cross-app sync)
-- Run once on the shared MySQL database (`projetjava`).
--
--   "C:\xampp\mysql\bin\mysql.exe" -u root projetjava < migration_equipement_image_blob.sql
--
-- After this, both the Java desktop app and the Symfony web app read/write
-- the same `image` column. The legacy `image_filename` column stays in place
-- as a fallback for the two pre-existing rows that were uploaded via Vich.
-- ─────────────────────────────────────────────────────────────────────────

ALTER TABLE equipements
    ADD COLUMN IF NOT EXISTS image LONGBLOB NULL AFTER updated_at;

-- Optional sanity check (uncomment to inspect):
-- SHOW COLUMNS FROM equipements;
