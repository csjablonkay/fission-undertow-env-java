FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD target/undertow-env-java-0.0.1-SNAPSHOT-shaded.jar /app.jar
ENTRYPOINT java ${JVM_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app.jar
EXPOSE 8888
