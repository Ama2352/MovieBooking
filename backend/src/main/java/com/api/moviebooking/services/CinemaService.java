package com.api.moviebooking.services;

import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.EntityDeletionForbiddenException;
import com.api.moviebooking.helpers.mapstructs.CinemaMapper;
import com.api.moviebooking.helpers.mapstructs.RoomMapper;
import com.api.moviebooking.helpers.mapstructs.SnackMapper;
import com.api.moviebooking.models.dtos.cinema.AddCinemaRequest;
import com.api.moviebooking.models.dtos.cinema.CinemaDataResponse;
import com.api.moviebooking.models.dtos.cinema.UpdateCinemaRequest;
import com.api.moviebooking.models.dtos.room.AddRoomRequest;
import com.api.moviebooking.models.dtos.room.RoomDataResponse;
import com.api.moviebooking.models.dtos.room.UpdateRoomRequest;
import com.api.moviebooking.models.dtos.snack.AddSnackRequest;
import com.api.moviebooking.models.dtos.snack.SnackDataResponse;
import com.api.moviebooking.models.dtos.snack.UpdateSnackRequest;
import com.api.moviebooking.models.entities.Cinema;
import com.api.moviebooking.models.entities.Room;
import com.api.moviebooking.models.entities.Snack;
import com.api.moviebooking.repositories.CinemaRepo;
import com.api.moviebooking.repositories.RoomRepo;
import com.api.moviebooking.repositories.SnackRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CinemaService {

    private final CinemaRepo cinemaRepo;
    private final CinemaMapper cinemaMapper;
    private final RoomRepo roomRepo;
    private final RoomMapper roomMapper;
    private final SnackRepo snackRepo;
    private final SnackMapper snackMapper;

    private Cinema findCinemaById(UUID cinemaId) {
        return cinemaRepo.findById(cinemaId)
                .orElseThrow(() -> new RuntimeException("Cinema not found"));
    }

    public CinemaDataResponse addCinema(AddCinemaRequest request) {
        Cinema newCinema = cinemaMapper.toEntity(request);
        cinemaRepo.save(newCinema);
        return cinemaMapper.toDataResponse(newCinema);
    }

    public CinemaDataResponse updateCinema(UUID cinemaId, UpdateCinemaRequest request) {
        Cinema cinema = findCinemaById(cinemaId);
        if (request.getName() != null) {
            cinema.setName(request.getName());
        }
        if (request.getAddress() != null) {
            cinema.setAddress(request.getAddress());
        }
        if (request.getHotline() != null) {
            cinema.setHotline(request.getHotline());
        }
        cinemaRepo.save(cinema);
        return cinemaMapper.toDataResponse(cinema);
    }

    // For testing purpose only
    public void deleteCinema_violatesForeignKeyConstraint(UUID id) {
        Cinema cinema = findCinemaById(id);
        cinemaRepo.delete(cinema);
    }

    public void deleteCinema(UUID id) {
        Cinema cinema = findCinemaById(id);

        if (!cinema.getRooms().isEmpty()) {
            throw new EntityDeletionForbiddenException();
        } else if (!cinema.getSnacks().isEmpty()) {
            throw new EntityDeletionForbiddenException();
        }

        cinemaRepo.delete(cinema);
    }

    public CinemaDataResponse getCinema(UUID cinemaId) {
        Cinema cinema = findCinemaById(cinemaId);
        return cinemaMapper.toDataResponse(cinema);
    }

    // Room CRUD methods
    private Room findRoomById(UUID roomId) {
        return roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
    }

    public RoomDataResponse addRoom(AddRoomRequest request) {
        Cinema cinema = findCinemaById(request.getCinemaId());
        Room newRoom = roomMapper.toEntity(request);
        newRoom.setCinema(cinema);
        roomRepo.save(newRoom);
        return roomMapper.toDataResponse(newRoom);
    }

    public RoomDataResponse updateRoom(UUID roomId, UpdateRoomRequest request) {
        Room room = findRoomById(roomId);
        if (request.getRoomType() != null) {
            room.setRoomType(request.getRoomType());
        }
        if (request.getRoomNumber() != null) {
            room.setRoomNumber(request.getRoomNumber());
        }
        roomRepo.save(room);
        return roomMapper.toDataResponse(room);
    }

    public void deleteRoom(UUID id) {
        Room room = findRoomById(id);
        roomRepo.delete(room);
    }

    public RoomDataResponse getRoom(UUID roomId) {
        Room room = findRoomById(roomId);
        return roomMapper.toDataResponse(room);
    }

    // Snack CRUD methods
    private Snack findSnackById(UUID snackId) {
        return snackRepo.findById(snackId)
                .orElseThrow(() -> new RuntimeException("Snack not found"));
    }

    public SnackDataResponse addSnack(AddSnackRequest request) {
        Cinema cinema = findCinemaById(request.getCinemaId());
        Snack newSnack = snackMapper.toEntity(request);
        newSnack.setCinema(cinema);
        snackRepo.save(newSnack);
        return snackMapper.toDataResponse(newSnack);
    }

    public SnackDataResponse updateSnack(UUID snackId, UpdateSnackRequest request) {
        Snack snack = findSnackById(snackId);
        if (request.getName() != null) {
            snack.setName(request.getName());
        }
        if (request.getDescription() != null) {
            snack.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            snack.setPrice(request.getPrice());
        }
        if (request.getType() != null) {
            snack.setType(request.getType());
        }
        snackRepo.save(snack);
        return snackMapper.toDataResponse(snack);
    }

    public void deleteSnack(UUID id) {
        Snack snack = findSnackById(id);
        snackRepo.delete(snack);
    }

    public SnackDataResponse getSnack(UUID snackId) {
        Snack snack = findSnackById(snackId);
        return snackMapper.toDataResponse(snack);
    }

    // Get all methods
    public List<CinemaDataResponse> getAllCinemas() {
        return cinemaRepo.findAll().stream()
                .map(cinemaMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    public List<RoomDataResponse> getAllRooms() {
        return roomRepo.findAll().stream()
                .map(roomMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    public List<SnackDataResponse> getAllSnacks() {
        return snackRepo.findAll().stream()
                .map(snackMapper::toDataResponse)
                .collect(Collectors.toList());
    }
}
