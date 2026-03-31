# ══════════════════════════════════════════════════════════════════
# 🐸 LingoCóc — Dockerfile (Multi-stage build)
# Stage 1 "builder" : Maven compile + package → tạo file .jar
# Stage 2 "runtime" : JRE tối thiểu + chạy .jar
# ══════════════════════════════════════════════════════════════════

# ── Stage 1: Build ────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml trước để tận dụng Docker layer cache
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy toàn bộ source
COPY src ./src

# Build (bỏ qua tests)
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S lingococ && adduser -S lingococ -G lingococ

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN mkdir -p /app/logs && chown -R lingococ:lingococ /app

USER lingococ

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
