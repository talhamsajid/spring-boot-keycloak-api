# ----------- Build Stage -----------
FROM gradle:7.6.4-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# ----------- Run Stage -----------
FROM openjdk:17-jdk-slim as runtime
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
