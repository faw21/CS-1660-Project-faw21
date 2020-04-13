FROM openjdk:9
COPY . aaronwu9774/cs1660project
WORKDIR aaronwu9774/cs1660project
RUN apt-get update
RUN apt-get -y install maven
RUN mvn clean package -DskipTests
ENV GOOGLE_APPLICATION_CREDENTIALS ./credential.json
CMD ["mvn", "exec:java"]