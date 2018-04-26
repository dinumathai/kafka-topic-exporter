FROM openjdk:8-jre-alpine3.7
MAINTAINER dinumathai@gmail.com

WORKDIR /kafka-topic-exporter

ADD target/kafka-topic-exporter.jar /kafka-topic-exporter/kafka-topic-exporter.jar
ADD start.sh /kafka-topic-exporter/start.sh
ADD config/log.properties /kafka-topic-exporter/log.properties
RUN chmod 777 /kafka-topic-exporter/start.sh

ENTRYPOINT ["/kafka-topic-exporter/start.sh"]
