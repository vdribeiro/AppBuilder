# Java runtime
FROM eclipse-temurin:21-jre-alpine
# Working directory
WORKDIR /app
# Copy the Fat JAR
COPY build/libs/*-all.jar /app/server.jar
# Run the server
ENTRYPOINT ["java", "-Dlogback.configurationFile=logback.xml", "-jar", "/app/server.jar"]
