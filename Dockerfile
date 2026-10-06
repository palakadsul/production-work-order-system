# Runtime image: Java 21 only (no Maven, no source code)
FROM eclipse-temurin:21-jre

# Run as a normal user, not root
RUN useradd --system --create-home pwos
WORKDIR /app

# The jar is built by Maven before docker build.
# --chown sets the owner while copying, so the jar
# is stored in one layer instead of two.
COPY --chown=pwos:pwos target/pwos-0.0.1-SNAPSHOT.jar app.jar
USER pwos

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
