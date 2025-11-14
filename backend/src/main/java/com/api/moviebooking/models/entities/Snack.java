package com.api.moviebooking.models.entities;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
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
@Table(name = "snacks")
public class Snack {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Cinema cinema;

    private String name;
    private String description;
    private BigDecimal price;
    private String type; // e.g., Popcorn, Pepsi, Combo 1, etc.
    private String imageUrl;
    private String imageCloudinaryId;

    @OneToMany(mappedBy = "snack", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    List<BookingSnack> bookingSnacks = new ArrayList<>();
}
