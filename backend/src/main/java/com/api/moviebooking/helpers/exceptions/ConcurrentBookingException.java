package com.api.moviebooking.helpers.exceptions;

public class ConcurrentBookingException extends RuntimeException {

    public ConcurrentBookingException(String message) {
        super(message);
    }

    public ConcurrentBookingException() {
        super("Unable to complete booking due to concurrent request. Please try again.");
    }
}
