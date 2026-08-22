# --- Build stage: compile with Maven ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve dependencies first so they are cached independently of source changes
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src src
RUN mvn -q package -DskipTests

# --- Extract stage: split the fat jar into image-friendly layers ---
FROM eclipse-temurin:21-jre-alpine AS extract
WORKDIR /extract

COPY --from=build /build/target/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# --- Runtime stage: minimal JRE, non-root user ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /application

RUN addgroup -S app && adduser -S app -G app
USER app

# Layers ordered from least to most frequently changing, so a code change
# only rebuilds the small application layer
COPY --from=extract /extract/extracted/dependencies/ ./
COPY --from=extract /extract/extracted/spring-boot-loader/ ./
COPY --from=extract /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract /extract/extracted/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
