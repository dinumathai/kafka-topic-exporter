package jp.gr.java_conf.ogibayashi.prometheus;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.exporter.MetricsServlet;

public class KafkaTopicExporter {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaTopicExporter.class);

    public static void main( String[] args ) throws IOException
    {
    	if(args.length == 0)
        {
            System.out.println("Proper Usage is: java -jar <kafka-topic-exporter>.jar <config file>.properties");
            System.exit(0);
        }
        final PropertyConfig pc = new PropertyConfig(args[0]);

        int serverPort = pc.getExporterPort();
        Server server = new Server(serverPort);
        LOG.info("starting server on port {}", serverPort);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
        
       
        ExecutorService executor = Executors.newSingleThreadExecutor();
        KafkaCollector kc = new KafkaCollector(pc).register();
        
        final Future consumer = executor.submit(new Thread(new ConsumerThread(kc,pc)));
        
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                public void run() {
                    LOG.info("Shutting down");
                    consumer.cancel(true);
                    executor.shutdown();
                    try {
                        if(executor.awaitTermination(3000L, TimeUnit.MILLISECONDS)) {
                            LOG.info("Shutdown completed");
                        } else {
                            LOG.info("Shutdown timed out");
                        }
                    }catch (InterruptedException e) {
                        LOG.info("Shut down interrupted");
                    }
                }
            }));

        
        try {
            server.start();
            server.join();
        }
        catch (Exception e) {
            LOG.error("Error happend in server", e);
        }
        finally{
            consumer.cancel(true);
            executor.shutdown();
        }
                
    }        
    
}
