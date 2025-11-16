package com.api.moviebooking.models.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "cinemas")
public class Cinema {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private String hotline;

    @OneToMany(mappedBy = "cinema", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private List<Room> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "cinema", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private List<Snack> snacks = new ArrayList<>();

}
