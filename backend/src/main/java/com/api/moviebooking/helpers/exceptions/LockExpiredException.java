package com.api.moviebooking.helpers.exceptions;

public class LockExpiredException extends RuntimeException {

    public LockExpiredException(String message) {
        super(message);
    }

    public LockExpiredException() {
        super("Your seat lock has expired. Please select seats again.");
    }
}
