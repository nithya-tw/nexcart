# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

# Copy Maven wrapper and parent pom
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Copy user-service pom
COPY services/user-service/pom.xml services/user-service/

# Download dependencies
RUN ./mvnw dependency:go-offline -pl services/user-service

# Copy source code
COPY services/user-service/src services/user-service/src

# Build the application
RUN ./mvnw clean package -pl services/user-service -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /workspace/services/user-service/target/*.jar app.jar

# Expose application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
