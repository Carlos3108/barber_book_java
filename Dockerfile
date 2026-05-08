FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# copy maven wrapper and pom first to leverage layer caching
COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml

# copy sources
COPY src src

RUN chmod +x mvnw && ./mvnw -q -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app

# copy the built jar (adjust the name if your artifactId/version change)
COPY --from=builder /app/target/barber_book-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
