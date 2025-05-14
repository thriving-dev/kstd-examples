package dev.thriving.poc.kstd.airtravel.flight.booking.notification;

import dev.thriving.poc.airtravel.avro.UserFlightBookingEnriched;
import dev.thriving.poc.airtravel.avro.UserFlightBookingNotification;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Duration;

public class FlightBoookingProcessor extends ContextualProcessor<String, UserFlightBookingEnriched, String, UserFlightBookingNotification> {

    private KeyValueStore<String, UserFlightBookingEnriched> flightBookingsStore;

    @Override
    public void init(ProcessorContext<String, UserFlightBookingNotification> context) {
        super.init(context);
        this.flightBookingsStore = context.getStateStore(KStreamsTopologyFactory.STATE_STORE_FLIGHT_BOOKINGS);
        context.schedule(Duration.ofHours(1), PunctuationType.WALL_CLOCK_TIME, timestamp -> {
            flightBookingsStore.all().forEachRemaining(stringUserFlightBookingEnrichedKeyValue -> {
                if (stringUserFlightBookingEnrichedKeyValue.value.getDepartureDate() == "") {
                    flightBookingsStore.delete(stringUserFlightBookingEnrichedKeyValue.key);
                }
            });
        });
    }

    @Override
    public void process(Record<String, UserFlightBookingEnriched> record) {
//        context().forward();
    }
}
