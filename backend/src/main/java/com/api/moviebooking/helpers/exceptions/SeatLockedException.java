package com.api.moviebooking.helpers.exceptions;

import java.util.List;
import java.util.UUID;

public class SeatLockedException extends RuntimeException {

    private final List<UUID> lockedSeatIds;

    public SeatLockedException(String message, List<UUID> lockedSeatIds) {
        super(message);
        this.lockedSeatIds = lockedSeatIds;
    }

    public SeatLockedException(List<UUID> lockedSeatIds) {
        super("One or more seats are already locked by another user");
        this.lockedSeatIds = lockedSeatIds;
    }

    public List<UUID> getLockedSeatIds() {
        return lockedSeatIds;
    }
}
