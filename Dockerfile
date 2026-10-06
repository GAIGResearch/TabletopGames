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

# Build the project's own classes jar (the fat jars are not needed here), and copy its dependencies to target/lib
RUN mvn package -DskipTests -Dassembly.skipAssembly=true \
 && mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/lib \
 && cp target/ModernBoardGame-*.jar target/tag-classes.jar


# --- STAGE 2: Create the Runtime Image ---
# A JRE on Ubuntu, with what Swing needs to draw: an X server with no screen (Xvfb, so the web server can lay out and
# draw game GUIs, see web.WebServer), and fonts. tini forwards docker's stop signal to Java.
FROM eclipse-temurin:21-jre

RUN apt-get update \
 && apt-get install -y --no-install-recommends xvfb xauth tini fontconfig fonts-dejavu-core libxrender1 libxtst6 libxi6 \
 && rm -rf /var/lib/apt/lists/*

# Set the working directory (games load their files from data/, relative to it)
WORKDIR /tag

# Dependencies first, as they change least: an image rebuilt after a code change then reuses this layer
COPY --from=build /app/target/lib /tag/lib

# Copy data files required for some games
COPY data /tag/data

COPY --from=build /app/target/tag-classes.jar /tag/tag-classes.jar

# For the WebServer entry point
EXPOSE 8080

# Every entry point runs under Xvfb, which the others simply do not use, e.g.
#   docker run tag RunGames config=...
#   docker run -p 8080:8080 tag WebServer games=LawnAndOrder token=...
ENTRYPOINT ["tini", "--", "xvfb-run", "-a", "-s", "-screen 0 2560x1600x24", \
            "java", "-cp", "/tag/tag-classes.jar:/tag/lib/*", "core.TAG"]
