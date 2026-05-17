FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Jenkins builds the JAR via `./mvnw clean package -DskipTests` in the Build jar
# stage *before* this docker build runs, so target/*.jar is in the build context.
COPY target/*.jar app.jar

# Jenkins decrypts api-secrets.yml via git-crypt before docker build, so this
# file is present in src/main/resources at image-build time.
COPY src/main/resources/api-secrets.yml /app/config/api-secrets.yml

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75", \
    "-jar", "/app/app.jar", \
    "--spring.config.import=optional:file:/app/config/api-secrets.yml"]
