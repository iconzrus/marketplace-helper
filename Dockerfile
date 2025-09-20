# Multi-stage Dockerfile for Spring Boot (Maven) backend

# --- Build stage ---
FROM maven:3.9.7-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only pom first for better layer caching
COPY pom.xml ./
RUN mvn -q -e -DskipTests dependency:go-offline

# Copy sources and build
COPY src ./src
RUN mvn -q -DskipTests package

# --- Runtime stage ---
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS=""
WORKDIR /app

# Copy jar
COPY --from=build /workspace/target/*.jar /app/app.jar

# Koyeb/Render expose 8080 by default
EXPOSE 8080

# Health check (optional)
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s CMD wget -qO- http://localhost:8080/api/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dserver.port=8080 -jar /app/app.jar"]


