# =============================================================================
# ORDER-SERVICE Dockerfile
# =============================================================================
# Two-stage approach:
#   The CI pipeline builds the JAR with Gradle, then this Dockerfile
#   copies it into a minimal JRE image for production.
#
# Build in CI:
#   ./gradlew :order-service:bootJar -x test
#   docker build -t order-service order-service/
# =============================================================================

FROM eclipse-temurin:21-jre-alpine

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the fat JAR built by Spring Boot's bootJar task
COPY build/libs/*.jar app.jar

# Ensure the JAR is owned by the app user
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8081

# JVM flags for containerized environments:
#   -XX:+UseContainerSupport   → respect cgroup memory/CPU limits
#   -XX:MaxRAMPercentage=75    → use up to 75% of container RAM for heap
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
