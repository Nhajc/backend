FROM eclipse-temurin:25.0.5_8-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:25.0.5_8-jre-alpine
RUN apk add --no-cache flac fdkaac
COPY --from=build /workspace/target/*.jar /app.jar
USER nobody
ENTRYPOINT ["java", "-jar", "/app.jar"]
