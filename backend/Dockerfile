# ─── Stage 1: Build ───────────────────────────────────────────────────────────
# Use the official Maven image pinned to the same Java version as pom.xml.
# Running `mvn package` inside Docker guarantees a reproducible build regardless
# of what Maven version the developer has locally.
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

# Copy dependency descriptors first so Docker can cache the dependency layer.
# Re-downloading 200MB of JARs on every code change is the #1 avoidable slowdown
# in backend CI pipelines.
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
# Skip tests in Docker build — tests belong in CI, not in image construction.
RUN mvn package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
# eclipse-temurin JRE (not JDK) on Alpine keeps the final image under 150MB.
# We don't need a compiler in production — only the runtime.
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user: running as root inside a container still poses risk if the
# container runtime is misconfigured or there's a path traversal vulnerability.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy only the fat JAR produced by spring-boot-maven-plugin.
COPY --from=build /build/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Pre-create the storage directory and assign ownership before dropping privileges.
# The volume mount in docker-compose will overlay this, but the path must exist
# with correct ownership for cases where no volume is mounted (e.g., integration tests).
RUN mkdir -p /app/storage && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

# Use exec form to ensure the JVM receives SIGTERM directly and can do a graceful shutdown.
ENTRYPOINT ["java", "-jar", "app.jar"]