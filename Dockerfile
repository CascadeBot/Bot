FROM gradle:7.5-jdk11 AS dependencyCache

WORKDIR /home/gradle

COPY *.gradle.kts ./
COPY gradle.properties ./

RUN gradle downloadDependencies --no-daemon

FROM gradle:7.5-jdk11 AS build

WORKDIR /home/gradle

COPY --from=dependencyCache /root/.gradle /root/.gradle
COPY *.gradle.kts ./
COPY gradle.properties .
COPY src/ src/

RUN gradle build --no-daemon

FROM openjdk:11-slim

WORKDIR /home/cascade

COPY --from=build "/home/gradle/build/libs/bot-shadow.jar" "bot.jar"

CMD java -jar bot.jar