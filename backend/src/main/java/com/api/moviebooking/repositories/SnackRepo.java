package com.api.moviebooking.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.moviebooking.models.entities.Snack;

public interface SnackRepo extends JpaRepository<Snack, UUID> {

    Optional<Snack> findByCinemaIdAndName(UUID cinemaId, String name);

    boolean existsByCinemaIdAndName(UUID cinemaId, String name);

    boolean existsByCinemaIdAndNameAndIdNot(UUID cinemaId, String name, UUID id);
}
