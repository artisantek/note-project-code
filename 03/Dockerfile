FROM maven:3.6.3-openjdk-11 as build
WORKDIR /app
COPY . .
RUN mvn clean install

FROM openjdk:11-slim
WORKDIR /app
COPY --from=build /app/target/knote*.jar app.jar
CMD ["java", "-jar", "app.jar"]
