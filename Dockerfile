FROM gradle:7.5-jdk11 AS build

WORKDIR /app

COPY *.gradle.kts gradle.properties ./
RUN gradle build --no-daemon

COPY src/ src/
RUN gradle build --no-daemon

FROM openjdk:11-slim

LABEL org.opencontainers.image.source=https://github.com/CascadeBot/Bot
LABEL org.opencontainers.image.description="Cascade Discord Bot"
LABEL org.opencontainers.image.licenses=MIT

WORKDIR /app

COPY --from=build "/app/build/libs/bot-shadow.jar" "bot.jar"

CMD ["java", "-jar", "bot.jar"]