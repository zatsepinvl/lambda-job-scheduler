FROM eclipse-temurin:17-jdk-alpine

# '*' matches any sequence of non-Separator characters
ARG JAR_FILE=build/libs/luartz-sample-app-*.jar

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/app.jar"]