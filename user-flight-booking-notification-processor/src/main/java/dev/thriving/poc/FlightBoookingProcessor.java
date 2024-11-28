package dev.thriving.poc;

import dev.thriving.poc.airtravel.avro.UserFlightBookingEnriched;
import dev.thriving.poc.airtravel.avro.UserFlightBookingNotification;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

public class FlightBoookingProcessor extends ContextualProcessor<String, UserFlightBookingEnriched, String, UserFlightBookingNotification> {

    private KeyValueStore<String, UserFlightBookingEnriched> flightBookingsStore;

    @Override
    public void init(ProcessorContext<String, UserFlightBookingNotification> context) {
        super.init(context);
        this.flightBookingsStore = context.getStateStore(KStreamsTopologyFactory.STATE_STORE_FLIGHT_BOOKINGS);
    }

    @Override
    public void process(Record<String, UserFlightBookingEnriched> record) {

    }
}