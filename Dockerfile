FROM alpine/git
WORKDIR /app
COPY . /app

FROM maven:3.5-jdk-8-alpine
WORKDIR /app
COPY --from=0 /app/auction_mechanism /app
RUN mvn package

FROM openjdk:8-jre-alpine
WORKDIR /app
ENV MASTERIP=127.0.0.1
ENV ID=0
COPY --from=1 /app/target/auction_mechanism-1.0-SNAPSHOT-jar-with-dependencies.jar /app

CMD /usr/bin/java -jar auction_mechanism-1.0-SNAPSHOT-jar-with-dependencies.jar -m $MASTERIP -id $ID