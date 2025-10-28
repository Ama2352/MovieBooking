package com.api.moviebooking.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.moviebooking.models.entities.Room;

public interface RoomRepo extends JpaRepository<Room, UUID> {

    Optional<Room> findByCinemaIdAndRoomNumber(UUID cinemaId, Integer roomNumber);

    boolean existsByCinemaIdAndRoomNumber(UUID cinemaId, Integer roomNumber);

    boolean existsByCinemaIdAndRoomNumberAndIdNot(UUID cinemaId, Integer roomNumber, UUID id);

}
