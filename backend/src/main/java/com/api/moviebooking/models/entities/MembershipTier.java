package com.api.moviebooking.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.api.moviebooking.models.enums.DiscountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
@Table(name = "membership_tiers")
public class MembershipTier {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // SILVER, GOLD, PLATINUM

    @Column(nullable = false)
    private Integer minPoints; // Điểm cần đạt để lên tier

    @Enumerated(EnumType.STRING)
    private DiscountType discountType; // PERCENTAGE / FIXED_AMOUNT (nullable)

    @Column(precision = 10, scale = 2)
    private BigDecimal discountValue; // Giá trị giảm giá (nullable)

    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả (nullable)

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "membershipTier")
    private List<User> users = new ArrayList<>();
}
