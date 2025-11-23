package com.api.moviebooking.models.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.api.moviebooking.models.enums.LockOwnerType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a temporary seat lock during booking process
 * Locks are automatically released after expiry time or when booking is
 * confirmed/cancelled
 * 
 * - Supports both authenticated users (USER) and guest sessions (GUEST_SESSION)
 * - lockOwnerId: userId for authenticated, sessionId for guests
 * - user: nullable, only populated after guest completes checkout
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

    /**
     * Identifier of the lock owner
     * - For authenticated users: User.id (UUID as string)
     * - For guest sessions: temporary sessionId (UUID as string)
     * This is the primary identifier for lock ownership
     */
    @Column(nullable = false)
    private String lockOwnerId;

    /**
     * Type of lock owner (USER or GUEST_SESSION)
     * Determines how to interpret lockOwnerId
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LockOwnerType lockOwnerType;

    /**
     * Reference to User entity
     * - For authenticated users: set immediately
     * - For guests: NULL until checkout, then populated when User record is created
     * NULLABLE to support guest sessions
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Showtime showtime;

    /**
     * List of seat lock entries with ticket type and price information
     * Each entry represents a locked seat with user-selected ticket type
     */
    @OneToMany(mappedBy = "seatLock", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SeatLockSeat> seatLockSeats = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean active = true;
}
