package com.api.moviebooking.models.entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "booking_promotions")
public class BookingPromotion {

    @EmbeddedId
    private BookingPromotionId id;

    @ManyToOne
    @MapsId("bookingId")
    private Booking booking;

    @ManyToOne
    @MapsId("promotionId")
    private Promotion promotion;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingPromotionId implements Serializable {
        private UUID bookingId;
        private UUID promotionId;
    }
}
