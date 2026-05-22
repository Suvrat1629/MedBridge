FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY build/libs/*.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8086
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
