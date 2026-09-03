FROM gradle:9.5.1-jdk21 AS build
WORKDIR /workspace

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /workspace/build/libs/CarteiraInvestimento-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
