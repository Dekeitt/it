FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src src
RUN ./mvnw -B -ntp clean package

FROM eclipse-temurin:17-jre-jammy
RUN useradd --system --uid 10001 --create-home appuser
WORKDIR /app
COPY --from=build /workspace/target/it-0.0.1-SNAPSHOT.jar app.jar
USER appuser
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
