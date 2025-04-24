  FROM eclipse-temurin:17-jdk-alpine AS build
  WORKDIR /workspace/app

  # Copy pom.xml first for better layer caching
  COPY pom.xml .
  COPY mvnw .
  # Only copy .mvn if it exists (removed to fix your error)
  # COPY .mvn .mvn (removed this line)

  # Make mvnw executable
  RUN chmod +x ./mvnw

  # Copy source code
  COPY src src

  # Build the application
  RUN ./mvnw package -DskipTests

  FROM eclipse-temurin:17-jre-alpine
  VOLUME /tmp
  COPY --from=build /workspace/app/target/*.jar app.jar
  ENTRYPOINT ["java","-jar","/app.jar"]