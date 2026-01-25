# --- STAGE 1: Build the Application ---
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set the working directory
WORKDIR /app

# Copy the pom.xml file first to cache dependencies layer
COPY pom.xml .

# Download dependencies (uses the cached layer if pom.xml hasn't changed)
RUN mvn dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Build the final JAR file
RUN mvn package -DskipTests


# --- STAGE 2: Create the Final, Smaller Runtime Image ---
# Use a lightweight JRE (Java Runtime Environment) base image for the final container
FROM eclipse-temurin:21-jre-alpine

# Set the working directory
WORKDIR /tag

# Copy the final JAR from the 'build' stage
COPY --from=build /app/target/TAG.jar /tag/TAG.jar

# Copy data files required for some games
COPY data /tag/data

ENTRYPOINT ["java", "-jar", "/tag/TAG.jar"]