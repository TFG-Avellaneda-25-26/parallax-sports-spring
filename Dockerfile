# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=build /workspace/build/libs/*.jar app.jar

# Copy decrypted secrets (Jenkins decrypts before docker build runs)
COPY --from=build /workspace/src/main/resources/api-secrets.yml /app/config/api-secrets.yml

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75", \
    "-jar", "/app/app.jar", \
    "--spring.config.import=optional:file:/app/config/api-secrets.yml"]
