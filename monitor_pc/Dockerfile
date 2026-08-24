FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw dependency:go-offline -q
COPY src/ ./src/
RUN ./mvnw clean package -DskipTests -q

FROM eclipse-temurin:25-jre-noble
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
