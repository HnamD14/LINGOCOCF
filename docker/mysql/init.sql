-- ══════════════════════════════════════════════════════════════
-- 🐸 LingoCóc — MySQL init script
-- Chạy 1 lần khi tạo container MySQL lần đầu tiên
-- ══════════════════════════════════════════════════════════════

-- Đảm bảo charset đúng cho tiếng Trung
ALTER DATABASE lingococ CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- (Tuỳ chọn) Tạo tài khoản admin mặc định
-- Password hash cho "admin123" — thay bằng hash của mật khẩu thật khi deploy
-- Tạo hash: htpasswd -bnBC 10 "" "mat_khau" | tr -d ':\n' | sed 's/$2y/$2a/'
-- INSERT IGNORE INTO users (username, email, password, role, full_name)
-- VALUES ('admin', 'admin@lingococ.vn', '$2a$10$...HASH...', 'ADMIN', 'Administrator');

-- Thêm cột coins nếu chưa tồn tại (safe migration)
ALTER TABLE users ADD COLUMN IF NOT EXISTS coins INT NOT NULL DEFAULT 0;
