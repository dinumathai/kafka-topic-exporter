#!/bin/sh
if [ -z ${PATH_PROPERTY_FILE+x}]; then
  export PATH_PROPERTY_FILE=kafka-topic-exporter.properties
fi

java -jar -Dlog4j.configuration=file:log.properties kafka-topic-exporter.jar $PATH_PROPERTY_FILE >> /var/log/app/kafka-topic-exporter.log
