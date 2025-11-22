package com.api.moviebooking.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.api.moviebooking.models.enums.ModifierType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
@Table(name = "ticket_types")
public class TicketType {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // adult, student, senior, member, double

    @Column(nullable = false, length = 100)
    private String label; // Display label: NGƯỜI LỚN, HSSV/U22-GV, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModifierType modifierType; // PERCENTAGE or FIXED_AMOUNT

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal modifierValue; // Modifier value (percentage or fixed amount)

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
