package com.api.moviebooking.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.api.moviebooking.models.enums.BookingStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "bookings")
public class Booking {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Showtime showtime;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime bookedAt;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice; // Giá gốc trước khi giảm

    private String discountReason; // Lý do giảm giá (tên promotion hoặc lý do khác)

    @Column(precision = 10, scale = 2)
    private BigDecimal discountValue; // Số tiền được giảm

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal finalPrice; // Giá cuối cùng sau khi giảm

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    private String qrCode;

    @Column(columnDefinition = "TEXT")
    private String qrPayload;

    private LocalDateTime paymentExpiresAt;

    @Column(nullable = false)
    private boolean loyaltyPointsAwarded = false;

    @Column(nullable = false)
    private boolean refunded = false;

    private LocalDateTime refundedAt;

    private String refundReason;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "booking_seats", joinColumns = @JoinColumn(name = "booking_id"), inverseJoinColumns = @JoinColumn(name = "showtime_seat_id"))
    private List<ShowtimeSeat> bookedSeats = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingPromotion> bookingPromotions = new ArrayList<>();
}
