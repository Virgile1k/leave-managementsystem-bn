 FROM eclipse-temurin:21-jdk-alpine AS build
 WORKDIR /workspace/app

 # Install maven
 RUN apk add --no-cache maven

 # Copy pom.xml first for better layer caching
 COPY pom.xml .

 # Copy source code
 COPY src src

 # Build the application
 RUN mvn package -DskipTests

 FROM eclipse-temurin:21-jre-alpine
 VOLUME /tmp
 COPY --from=build /workspace/app/target/*.jar app.jar
 ENTRYPOINT ["java","-jar","/app.jar"]