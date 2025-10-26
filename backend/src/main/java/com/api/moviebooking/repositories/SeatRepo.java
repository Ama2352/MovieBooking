package com.api.moviebooking.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.moviebooking.models.entities.Seat;

public interface SeatRepo extends JpaRepository<Seat, UUID> {

}
