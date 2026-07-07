FROM maven:17-eclipse-temurin AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/harness-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
