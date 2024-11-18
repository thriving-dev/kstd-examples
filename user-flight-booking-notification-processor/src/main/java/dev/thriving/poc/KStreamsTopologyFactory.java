package dev.thriving.poc;

import dev.thriving.poc.avro.*;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.micronaut.configuration.kafka.streams.ConfiguredStreamBuilder;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyDescription;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.Stores;

import java.util.Collections;
import java.util.Map;

@Factory
public class KStreamsTopologyFactory {

    private static final String SOURCE_TOPIC_USER_FLIGHT_BOOKING = "user_flight_booking_v3";
    private static final String SOURCE_TOPIC_AIRPORT_INFO_I18N = "airport_info_i18n_v5";
    private static final String SOURCE_TOPIC_FLIGHT_STATUS_UPDATE = "flight_status_update_v1";
    private static final String SINK_TOPIC_USER_FLIGHT_BOOKING_NOTIFICATION = "user_flight_booking_notification_v1";

    static final String STATE_STORE_AIRPORT_INFO = "airport-info";
    static final String STATE_STORE_FLIGHT_BOOKINGS = "flight-bookings";

    @Singleton
    KStream<String, UserFlightBookingNotification> exampleStream(ConfiguredStreamBuilder builder) {
        final Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url", "http://localhost:8081");

        Serde<String> stringSerde = Serdes.String();
        SpecificAvroSerde<UserFlightBooking> userFlightBookingSerde = new SpecificAvroSerde<>();
        userFlightBookingSerde.configure(serdeConfig, false);

        builder.addStateStore(Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(STATE_STORE_FLIGHT_BOOKINGS),
                stringSerde,
                userFlightBookingSerde
        ));

        KStream<String, UserFlightBooking> bookings = builder.stream(
                SOURCE_TOPIC_USER_FLIGHT_BOOKING,
                Consumed.as("flight-booking-source"));

        GlobalKTable<String, AirportInfoI18n> airportInfo = builder.globalTable(
                SOURCE_TOPIC_AIRPORT_INFO_I18N,
                Consumed.as("airport-info-source"),
                Materialized.as(Stores.inMemoryKeyValueStore(STATE_STORE_AIRPORT_INFO)));

        KStream<String, FlightStatusUpdate> flightStatusUpdates = builder.stream(
                SOURCE_TOPIC_FLIGHT_STATUS_UPDATE,
                Consumed.as("status-update-source"));

        KStream<String, UserFlightBookingEnriched> bookingsEnriched = bookings
                .process(supplier2(), Named.as("airport-enrichment-processor"));

        bookingsEnriched.repartition(Repartitioned
                        .<String, UserFlightBookingEnriched>as("bookings-enriched-internal")
                        .withNumberOfPartitions(12))
                .process(supplier3(), Named.as("flight-booking-processor"), STATE_STORE_FLIGHT_BOOKINGS);

        KStream<String, UserFlightBookingNotification> notifications = flightStatusUpdates
                .filter((k, v) -> true, Named.as("passenger-flight-filter"))
                .process(supplier(), Named.as("booking-notification-processor"), STATE_STORE_FLIGHT_BOOKINGS);

        notifications.to(SINK_TOPIC_USER_FLIGHT_BOOKING_NOTIFICATION, Produced.as("user-notification-sink"));

        return notifications;
    }

    private ProcessorSupplier<? super String, ? super FlightStatusUpdate, String, UserFlightBookingNotification> supplier() {
        return () -> new ContextualProcessor<>() {
            @Override
            public void process(Record<String, FlightStatusUpdate> record) {
                System.out.println("supplier");
            }
        };
    }


    private ProcessorSupplier<? super String, ? super UserFlightBooking, String, UserFlightBookingEnriched> supplier2() {
        return () -> new ContextualProcessor<>() {
            @Override
            public void process(Record<String, UserFlightBooking> record) {
                System.out.println("supplier2");
            }
        };
    }

    private ProcessorSupplier<? super String, ? super UserFlightBookingEnriched, Object, Object> supplier3() {
        return () -> new ContextualProcessor<>() {
            @Override
            public void process(Record<String, UserFlightBookingEnriched> record) {
                System.out.println("supplier3");
            }
        };
    }
    @Singleton
    public Topology topology(StreamsBuilder streamsBuilder) {
        return streamsBuilder.build();
    }

    @Singleton
    public String topologyDescription(Topology topology) {
        TopologyDescription description = topology.describe();
        return description.toString();
    }
}
