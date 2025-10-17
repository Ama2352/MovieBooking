# === Stage 1: Build the app with Maven ===
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# === Stage 2: Run the app ===
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/moviebooking-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
