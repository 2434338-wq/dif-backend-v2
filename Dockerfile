FROM gradle:8.10-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle clean jar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
COPY --from=build /app/src/main/resources/application.conf application.conf
EXPOSE 8080
CMD ["java", "-Dconfig.file=/app/application.conf", "-jar", "/app/app.jar"]
