# Stage Compilation
FROM maven:3.9.6-amazoncorretto-21 AS build
WORKDIR /app
COPY pom.xml .
RUN echo "POM file copied"
COPY src ./src
RUN echo "Source files copied"
RUN mvn clean package -DskipTests
RUN echo "Maven build completed"

# Stage Execution
FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /app/target/bys_api-0.0.1.jar .
EXPOSE 8080
ENTRYPOINT ["java","-jar","bys_api-0.0.1.jar"]
