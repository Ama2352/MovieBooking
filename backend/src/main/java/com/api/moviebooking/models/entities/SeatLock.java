package com.api.moviebooking.models.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a temporary seat lock during booking process
 * Locks are automatically released after expiry time or when booking is
 * confirmed/cancelled
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "seat_locks")
public class SeatLock {

    @Id
    @UuidGenerator
    private UUID id;

    /**
     * The unique lock token (UUID) used as the VALUE in Redis seat locks
     * This token is stored in Redis for each seat:
     * "lock:seat:{showtimeId}:{seatId}" → lockKey
     * When releasing locks, we compare this token to ensure only the lock owner can
     * release
     * Format: Random UUID string (e.g., "8f4c2e9a-1b3d-4f6e-9c8b-7a5d4f3e2c1b")
     */
    @Column(unique = true, nullable = false)
    private String lockKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Showtime showtime;

    /**
     * List of seats locked in this session
     */
    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "seat_lock_seats", joinColumns = @JoinColumn(name = "seat_lock_id"), inverseJoinColumns = @JoinColumn(name = "showtime_seat_id"))
    private List<ShowtimeSeat> lockedSeats = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean active = true;
}
