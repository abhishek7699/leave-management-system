FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY employee-service/pom.xml ./employee-service/pom.xml
COPY leave-service/pom.xml ./leave-service/pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY employee-service/src ./employee-service/src
RUN mvn -q -pl employee-service package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/employee-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
