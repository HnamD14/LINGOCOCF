#!/bin/bash
# ══════════════════════════════════════════════════════════════════
# 🐸 LingoCóc — Script khởi động nhanh
# Chạy: chmod +x docker-run.sh && ./docker-run.sh
# ══════════════════════════════════════════════════════════════════
set -e

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ── Kiểm tra Docker ──────────────────────────────────────────────
command -v docker        >/dev/null 2>&1 || error "Docker chưa cài. Xem: https://docs.docker.com/get-docker/"
command -v docker-compose >/dev/null 2>&1 || \
  docker compose version  >/dev/null 2>&1 || error "docker-compose chưa cài."

# ── Kiểm tra file .env ───────────────────────────────────────────
if [ ! -f ".env" ]; then
  warn "Chưa có file .env — sao chép từ .env.example..."
  cp .env.example .env
  warn "Hãy điền thông tin vào .env trước khi tiếp tục!"
  warn "  nano .env"
  echo ""
  read -p "Nhấn Enter sau khi đã điền .env để tiếp tục, hoặc Ctrl+C để thoát..."
fi

# ── Chọn lệnh docker compose ────────────────────────────────────
DC="docker-compose"
docker compose version >/dev/null 2>&1 && DC="docker compose"

info "Đang build và khởi động LingoCóc..."
$DC up -d --build

info "Đợi services khởi động (khoảng 30 giây)..."
sleep 10

# ── Kiểm tra health ──────────────────────────────────────────────
info "Kiểm tra trạng thái containers:"
$DC ps

echo ""
info "✅ LingoCóc đang chạy tại:"
echo "   🌐 Frontend:  http://localhost"
echo "   📡 API:       http://localhost/api"
echo "   📖 Swagger:   http://localhost/swagger-ui.html"
echo ""
echo "   📋 Xem log:   $DC logs -f backend"
echo "   🛑 Tắt:       $DC down"
echo "   🗑️  Xoá data:  $DC down -v"
