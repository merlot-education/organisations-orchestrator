FROM maven:3.8-jdk-17-slim AS build
COPY . /opt/
RUN mvn -f /opt/pom.xml clean package

FROM openjdk:17-jre-slim
COPY --from=build /opt/target/organisations-orchestrator-*.jar /opt/organisations-orchestrator.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/opt/organisations-orchestrator.jar"]