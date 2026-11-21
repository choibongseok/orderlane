FROM eclipse-temurin:21-jre
COPY build/libs/orderlane-app.jar /app/orderlane-app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/orderlane-app.jar"]
