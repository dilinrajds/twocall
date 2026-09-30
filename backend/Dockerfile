# ==============================================================================
# Multi-stage Dockerfile for TwoCall ChatApp Backend
# Lightweight, hardened JRE 21 Alpine image suitable for 512MB RAM free tiers
# ==============================================================================

FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Cache gradle dependencies
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# Copy source code and build production jar
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
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

