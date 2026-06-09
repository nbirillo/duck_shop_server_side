# Build the runnable (fat) jar first:  ./gradlew bootJar
# Then:                                 docker build -t duck-shop .
#                                       docker run -p 8080:8080 duck-shop
FROM eclipse-temurin:21-jre
WORKDIR /app
# The Spring Boot bootJar (not the "-plain" library jar)
COPY build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
