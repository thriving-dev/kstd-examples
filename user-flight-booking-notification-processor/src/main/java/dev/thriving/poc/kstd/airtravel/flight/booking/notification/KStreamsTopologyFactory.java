package dev.thriving.poc.kstd.airtravel.flight.booking.notification;

import dev.thriving.poc.airtravel.avro.*;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.micronaut.configuration.kafka.streams.ConfiguredStreamBuilder;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.Stores;

import java.util.Collections;
import java.util.Map;

@Factory
public class KStreamsTopologyFactory {

    private static final String SOURCE_TOPIC_USER_FLIGHT_BOOKING = "user_flight_booking_v3";
    private static final String SOURCE_TOPIC_FLIGHT = "flight_v2";
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

        // custom state stores
        builder.addStateStore(Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(STATE_STORE_FLIGHT_BOOKINGS),
                stringSerde,
                userFlightBookingSerde
        ));

        // sources
        KStream<String, UserFlightBooking> bookings = builder.stream(
                SOURCE_TOPIC_USER_FLIGHT_BOOKING,
                Consumed.as("flight-booking-source"));

        KStream<String, Flight> flights = builder.stream(
                SOURCE_TOPIC_FLIGHT,
                Consumed.as("flight-source"));

        GlobalKTable<String, AirportInfoI18n> airportInfo = builder.globalTable(
                SOURCE_TOPIC_AIRPORT_INFO_I18N,
                Consumed.as("airport-info-source"),
                Materialized.as(Stores.inMemoryKeyValueStore(STATE_STORE_AIRPORT_INFO)));

        KStream<String, FlightStatusUpdate> flightStatusUpdates = builder.stream(
                SOURCE_TOPIC_FLIGHT_STATUS_UPDATE,
                Consumed.as("status-update-source"));

        // topology
        KStream<String, Flight> passengerFlightsRepartitioned = flights
                .filter(new PassengerFlightFilter(), Named.as("passenger-flight-filter"))
                .repartition(Repartitioned.<String, Flight>as("passenger-flights-repartitioned").withNumberOfPartitions(12));

        KTable<String, FlightEnriched> flightsEnriched = passengerFlightsRepartitioned
                .process(AirportEnrichmentProcessor::new, Named.as("airport-enrichment-processor"))
                .toTable(Named.as("flights-enriched-table"));

        KStream<String, UserFlightBookingEnriched> bookingsEnriched = bookings
                .selectKey((k, v) -> v.getDepartureDate() + "_" + v.getFlightNumber())
                .join(flightsEnriched, new BookingToFlightKVJoiner(), Joined.as("bookings-to-flights-join"));

        KStream<String, UserFlightBookingNotification> notifications = bookingsEnriched
                .process(FlightBoookingProcessor::new, Named.as("flight-booking-processor"), STATE_STORE_FLIGHT_BOOKINGS)
                .merge(flightStatusUpdates
                        .process(BookingNotificationProcessor::new, Named.as("booking-notification-processor"), STATE_STORE_FLIGHT_BOOKINGS));

        // sink
        notifications.to(SINK_TOPIC_USER_FLIGHT_BOOKING_NOTIFICATION, Produced.as("user-notification-sink"));

        // (return any KStream for micronaut-kafka)
        return notifications;
    }


}
