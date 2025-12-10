# ------------------
# Build stage (Java 21)
# ------------------
FROM maven:3.9.4-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests clean package

# ------------------
# Runtime stage (Java 21, Alpine)
# ------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
ENV JAVA_OPTS=""
WORKDIR /opt/app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /opt/app/app.jar"]
