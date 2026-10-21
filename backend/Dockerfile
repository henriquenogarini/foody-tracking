# syntax=docker/dockerfile:1

# ---- Build stage: compiles the boot jar. The quality gate (./mvnw verify) runs in CI, not here.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Wrapper and POM first so the dependency layer is cached until pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
# The repository is edited on Windows: strip CRLF so the wrapper runs under Linux.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests package \
    && cp target/foody-tracking-api-*.jar /build/app.jar

# ---- Runtime stage: JRE only, non-root.
FROM eclipse-temurin:21-jre

# curl is installed only for the HEALTHCHECK below.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 foody

WORKDIR /app
RUN mkdir -p /app/data && chown foody /app/data

COPY --from=build /build/app.jar /app/app.jar

# TZ drives log timestamps (CLAUDE.md §5.5).
ENV TZ=America/Sao_Paulo

USER foody
VOLUME /app/data
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

# The profile is a system property, not an ENV: it outranks SPRING_PROFILES_ACTIVE from any
# --env-file, so the image can never boot the dev profile and its public secrets.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Dspring.profiles.active=prod", "-jar", "/app/app.jar"]
