package jp.gr.java_conf.ogibayashi.prometheus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.prometheus.client.Collector;

public class KafkaCollector extends Collector {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaCollector.class);

    private ObjectMapper mapper = new ObjectMapper();
    private Map<String, Map<KafkaExporterLogEntry, LocalDateTime>> metricEntries = new ConcurrentHashMap<String, Map<KafkaExporterLogEntry, LocalDateTime>>();
    private long expire;
    
    public KafkaCollector(PropertyConfig pc) {
        expire = pc.getMetricExpire();
    }

    public void add(String topic, String recordValue) {
        add(topic, recordValue, LocalDateTime.now());
    }
    
    public void add(String topic, String recordValue, LocalDateTime datetime) {
        LOG.debug("add: {}, {}", topic, recordValue);
        try {          
            KafkaExporterLogEntry record = mapper.readValue(recordValue, KafkaExporterLogEntry.class);
            String metricName = topic.replaceAll("[^A-Za-z0-9]","_") + "_" + record.getName().replaceAll("[^A-Za-z0-9]","_");
            if (metricName.startsWith("_")) {
              metricName = metricName.substring(1);
            }
            if(! metricEntries.containsKey(metricName)){
                metricEntries.put(metricName, new ConcurrentHashMap<KafkaExporterLogEntry, LocalDateTime>());
            }

            Map<KafkaExporterLogEntry, LocalDateTime> entry = metricEntries.get(metricName);
            entry.remove(record);
            entry.put(record, datetime);
        }
        catch(JsonMappingException e){
            LOG.warn("Invalid record: " + recordValue, e);
        }
        catch(Exception e){
            LOG.error("Error happened in adding record to the collector", e);
        }
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return collect(LocalDateTime.now());
    }

    
    public List<MetricFamilySamples> collect(LocalDateTime current_timestamp) {
        
        List<MetricFamilySamples> mfsList = new ArrayList<MetricFamilySamples>();
        for(Map.Entry<String, Map<KafkaExporterLogEntry, LocalDateTime>> e: metricEntries.entrySet()){
            List<MetricFamilySamples.Sample> samples = new ArrayList<MetricFamilySamples.Sample>();
            for(Map.Entry<KafkaExporterLogEntry, LocalDateTime> le: e.getValue().entrySet()){
                if (expire != 0 && le.getValue().plusSeconds(expire).isBefore(current_timestamp)) {
                    e.getValue().remove(le.getKey());
                    if(e.getValue().isEmpty()) {
                        metricEntries.remove(e.getKey());
                    }
                }
                else {
                    samples.add(generateSample(e.getKey(), le.getKey()));
                }
            }
            mfsList.add(new MetricFamilySamples(e.getKey(), Type.GAUGE, "", samples));
        }

        return mfsList;
    }

    public MetricFamilySamples.Sample generateSample(String metricName, KafkaExporterLogEntry logEntry) {
        ArrayList<String> labelNames = new ArrayList<String>();
        ArrayList<String> labelValues = new ArrayList<String>();
        if (logEntry.getLabels() != null) {
            for(Map.Entry<String, String> entry: logEntry.getLabels().entrySet()){
                labelNames.add(entry.getKey().replaceAll("[^A-Za-z0-9]","_"));
                labelValues.add(entry.getValue());
            }
        }
        MetricFamilySamples.Sample sample = new MetricFamilySamples.Sample(metricName, labelNames, labelValues, logEntry.getValue(), logEntry.getTimestamp());
        LOG.debug("sample: {}", sample );
        return sample; 
    }
}
