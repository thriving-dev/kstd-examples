package dev.thriving.poc;

import dev.thriving.poc.airtravel.avro.AirportInfoI18n;
import dev.thriving.poc.airtravel.avro.Flight;
import dev.thriving.poc.airtravel.avro.FlightEnriched;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

public class AirportEnrichmentProcessor extends ContextualProcessor<String, Flight, String, FlightEnriched> {

    private KeyValueStore<String, AirportInfoI18n> airportInfoStore;

    @Override
    public void init(ProcessorContext<String, FlightEnriched> context) {
        super.init(context);
        this.airportInfoStore = context.getStateStore(KStreamsTopologyFactory.STATE_STORE_AIRPORT_INFO);
    }

    @Override
    public void process(Record<String, Flight> record) {

    }
}
