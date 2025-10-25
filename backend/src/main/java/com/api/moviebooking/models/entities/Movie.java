package com.api.moviebooking.models.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.api.moviebooking.models.enums.MovieStatus;

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
@Table(name = "movies")
public class Movie {

    @Id
    @UuidGenerator
    private UUID id;

    private String title;

    private String genre;

    @Column(columnDefinition = "TEXT")
    private String description;

    private int duration; // in minutes

    private int minimumAge;

    private String director;

    @Column(columnDefinition = "TEXT")
    private String actors;

    private String posterUrl;

    private String posterCloudinaryId;

    private String trailerUrl;

    @Enumerated(EnumType.STRING)
    private MovieStatus status;

    private String language;

    @OneToMany(mappedBy = "movie")
    private List<Showtime> showtimes = new ArrayList<>();

}
