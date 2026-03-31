# 🐸 LingoCóc v2.0 — Hướng dẫn chạy

## Cấu trúc dự án

```
lingococ/
├── backend/                    ← Spring Boot 3.2, Java 17
│   ├── Dockerfile              ← Multi-stage build
│   └── src/main/
│       ├── java/com/example/auth/
│       │   ├── config/         ← Security, OpenAPI, ExceptionHandler
│       │   ├── controller/     ← Auth, User, Payment, AI
│       │   ├── dto/            ← Request/Response với @Valid
│       │   ├── filter/         ← JWT, RateLimit
│       │   ├── model/          ← User, Payment entities
│       │   └── service/        ← Business logic
│       └── resources/
│           └── application.yml ← Config (dev=H2, prod=MySQL)
├── frontend/
│   ├── index.html              ← App học tiếng Trung
│   └── admin.html              ← Trang quản lý
├── docker/
│   ├── nginx/nginx.conf        ← Reverse proxy config
│   └── mysql/init.sql          ← DB init script
├── docker-compose.yml          ← Orchestration
├── .env.example                ← Template biến môi trường
├── .gitignore
└── docker-run.sh               ← Script khởi động nhanh
```

---

## 🖥️ Chạy LOCAL (Development — không cần Docker)

**Yêu cầu:** Java 17+, VS Code + Extension Pack for Java

```bash
# 1. Chạy Backend
cd backend/
# VS Code: mở AuthApplication.java → click ▶ Run
# Hoặc: mvn spring-boot:run

# 2. Mở Frontend
# VS Code: chuột phải index.html → Open with Live Server
# Truy cập: http://localhost:5500
```

API Docs: `http://localhost:8080/swagger-ui.html`  
H2 Console: `http://localhost:8080/h2-console`

---

## 🐳 Chạy bằng Docker (Production-ready)

**Yêu cầu:** Docker Desktop hoặc Docker Engine + docker-compose

### Lần đầu:
```bash
# 1. Sao chép file cấu hình
cp .env.example .env

# 2. Điền thông tin vào .env (bắt buộc)
nano .env   # hoặc code .env

# 3. Khởi động (build + run)
./docker-run.sh
# Hoặc thủ công:
# docker-compose up -d --build
```

### Sau khi khởi động:
| URL | Mô tả |
|-----|-------|
| `http://localhost` | Frontend app |
| `http://localhost/api/...` | API |
| `http://localhost/swagger-ui.html` | Swagger UI |

### Lệnh thường dùng:
```bash
docker-compose ps              # Xem trạng thái
docker-compose logs -f backend # Xem log backend real-time
docker-compose logs -f nginx   # Xem log Nginx
docker-compose restart backend # Restart sau khi sửa code
docker-compose down            # Tắt (giữ data)
docker-compose down -v         # Tắt + xoá toàn bộ data
```

### Rebuild sau khi sửa code:
```bash
docker-compose up -d --build backend
```

---

## 🔑 Cấu hình bắt buộc trong `.env`

| Biến | Mô tả | Lấy ở đâu |
|------|-------|-----------|
| `DB_ROOT_PASSWORD` | Mật khẩu root MySQL | Tự đặt |
| `DB_PASS` | Mật khẩu user DB | Tự đặt |
| `JWT_SECRET` | Chuỗi bí mật ký JWT | `openssl rand -base64 64` |
| `MAIL_USERNAME` | Gmail gửi email | Gmail của bạn |
| `MAIL_PASSWORD` | App Password Gmail | [Hướng dẫn](https://support.google.com/mail/answer/185833) |
| `ANTHROPIC_API_KEY` | API key Anthropic AI | [console.anthropic.com](https://console.anthropic.com) |
| `SEPAY_WEBHOOK_SECRET` | Webhook secret SePay | Dashboard SePay |
| `CORS_ORIGINS` | Domain frontend | Domain thật của bạn |

---

## 🚀 Deploy lên VPS/Cloud

```bash
# Trên VPS (Ubuntu 22.04):
# 1. Cài Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 2. Clone project
git clone https://github.com/your/lingococ.git
cd lingococ

# 3. Cấu hình
cp .env.example .env && nano .env
# Đổi CORS_ORIGINS=https://domain-cua-ban.vn

# 4. Chạy
./docker-run.sh

# 5. HTTPS — cài certbot
sudo apt install certbot
sudo certbot certonly --standalone -d domain-cua-ban.vn
# Copy certs vào docker/nginx/certs/
# Bỏ comment phần HTTPS trong docker/nginx/nginx.conf
# docker-compose restart nginx
```

---

## 📡 API Endpoints

| Method | URL | Mô tả | Auth |
|--------|-----|-------|------|
| POST | /api/auth/register | Đăng ký | Public |
| POST | /api/auth/login | Đăng nhập | Public |
| POST | /api/auth/forgot-password | Quên mật khẩu | Public |
| GET  | /api/user/me | Thông tin user | JWT |
| POST | /api/user/progress | Đồng bộ tiến độ | JWT |
| POST | /api/user/streak/recover | Phục hồi streak | JWT |
| GET  | /api/user/leaderboard | Bảng xếp hạng | JWT |
| GET  | /api/payment/bank-info | Thông tin NH | Public |
| POST | /api/payment/webhook/sepay | SePay webhook | Public |
| POST | /api/payment/create-order | Tạo đơn + QR | JWT |
| POST | /api/ai/explain | Giải thích Hán tự | JWT (PLUS/PRO) |
| GET  | /api/ai/quota | Kiểm tra quota AI | JWT |

Xem đầy đủ tại: `http://localhost/swagger-ui.html`
