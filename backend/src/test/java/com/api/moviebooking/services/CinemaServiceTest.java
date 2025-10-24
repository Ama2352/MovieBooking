package com.api.moviebooking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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

@ExtendWith(MockitoExtension.class)
class CinemaServiceTest {

    @Mock
    private CinemaRepo cinemaRepo;

    @Mock
    private CinemaMapper cinemaMapper;

    @Mock
    private RoomRepo roomRepo;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private SnackRepo snackRepo;

    @Mock
    private SnackMapper snackMapper;

    @InjectMocks
    private CinemaService cinemaService;

    @Test
    void addCinema_mapsSavesAndReturnsResponse() {
        AddCinemaRequest req = AddCinemaRequest.builder()
                .name("Cinema A").address("123 Street").hotline("0123456789").build();

        Cinema entity = new Cinema();
        entity.setName("Cinema A");
        entity.setAddress("123 Street");
        entity.setHotline("0123456789");

        CinemaDataResponse expected = new CinemaDataResponse();
        expected.setName("Cinema A");
        expected.setAddress("123 Street");
        expected.setHotline("0123456789");

        when(cinemaMapper.toEntity(req)).thenReturn(entity);
        when(cinemaMapper.toDataResponse(entity)).thenReturn(expected);

        CinemaDataResponse result = cinemaService.addCinema(req);

        verify(cinemaRepo).save(entity);
        // Mapper returns the same object we stubbed
        assertSame(expected, result);
    }

    @Test
    void updateCinema_updatesNonNullFieldsAndSaves() {
        UUID id = UUID.randomUUID();
        Cinema existing = new Cinema();
        existing.setId(id);
        existing.setName("Old Name");
        existing.setAddress("Old Addr");
        existing.setHotline("0000");

        UpdateCinemaRequest req = UpdateCinemaRequest.builder()
                .name("New Name").address(null).hotline("1111").build();

        when(cinemaRepo.findById(id)).thenReturn(Optional.of(existing));

        CinemaDataResponse mapped = new CinemaDataResponse();
        mapped.setName("New Name");
        mapped.setAddress("Old Addr");
        mapped.setHotline("1111");
        when(cinemaMapper.toDataResponse(existing)).thenReturn(mapped);

        CinemaDataResponse result = cinemaService.updateCinema(id, req);

        verify(cinemaRepo).save(existing);
        assertEquals("New Name", existing.getName());
        assertEquals("Old Addr", existing.getAddress());
        assertEquals("1111", existing.getHotline());

        // Mapper returns the same object we stubbed
        assertSame(mapped, result);
    }

    @Test
    void deleteCinema_findsAndDeletes() {
        UUID id = UUID.randomUUID();
        Cinema existing = new Cinema();
        existing.setId(id);
        existing.setRooms(new ArrayList<>());
        existing.setSnacks(new ArrayList<>());
        when(cinemaRepo.findById(id)).thenReturn(Optional.of(existing));

        cinemaService.deleteCinema(id);

        verify(cinemaRepo).delete(existing);
    }

