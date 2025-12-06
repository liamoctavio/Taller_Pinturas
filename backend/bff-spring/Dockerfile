# ------------------
# Build stage (Java 21)
# ------------------
FROM maven:3.9.4-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -e -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# ------------------
# Runtime stage (Java 21, Alpine)
# ------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# crear usuario no-root
RUN addgroup -S app && adduser -S -G app app
USER app

WORKDIR /app

COPY --from=builder /workspace/target/tallerpinturas-*-SNAPSHOT.jar ./app.jar

ENV JAVA_OPTS=""
ENV SERVER_PORT=8080

EXPOSE 8080

ENTRYPOINT ["/bin/sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
