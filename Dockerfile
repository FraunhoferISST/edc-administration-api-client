FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

COPY settings.gradle.kts build.gradle.kts ./
#COPY gradle.properties* ./

COPY src ./src

RUN ./gradlew --no-daemon bootJar -x test

RUN mkdir -p /build && cp build/libs/*.jar /build/app.jar

FROM eclipse-temurin:25-jre-alpine AS layers
WORKDIR /app
COPY --from=build /build/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted
RUN mkdir -p extracted/snapshot-dependencies

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup --system spring && adduser --system -G spring spring

COPY --from=layers --chown=spring:spring /app/extracted/dependencies/ ./
COPY --from=layers --chown=spring:spring /app/extracted/spring-boot-loader/ ./
COPY --from=layers --chown=spring:spring /app/extracted/snapshot-dependencies/ ./
COPY --from=layers --chown=spring:spring /app/extracted/application/ ./

USER spring:spring
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -cp /app org.springframework.boot.loader.launch.JarLauncher"]
