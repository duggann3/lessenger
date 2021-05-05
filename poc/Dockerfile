FROM openjdk:8-jdk-alpine
ARG JAR_FILE=target/poc-chatapp-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
RUN apk add curl
ENTRYPOINT ["java","-jar","/app.jar"]