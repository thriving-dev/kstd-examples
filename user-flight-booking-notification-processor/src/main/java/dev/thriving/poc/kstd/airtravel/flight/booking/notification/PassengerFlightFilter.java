package dev.thriving.poc.kstd.airtravel.flight.booking.notification;

import dev.thriving.poc.airtravel.avro.Flight;

public class PassengerFlightFilter implements org.apache.kafka.streams.kstream.Predicate<String, Flight> {
    @Override
    public boolean test(String s, Flight flight) {
        return false;
    }
}
