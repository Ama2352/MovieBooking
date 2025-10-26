package com.api.moviebooking.models.enums;

public enum SeatStatus {
    AVAILABLE, // Seat is available for booking
    LOCKED, // Seat is temporarily locked during checkout process
    BOOKED // Seat is confirmed and booked
}
