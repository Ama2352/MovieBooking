package com.api.moviebooking.models.entities;

import java.time.LocalDateTime;
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
@Table(name = "showtimes")
public class Showtime {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Movie movie;

    private String format; // Ex: 2D Phụ đề Anh, 3D Phụ đề Việt

    @Column(nullable = false)
    private LocalDateTime startTime;

    @OneToMany(mappedBy = "showtime", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    List<ShowtimeSeat> showtimeSeats = new ArrayList<>();

    @OneToMany(mappedBy = "showtime", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    List<SeatLock> seatLocks = new ArrayList<>();

    @OneToMany(mappedBy = "showtime", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    List<Booking> bookings = new ArrayList<>();

}
