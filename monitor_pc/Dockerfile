FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app
COPY monitor_pc/.mvn/ .mvn/
COPY monitor_pc/mvnw monitor_pc/pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw dependency:go-offline -q
COPY monitor_pc/src/ ./src/
RUN ./mvnw clean package -Dmaven.test.skip=true -q

FROM eclipse-temurin:25-jre-noble
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
