# Runtime image: Java 21 only (no Maven, no source code)
FROM eclipse-temurin:21-jre

# Run as a normal user, not root
RUN useradd --system --create-home pwos
WORKDIR /app

# The jar is built by Maven before docker build
COPY target/pwos-0.0.1-SNAPSHOT.jar app.jar
RUN chown pwos:pwos app.jar
USER pwos

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
