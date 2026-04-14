-- ══════════════════════════════════════════════════════════════
-- Migration: Thêm cột lưu custom decks và saved cards
-- Chạy script này nếu database đã tồn tại (không chạy init.sql)
-- Lệnh: mysql -u root -p lingococ < migrate_add_decks.sql
-- ══════════════════════════════════════════════════════════════
ALTER TABLE users ADD COLUMN IF NOT EXISTS custom_decks_json MEDIUMTEXT NULL
  COMMENT 'JSON array: bộ thẻ tự tạo của user (myDecks)';
ALTER TABLE users ADD COLUMN IF NOT EXISTS saved_cards_json TEXT NULL
  COMMENT 'JSON array: card AI đã lưu (sc_session_saved)';
