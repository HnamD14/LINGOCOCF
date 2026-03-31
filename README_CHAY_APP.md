# 🐸 Hướng dẫn chạy LingoCóc

## ✅ Cách 1 — Chạy nhanh (không cần PostgreSQL, dùng H2)

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Sau đó mở trình duyệt: **http://localhost:8080**

---

## ✅ Cách 2 — Chạy với PostgreSQL (production-like)

### Bước 1: Cài PostgreSQL và tạo database
```sql
CREATE DATABASE lingococ;
```

### Bước 2: Tạo file `.env` (copy từ `.env.example`)
```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=lingococ
DB_USER=postgres
DB_PASS=your_password
```

### Bước 3: Chạy backend
```bash
cd backend
mvn spring-boot:run
```

Sau đó mở: **http://localhost:8080**

---

## ⚠️ Lưu ý quan trọng

- **KHÔNG** dùng Live Server để mở `frontend/index.html` vì không có API
- Luôn truy cập qua **http://localhost:8080** sau khi Spring Boot đã start
- Khi thấy `Started AuthApplication` trong terminal là đã sẵn sàng
- Tài khoản admin mặc định: `admin` / `admin123`

---

## 🐛 Lỗi thường gặp

| Lỗi | Nguyên nhân | Cách sửa |
|-----|-------------|----------|
| `Compilation error: cannot find symbol` | Thiếu Lombok processor | Đã fix trong pom.xml |
| `Connection refused` (PostgreSQL) | PostgreSQL chưa chạy | Dùng profile `dev` (H2) |
| `Port 8080 already in use` | Có app khác dùng port | Tắt app đó hoặc đổi port |
| CSS không load | Mở qua Live Server | Truy cập localhost:8080 |
