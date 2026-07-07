FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY pom.xml .
RUN apt-get update && apt-get install -y maven
RUN mvn dependency:go-offline
COPY src ./src
COPY harness.yaml .
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/harness-1.0.0.jar harness.jar
COPY harness.yaml .
ENTRYPOINT ["java", "-jar", "harness.jar"]