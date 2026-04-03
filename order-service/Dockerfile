FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml ./pom.xml
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

RUN jar tf target/*.jar | grep changelog

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
