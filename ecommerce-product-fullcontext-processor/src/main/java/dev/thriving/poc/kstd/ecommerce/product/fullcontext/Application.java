package dev.thriving.poc.kstd.ecommerce.product.fullcontext;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;
import org.apache.kafka.streams.KafkaStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        ApplicationContext context = Micronaut.run(Application.class, args);

        // Retrieve the KafkaStreams bean from the context
        KafkaStreams kafkaStreams = context.getBean(KafkaStreams.class);
        LOG.info("Starting Kafka Streams {}", kafkaStreams);
    }
}
