FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY target/employee-management-system-1.0.0.jar app.jar

EXPOSE 8080

CMD [ "java", "-jar", "app.jar" ]