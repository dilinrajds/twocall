# ==============================================================================
# Production Dockerfile for TwoCall ChatApp Backend (Root Context)
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy gradle wrapper and project definition from backend
COPY backend/gradlew backend/settings.gradle backend/build.gradle ./
COPY backend/gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# Copy backend source code and compile jar
COPY backend/src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage with hardened lightweight JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S chatapp && adduser -S chatapp -G chatapp
RUN mkdir -p /app/uploads/media /app/config && chown -R chatapp:chatapp /app

USER chatapp

COPY --from=builder --chown=chatapp:chatapp /workspace/build/libs/*.jar app.jar

ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080 10000

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
