package com.api.moviebooking.models.entities;

import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

import com.api.moviebooking.models.enums.SeatStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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
@Table(name = "showtime_seats")
public class ShowtimeSeat {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY)
    private Seat seat;

    private SeatStatus status;
    private BigDecimal price;
}
