# Kafka Topic Exporter

Consume Kafka topics and export to Prometheus

### Start process

```
mvn clean package
cd target
echo "Create log4j.properties and kafka-topic-exporter.properties and put it in target folder"
java -jar -Dlog4j.configuration=file:log4j.properties kafka-topic-exporter-0.0.5-jar-with-dependencies.jar kafka-topic-exporter.properties
```

### Run as Docker

The below command are based on the assumption that the properties file is preset under "/usr/local/src/kafka-topic-exporter/kafka-topic-exporter.properties"
```
docker pull dmathai/prom-kafka-topic-exporter:latest
docker run -d -v /var/log/:/var/log/ -v /usr/local/src/kafka-topic-exporter/:/usr/local/src/kafka-topic-exporter/ -p 9211:9211 -e PATH_PROPERTY_FILE=/usr/local/src/kafka-topic-exporter/kafka-topic-exporter.properties dmathai/prom-kafka-topic-exporter:latest
```

### Configuration

The configuration file must be passed as parameter when starting the process.

```
## Kafka Exporter Properties

# HTTP port used for the exporter
exporter.port=12340

# Time in seconds that the metrics, once retrieved, will consider as valid
exporter.metric.expire.seconds=60

# RegEx for filtering topics to retrieve
kafka.consumer.topics=export\..*

# RegEx for removing substring from Kafka info
kafka.consumer.remove.prefix=export\.

## Kafka core properties

# A list of host/port pairs to use for establishing the initial connection to the Kafka cluster.
# The client will make use of all servers irrespective of which servers are specified here for bootstrapping
# this list only impacts the initial hosts used to discover the full set of servers. This list should be in
# the form <code>host1:port1,host2:port2,...</code>. Since these servers are just used for the initial
# connection to discover the full cluster membership (which may change dynamically), this list need not
# contain the full set of servers (you may want more than one, though, in case a server is down).
bootstrap.servers=localhost:9092

# A unique string that identifies the consumer group this consumer belongs to. This property is required if
# the consumer uses either the group management functionality by using <code>subscribe(topic)</code> or the
# Kafka-based offset management strategy.
# kte = kafka-topic-exporter
group.id=kte-group

# An id string to pass to the server when making requests. The purpose of this is to be able to track the
# source of requests beyond just ip/port by allowing a logical application name to be included in
# server-side request logging.
# kte = kafka-topic-exporter
client.id=kte
```

* exporter.metric.expire.seconds \[default: 0 (no expire)\]
    * When a metric (name & labels) is not updated for this time period (in second), it will be removed from exporter response.

### Record format

Each record in the topics should be the following format. `timestamp` and `labels` are optional.

```
{
  "name": "<metric_name>",
  "value": <metric_value>,
  "timestamp": <epoch_value_with_millis>,
  "labels: {
    "foolabel": "foolabelvalue",
    "barlabel": "barlabelvalue"
  }
}
```


Then the following item will be exported.

```
<kafka_topic_name>_<metric_name>{foolabel="foolabelvalue", barlabel="barlabelvalue"} <metric_value> <epoch_value>
```
