package com.api.moviebooking.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.moviebooking.models.entities.Cinema;

public interface CinemaRepo extends JpaRepository<Cinema, UUID> {

}
