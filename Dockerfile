FROM gradle:8.10-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle clean shadowJar --no-daemon -x test 2>/dev/null || gradle clean jar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
