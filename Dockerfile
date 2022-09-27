FROM gradle:7.5-jdk11 AS build

WORKDIR /home/gradle

COPY . .

RUN gradle build --no-daemon

FROM openjdk:11-slim

COPY --from=build "/home/gradle/build/libs/bot-shadow.jar" "bot.jar"

RUN java -jar bot.jar