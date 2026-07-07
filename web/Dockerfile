# Multi-stage build: Java JAR + Python web server
FROM eclipse-temurin:17-jdk AS java-build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw.cmd .
RUN apt-get update && apt-get install -y maven
COPY src ./src
COPY harness.yaml .
RUN mvn package -DskipTests -q

FROM python:3.12-slim
WORKDIR /app
RUN apt-get update && apt-get install -y openjdk-17-jre-headless && rm -rf /var/lib/apt/lists/*
COPY --from=java-build /app/target/harness-1.0.0.jar ./target/harness-1.0.0.jar
COPY --from=java-build /app/harness.yaml .
COPY web/requirements.txt ./web/requirements.txt
RUN pip install --no-cache-dir -r web/requirements.txt
COPY web/ ./web/
WORKDIR /app/web
EXPOSE 5000
CMD ["gunicorn", "-k", "gevent", "-w", "1", "-b", "0.0.0.0:5000", "app:app"]