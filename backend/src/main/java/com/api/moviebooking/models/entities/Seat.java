package com.api.moviebooking.models.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

import com.api.moviebooking.models.enums.SeatType;

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
@Table(name = "seats")
public class Seat {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Room room;

    private int seatNumber;
    private String rowLabel;
    private SeatType seatType;

    @OneToMany(mappedBy = "seat", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private List<ShowtimeSeat> showtimeSeats = new ArrayList<>();

}