    @Test
    void deleteCinema_throwsWhenHasRooms() {
        UUID id = UUID.randomUUID();
        Cinema existing = new Cinema();
        existing.setId(id);
        Room room = new Room();
        existing.setRooms(List.of(room));
        existing.setSnacks(new ArrayList<>());
        when(cinemaRepo.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(EntityDeletionForbiddenException.class, () -> cinemaService.deleteCinema(id));
    }

    @Test
    void deleteCinema_throwsWhenHasSnacks() {
        UUID id = UUID.randomUUID();
        Cinema existing = new Cinema();
        existing.setId(id);
        existing.setRooms(new ArrayList<>());
        Snack snack = new Snack();
        existing.setSnacks(List.of(snack));
        when(cinemaRepo.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(EntityDeletionForbiddenException.class, () -> cinemaService.deleteCinema(id));
    }

    @Test
    void getCinema_returnsMappedResponse() {
        UUID id = UUID.randomUUID();
        Cinema existing = new Cinema();
        existing.setId(id);
        existing.setName("C");
        existing.setAddress("A");
        existing.setHotline("H");

        when(cinemaRepo.findById(id)).thenReturn(Optional.of(existing));

        CinemaDataResponse mapped = new CinemaDataResponse();
        mapped.setName("C");
        mapped.setAddress("A");
        mapped.setHotline("H");
        when(cinemaMapper.toDataResponse(existing)).thenReturn(mapped);

        CinemaDataResponse result = cinemaService.getCinema(id);
        // Mapper returns the same object we stubbed
        org.junit.jupiter.api.Assertions.assertSame(mapped, result);
    }

    @Test
    void operations_throwWhenCinemaNotFound() {
        UUID id = UUID.randomUUID();
        when(cinemaRepo.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cinemaService.getCinema(id));
        assertThrows(RuntimeException.class,
                () -> cinemaService.updateCinema(id, UpdateCinemaRequest.builder().build()));
        assertThrows(RuntimeException.class, () -> cinemaService.deleteCinema(id));
    }

    // Room CRUD Tests
    @Test
    void addRoom_mapsSavesAndReturnsResponse() {
        UUID cinemaId = UUID.randomUUID();
        Cinema cinema = new Cinema();
        cinema.setId(cinemaId);

        AddRoomRequest req = AddRoomRequest.builder()
                .cinemaId(cinemaId)
                .roomType("IMAX")
                .roomNumber(1)
                .build();

        Room entity = new Room();
        entity.setRoomType("IMAX");
        entity.setRoomNumber(1);

        RoomDataResponse expected = new RoomDataResponse();
        expected.setRoomType("IMAX");
        expected.setRoomNumber(1);

        when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(roomMapper.toEntity(req)).thenReturn(entity);
        when(roomMapper.toDataResponse(entity)).thenReturn(expected);

        RoomDataResponse result = cinemaService.addRoom(req);

        verify(roomRepo).save(entity);
        assertEquals(cinema, entity.getCinema());
        assertSame(expected, result);
    }

    @Test
    void updateRoom_updatesNonNullFieldsAndSaves() {
        UUID roomId = UUID.randomUUID();
        Room existing = new Room();
        existing.setId(roomId);
        existing.setRoomType("IMAX");
        existing.setRoomNumber(1);

        UpdateRoomRequest req = UpdateRoomRequest.builder()
                .roomType("4DX")
                .roomNumber(null)
                .build();

        when(roomRepo.findById(roomId)).thenReturn(Optional.of(existing));

        RoomDataResponse mapped = new RoomDataResponse();
        mapped.setRoomType("4DX");
        mapped.setRoomNumber(1);
        when(roomMapper.toDataResponse(existing)).thenReturn(mapped);

        RoomDataResponse result = cinemaService.updateRoom(roomId, req);

        verify(roomRepo).save(existing);
        assertEquals("4DX", existing.getRoomType());
        assertEquals(1, existing.getRoomNumber());
        assertSame(mapped, result);
    }

    @Test
    void deleteRoom_findsAndDeletes() {
        UUID roomId = UUID.randomUUID();
        Room existing = new Room();
        existing.setId(roomId);
        when(roomRepo.findById(roomId)).thenReturn(Optional.of(existing));

        cinemaService.deleteRoom(roomId);

        verify(roomRepo).delete(existing);
    }

    @Test
    void getRoom_returnsMappedResponse() {
        UUID roomId = UUID.randomUUID();
        Room existing = new Room();
        existing.setId(roomId);
        existing.setRoomType("STARIUM");
        existing.setRoomNumber(3);

        when(roomRepo.findById(roomId)).thenReturn(Optional.of(existing));

        RoomDataResponse mapped = new RoomDataResponse();
        mapped.setRoomType("STARIUM");
        mapped.setRoomNumber(3);
        when(roomMapper.toDataResponse(existing)).thenReturn(mapped);

        RoomDataResponse result = cinemaService.getRoom(roomId);
        assertSame(mapped, result);
    }

    @Test
    void roomOperations_throwWhenRoomNotFound() {
        UUID id = UUID.randomUUID();
        when(roomRepo.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cinemaService.getRoom(id));
        assertThrows(RuntimeException.class,
                () -> cinemaService.updateRoom(id, UpdateRoomRequest.builder().build()));
        assertThrows(RuntimeException.class, () -> cinemaService.deleteRoom(id));
    }

    // Snack CRUD Tests
    @Test
    void addSnack_mapsSavesAndReturnsResponse() {
        UUID cinemaId = UUID.randomUUID();
        Cinema cinema = new Cinema();
        cinema.setId(cinemaId);

        AddSnackRequest req = AddSnackRequest.builder()
                .cinemaId(cinemaId)
                .name("Popcorn")
                .description("Large Popcorn")
                .price(new BigDecimal("5.99"))
                .type("Snack")
                .build();

        Snack entity = new Snack();
        entity.setName("Popcorn");
        entity.setDescription("Large Popcorn");
        entity.setPrice(new BigDecimal("5.99"));
        entity.setType("Snack");

        SnackDataResponse expected = new SnackDataResponse();
        expected.setName("Popcorn");
        expected.setDescription("Large Popcorn");
        expected.setPrice(new BigDecimal("5.99"));
        expected.setType("Snack");

        when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.of(cinema));
        when(snackMapper.toEntity(req)).thenReturn(entity);
        when(snackMapper.toDataResponse(entity)).thenReturn(expected);

        SnackDataResponse result = cinemaService.addSnack(req);

        verify(snackRepo).save(entity);
        assertEquals(cinema, entity.getCinema());
        assertSame(expected, result);
    }

    @Test
    void updateSnack_updatesNonNullFieldsAndSaves() {
        UUID snackId = UUID.randomUUID();
        Snack existing = new Snack();
        existing.setId(snackId);
        existing.setName("Popcorn");
        existing.setDescription("Small Popcorn");
        existing.setPrice(new BigDecimal("3.99"));
        existing.setType("Snack");

        UpdateSnackRequest req = UpdateSnackRequest.builder()
                .name("Combo 1")
                .description(null)
                .price(new BigDecimal("9.99"))
                .type("Combo")
                .build();

        when(snackRepo.findById(snackId)).thenReturn(Optional.of(existing));

        SnackDataResponse mapped = new SnackDataResponse();
        mapped.setName("Combo 1");
        mapped.setDescription("Small Popcorn");
        mapped.setPrice(new BigDecimal("9.99"));
        mapped.setType("Combo");
        when(snackMapper.toDataResponse(existing)).thenReturn(mapped);

        SnackDataResponse result = cinemaService.updateSnack(snackId, req);

        verify(snackRepo).save(existing);
        assertEquals("Combo 1", existing.getName());
        assertEquals("Small Popcorn", existing.getDescription());
        assertEquals(new BigDecimal("9.99"), existing.getPrice());
        assertEquals("Combo", existing.getType());
        assertSame(mapped, result);
    }

    @Test
    void deleteSnack_findsAndDeletes() {
        UUID snackId = UUID.randomUUID();
        Snack existing = new Snack();
        existing.setId(snackId);
        when(snackRepo.findById(snackId)).thenReturn(Optional.of(existing));

        cinemaService.deleteSnack(snackId);

        verify(snackRepo).delete(existing);
    }

    @Test
    void getSnack_returnsMappedResponse() {
        UUID snackId = UUID.randomUUID();
        Snack existing = new Snack();
        existing.setId(snackId);
        existing.setName("Pepsi");
        existing.setDescription("Large Pepsi");
        existing.setPrice(new BigDecimal("4.99"));
        existing.setType("Drink");

        when(snackRepo.findById(snackId)).thenReturn(Optional.of(existing));

        SnackDataResponse mapped = new SnackDataResponse();
        mapped.setName("Pepsi");
        mapped.setDescription("Large Pepsi");
        mapped.setPrice(new BigDecimal("4.99"));
        mapped.setType("Drink");
        when(snackMapper.toDataResponse(existing)).thenReturn(mapped);

        SnackDataResponse result = cinemaService.getSnack(snackId);
        assertSame(mapped, result);
    }

    @Test
    void snackOperations_throwWhenSnackNotFound() {
        UUID id = UUID.randomUUID();
        when(snackRepo.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cinemaService.getSnack(id));
        assertThrows(RuntimeException.class,
                () -> cinemaService.updateSnack(id, UpdateSnackRequest.builder().build()));
        assertThrows(RuntimeException.class, () -> cinemaService.deleteSnack(id));
    }
}
