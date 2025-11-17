package com.api.moviebooking.models.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "rooms")
public class Room {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Cinema cinema;

    private String roomType; // IMAX, 4DX, STARIUM, etc.

    @Column(nullable = false)
    private int roomNumber;

    @OneToMany(mappedBy = "room", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private List<Seat> seats = new ArrayList<>();

    @OneToMany(mappedBy = "room", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private List<Showtime> showtimes = new ArrayList<>();

}
