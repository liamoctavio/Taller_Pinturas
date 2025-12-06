FROM maven:3.9.4-eclipse-temurin-21 as builder
WORKDIR /app

COPY pom.xml .
RUN mvn -B -e -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
ENV JAVA_OPTS=""
WORKDIR /opt/app
COPY --from=build /app/target/bff-spring-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

