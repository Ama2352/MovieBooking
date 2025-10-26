package com.api.moviebooking.helpers.exceptions;

public class MaxSeatsExceededException extends RuntimeException {

    private final int maxSeats;
    private final int requestedSeats;

    public MaxSeatsExceededException(int maxSeats, int requestedSeats) {
        super(String.format("Maximum %d seats allowed per booking. You tried to book %d seats.", maxSeats,
                requestedSeats));
        this.maxSeats = maxSeats;
        this.requestedSeats = requestedSeats;
    }

    public int getMaxSeats() {
        return maxSeats;
    }

    public int getRequestedSeats() {
        return requestedSeats;
    }
}
