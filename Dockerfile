# ---- build stage -------------------------------------------------------
# Dependencies are resolved in their own layer so source edits don't
# re-download the world on every build.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# ---- runtime stage -----------------------------------------------------
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# Never run the API as root.
RUN groupadd --system --gid 1001 famtree \
 && useradd --system --uid 1001 --gid famtree --home /app famtree

COPY --from=build --chown=famtree:famtree /build/target/*.jar /app/app.jar

USER famtree
EXPOSE 8080

# Container-aware heap sizing; override JAVA_OPTS to tune.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
