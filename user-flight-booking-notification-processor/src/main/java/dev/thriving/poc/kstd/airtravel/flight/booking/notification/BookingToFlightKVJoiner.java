package dev.thriving.poc.kstd.airtravel.flight.booking.notification;

import dev.thriving.poc.airtravel.avro.FlightEnriched;
import dev.thriving.poc.airtravel.avro.UserFlightBooking;
import dev.thriving.poc.airtravel.avro.UserFlightBookingEnriched;
import org.apache.kafka.streams.kstream.ValueJoinerWithKey;

public class BookingToFlightKVJoiner implements ValueJoinerWithKey<String, UserFlightBooking, FlightEnriched, UserFlightBookingEnriched> {
    @Override
    public UserFlightBookingEnriched apply(String s, UserFlightBooking userFlightBooking, FlightEnriched flightEnriched) {
        return null;
    }
}
