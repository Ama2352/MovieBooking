package com.api.moviebooking.services;

import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.DuplicateResourceException;
import com.api.moviebooking.helpers.exceptions.EntityDeletionForbiddenException;
import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
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
                .orElseThrow(() -> new ResourceNotFoundException("Cinema", "id", cinemaId));
    }

    /**
     * Add a new cinema
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: existsByName
     */
    public CinemaDataResponse addCinema(AddCinemaRequest request) {
        // Check for duplicate cinema name
        if (cinemaRepo.existsByName(request.getName())) {
            throw new DuplicateResourceException("Cinema", "name", request.getName());
        }

        Cinema newCinema = cinemaMapper.toEntity(request);
        cinemaRepo.save(newCinema);
        return cinemaMapper.toDataResponse(newCinema);
    }

    /**
     * Update cinema information
     * Predicate nodes (d): 5 -> V(G) = d + 1 = 6
     * Nodes: findCinemaById, name!=null, existsByNameAndIdNot, address!=null,
     * hotline!=null
     */
    public CinemaDataResponse updateCinema(UUID cinemaId, UpdateCinemaRequest request) {
        Cinema cinema = findCinemaById(cinemaId);

        // Check for duplicate cinema name (if name is being updated)
        if (request.getName() != null) {
            if (cinemaRepo.existsByNameAndIdNot(request.getName(), cinemaId)) {
                throw new DuplicateResourceException("Cinema", "name", request.getName());
            }
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

    // For testing purpose only (this is not a main method)
    public void deleteCinema_violatesForeignKeyConstraint(UUID id) {
        Cinema cinema = findCinemaById(id);
        cinemaRepo.delete(cinema);
    }

    /**
     * Delete cinema with validation
     * Predicate nodes (d): 3 -> V(G) = d + 1 = 4
     * Nodes: findCinemaById, !isEmpty(rooms), !isEmpty(snacks)
     */
    public void deleteCinema(UUID id) {
        Cinema cinema = findCinemaById(id);

        if (!cinema.getRooms().isEmpty()) {
            throw new EntityDeletionForbiddenException("Cannot delete cinema with existing rooms");
        } else if (!cinema.getSnacks().isEmpty()) {
            throw new EntityDeletionForbiddenException("Cannot delete cinema with existing snacks");
        }

        cinemaRepo.delete(cinema);
    }

    /**
     * Get cinema by ID
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findCinemaById
     */
    public CinemaDataResponse getCinema(UUID cinemaId) {
        Cinema cinema = findCinemaById(cinemaId);
        return cinemaMapper.toDataResponse(cinema);
    }

    private Room findRoomById(UUID roomId) {
        return roomRepo.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
    }

    /**
     * Add a new room to a cinema
     * Predicate nodes (d): 2 -> V(G) = d + 1 = 3
     * Nodes: findCinemaById, existsByCinemaIdAndRoomNumber
     */
    public RoomDataResponse addRoom(AddRoomRequest request) {
        Cinema cinema = findCinemaById(request.getCinemaId());

        // Check for duplicate room number in the same cinema
        if (roomRepo.existsByCinemaIdAndRoomNumber(request.getCinemaId(), request.getRoomNumber())) {
            throw new DuplicateResourceException(
                    String.format("Room number %d already exists in cinema '%s'",
                            request.getRoomNumber(), cinema.getName()));
        }

        Room newRoom = roomMapper.toEntity(request);
        newRoom.setCinema(cinema);
        roomRepo.save(newRoom);
        return roomMapper.toDataResponse(newRoom);
    }

    /**
     * Update room information
     * Predicate nodes (d): 4 -> V(G) = d + 1 = 5
     * Nodes: findRoomById, roomType!=null, roomNumber!=null,
     * existsByCinemaIdAndRoomNumberAndIdNot
     */
    public RoomDataResponse updateRoom(UUID roomId, UpdateRoomRequest request) {
        Room room = findRoomById(roomId);

        if (request.getRoomType() != null) {
            room.setRoomType(request.getRoomType());
        }
        if (request.getRoomNumber() != null) {
            // Check for duplicate room number in the same cinema (if room number is being
            // updated)
            if (roomRepo.existsByCinemaIdAndRoomNumberAndIdNot(
                    room.getCinema().getId(), request.getRoomNumber(), roomId)) {
                throw new DuplicateResourceException(
                        String.format("Room number %d already exists in cinema '%s'",
                                request.getRoomNumber(), room.getCinema().getName()));
            }
            room.setRoomNumber(request.getRoomNumber());
        }
        roomRepo.save(room);
        return roomMapper.toDataResponse(room);
    }

    /**
     * Delete a room
     * Predicate nodes (d): 3 -> V(G) = d + 1 = 4
     * Nodes: findRoomById, !isEmpty(seats), !isEmpty(showtimes)
     */
    public void deleteRoom(UUID id) {
        Room room = findRoomById(id);

        if (!room.getSeats().isEmpty()) {
            throw new EntityDeletionForbiddenException("Cannot delete room with existing seats");
        } else if (!room.getShowtimes().isEmpty()) {
            throw new EntityDeletionForbiddenException("Cannot delete room with existing showtimes");
        }

        roomRepo.delete(room);
    }

    /**
     * Get room by ID
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findRoomById
     */
    public RoomDataResponse getRoom(UUID roomId) {
        Room room = findRoomById(roomId);
        return roomMapper.toDataResponse(room);
    }

    private Snack findSnackById(UUID snackId) {
        return snackRepo.findById(snackId)
                .orElseThrow(() -> new ResourceNotFoundException("Snack", "id", snackId));
    }

    /**
     * Add a new snack to a cinema
     * Predicate nodes (d): 2 -> V(G) = d + 1 = 3
     * Nodes: findCinemaById, existsByCinemaIdAndName
     */
    public SnackDataResponse addSnack(AddSnackRequest request) {
        Cinema cinema = findCinemaById(request.getCinemaId());

        // Check for duplicate snack name in the same cinema
        if (snackRepo.existsByCinemaIdAndName(request.getCinemaId(), request.getName())) {
            throw new DuplicateResourceException(
                    String.format("Snack '%s' already exists in cinema '%s'",
                            request.getName(), cinema.getName()));
        }

        Snack newSnack = snackMapper.toEntity(request);
        newSnack.setCinema(cinema);
        snackRepo.save(newSnack);
        return snackMapper.toDataResponse(newSnack);
    }

    /**
     * Update snack information
     * Predicate nodes (d): 6 -> V(G) = d + 1 = 7
     * Nodes: findSnackById, name!=null, existsByCinemaIdAndNameAndIdNot,
     * description!=null,
     * price!=null, type!=null
     */
    public SnackDataResponse updateSnack(UUID snackId, UpdateSnackRequest request) {
        Snack snack = findSnackById(snackId);

        if (request.getName() != null) {
            // Check for duplicate snack name in the same cinema (if name is being updated)
            if (snackRepo.existsByCinemaIdAndNameAndIdNot(
                    snack.getCinema().getId(), request.getName(), snackId)) {
                throw new DuplicateResourceException(
                        String.format("Snack '%s' already exists in cinema '%s'",
                                request.getName(), snack.getCinema().getName()));
            }
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

    /**
     * Delete a snack
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findSnackById
     */
    public void deleteSnack(UUID id) {
        Snack snack = findSnackById(id);
        snackRepo.delete(snack);
    }

    /**
     * Get snack by ID
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findSnackById
     */
    public SnackDataResponse getSnack(UUID snackId) {
        Snack snack = findSnackById(snackId);
        return snackMapper.toDataResponse(snack);
    }

    // Get all methods

    /**
     * Get all cinemas
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     */
    public List<CinemaDataResponse> getAllCinemas() {
        return cinemaRepo.findAll().stream()
                .map(cinemaMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all rooms
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     */
    public List<RoomDataResponse> getAllRooms() {
        return roomRepo.findAll().stream()
                .map(roomMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all snacks
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     */
    public List<SnackDataResponse> getAllSnacks() {
        return snackRepo.findAll().stream()
                .map(snackMapper::toDataResponse)
                .collect(Collectors.toList());
    }
}
