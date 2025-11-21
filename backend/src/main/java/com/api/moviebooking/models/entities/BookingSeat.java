package com.api.moviebooking.models.entities;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Junction entity that links bookings to showtime seats with ticket type and final price
 * Replaces the simple @ManyToMany relationship to store additional booking seat data
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "booking_seats")
public class BookingSeat {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_seat_id", nullable = false)
    private ShowtimeSeat showtimeSeat;

    /**
     * The ticket type applied to this specific seat at booking time
     * e.g., "adult", "student", "senior"
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_applied_id", nullable = false)
    private TicketType ticketTypeApplied;

    /**
     * The final calculated price for this seat after ticket type modifier
     * = showtime seat base price * ticket type modifier
     */
    @Column(name = "price_final", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceFinal;
}
