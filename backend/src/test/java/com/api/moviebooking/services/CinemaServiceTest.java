package com.api.moviebooking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

/**
 * White-Box Testing for CinemaService
 * Tests organized by method with cyclomatic complexity from code comments
 * Total Methods: 18
 * Total Cyclomatic Complexity: 50
 * Total Test Cases: 50
 */
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

    private UUID cinemaId;
    private UUID roomId;
    private UUID snackId;
    private Cinema mockCinema;
    private Room mockRoom;
    private Snack mockSnack;
    private CinemaDataResponse mockCinemaResponse;
    private RoomDataResponse mockRoomResponse;
    private SnackDataResponse mockSnackResponse;

    @BeforeEach
    void setUp() {
        cinemaId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        snackId = UUID.randomUUID();

        // Setup mock Cinema
        mockCinema = new Cinema();
        mockCinema.setId(cinemaId);
        mockCinema.setName("CGV Cinema");
        mockCinema.setAddress("123 Main St");
        mockCinema.setHotline("1900-1234");
        mockCinema.setRooms(new ArrayList<>());
        mockCinema.setSnacks(new ArrayList<>());

        mockCinemaResponse = new CinemaDataResponse();
        mockCinemaResponse.setId(cinemaId.toString());
        mockCinemaResponse.setName("CGV Cinema");
        mockCinemaResponse.setAddress("123 Main St");
        mockCinemaResponse.setHotline("1900-1234");

        // Setup mock Room
        mockRoom = new Room();
        mockRoom.setId(roomId);
        mockRoom.setCinema(mockCinema);
        mockRoom.setRoomType("IMAX");
        mockRoom.setRoomNumber(1);
        mockRoom.setSeats(new ArrayList<>());
        mockRoom.setShowtimes(new ArrayList<>());

        mockRoomResponse = new RoomDataResponse();
        mockRoomResponse.setId(roomId.toString());
        mockRoomResponse.setCinemaId(cinemaId.toString());
        mockRoomResponse.setRoomType("IMAX");
        mockRoomResponse.setRoomNumber(1);

        // Setup mock Snack
        mockSnack = new Snack();
        mockSnack.setId(snackId);
        mockSnack.setCinema(mockCinema);
        mockSnack.setName("Popcorn");
        mockSnack.setDescription("Large popcorn");
        mockSnack.setPrice(new BigDecimal("50000"));
        mockSnack.setType("FOOD");

        mockSnackResponse = new SnackDataResponse();
        mockSnackResponse.setId(snackId.toString());
        mockSnackResponse.setCinemaId(cinemaId.toString());
        mockSnackResponse.setName("Popcorn");
        mockSnackResponse.setDescription("Large popcorn");
        mockSnackResponse.setPrice(new BigDecimal("50000"));
        mockSnackResponse.setType("FOOD");
    }

    // ========================================================================
    // 1. addCinema() Tests
    // Cyclomatic Complexity: V(G) = 2
    // Minimum Test Cases Required: 2
    // Decision Nodes: existsByName
    // ========================================================================

    @Nested
    @DisplayName("addCinema() - V(G)=2, Min Tests=2")
    class AddCinemaTests {

        /**
         * Test Case TC-1: Successfully add a new cinema
         */
        @Test
        @DisplayName("TC-1: Should successfully add cinema with unique name")
        void testAddCinema_Success() {
            AddCinemaRequest request = new AddCinemaRequest();
            request.setName("New Cinema");
            request.setAddress("456 Street");
            request.setHotline("1900-5678");

            when(cinemaRepo.existsByName(request.getName())).thenReturn(false);
            when(cinemaMapper.toEntity(request)).thenReturn(mockCinema);
            when(cinemaRepo.save(mockCinema)).thenReturn(mockCinema);
            when(cinemaMapper.toDataResponse(mockCinema)).thenReturn(mockCinemaResponse);

            CinemaDataResponse result = cinemaService.addCinema(request);

            assertNotNull(result);
            assertEquals(mockCinemaResponse.getName(), result.getName());
            verify(cinemaRepo).existsByName(request.getName());
            verify(cinemaRepo).save(mockCinema);
            verify(cinemaMapper).toDataResponse(mockCinema);
        }

        /**
         * Test Case TC-2: Duplicate cinema name
         */
        @Test
        @DisplayName("TC-2: Should throw DuplicateResourceException when cinema name exists")
        void testAddCinema_DuplicateName() {
            AddCinemaRequest request = new AddCinemaRequest();
            request.setName("Existing Cinema");
            request.setAddress("789 Road");
            request.setHotline("1900-9999");

            when(cinemaRepo.existsByName(request.getName())).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> {
                cinemaService.addCinema(request);
            });

            verify(cinemaRepo).existsByName(request.getName());
            verify(cinemaRepo, never()).save(any());
        }
    }

    // ========================================================================
    // 2. updateCinema() Tests
    // Cyclomatic Complexity: V(G) = 6
    // Minimum Test Cases Required: 6
    // Decision Nodes: findCinemaById, name!=null, existsByNameAndIdNot,
    // address!=null,
    // hotline!=null
    // ========================================================================

    @Nested
    @DisplayName("updateCinema() - V(G)=6, Min Tests=6")
    class UpdateCinemaTests {

        /**
         * Test Case TC-1: Update all fields successfully
         */
        @Test
        @DisplayName("TC-1: Should update all cinema fields successfully")
        void testUpdateCinema_AllFields() {
            UpdateCinemaRequest request = new UpdateCinemaRequest();
            request.setName("Updated Cinema");
            request.setAddress("New Address");
            request.setHotline("1900-0000");

            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.of(mockCinema));
            when(cinemaRepo.existsByNameAndIdNot(request.getName(), cinemaId)).thenReturn(false);
            when(cinemaRepo.save(any(Cinema.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(cinemaMapper.toDataResponse(any(Cinema.class))).thenReturn(mockCinemaResponse);

            CinemaDataResponse result = cinemaService.updateCinema(cinemaId, request);

            assertNotNull(result);
            assertEquals("Updated Cinema", mockCinema.getName());
            assertEquals("New Address", mockCinema.getAddress());
            assertEquals("1900-0000", mockCinema.getHotline());
            verify(cinemaRepo).findById(cinemaId);
            verify(cinemaRepo).existsByNameAndIdNot(request.getName(), cinemaId);
            verify(cinemaRepo).save(mockCinema);
        }

        /**
         * Test Case TC-2: Update only name
         */
        @Test
        @DisplayName("TC-2: Should update only cinema name")
        void testUpdateCinema_NameOnly() {
            UpdateCinemaRequest request = new UpdateCinemaRequest();
            request.setName("New Name Only");

            String originalAddress = mockCinema.getAddress();
            String originalHotline = mockCinema.getHotline();

            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.of(mockCinema));
            when(cinemaRepo.existsByNameAndIdNot(request.getName(), cinemaId)).thenReturn(false);
            when(cinemaRepo.save(any(Cinema.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(cinemaMapper.toDataResponse(any(Cinema.class))).thenReturn(mockCinemaResponse);

            CinemaDataResponse result = cinemaService.updateCinema(cinemaId, request);

            assertNotNull(result);
            assertEquals("New Name Only", mockCinema.getName());
            assertEquals(originalAddress, mockCinema.getAddress());
            assertEquals(originalHotline, mockCinema.getHotline());
            verify(cinemaRepo).existsByNameAndIdNot(request.getName(), cinemaId);
            verify(cinemaRepo).save(mockCinema);
        }

        /**
         * Test Case TC-3: Duplicate name during update
         */
        @Test
        @DisplayName("TC-3: Should throw DuplicateResourceException when updating to existing name")
        void testUpdateCinema_DuplicateName() {
            UpdateCinemaRequest request = new UpdateCinemaRequest();
            request.setName("Existing Cinema Name");

            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.of(mockCinema));
            when(cinemaRepo.existsByNameAndIdNot(request.getName(), cinemaId)).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> {
                cinemaService.updateCinema(cinemaId, request);
            });

            verify(cinemaRepo).existsByNameAndIdNot(request.getName(), cinemaId);
            verify(cinemaRepo, never()).save(any());
        }

        /**
         * Test Case TC-4: Update only address
         */
        @Test
        @DisplayName("TC-4: Should update only cinema address")
        void testUpdateCinema_AddressOnly() {
            UpdateCinemaRequest request = new UpdateCinemaRequest();
            request.setAddress("New Address Only");

            String originalName = mockCinema.getName();
            String originalHotline = mockCinema.getHotline();

            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.of(mockCinema));
            when(cinemaRepo.save(any(Cinema.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(cinemaMapper.toDataResponse(any(Cinema.class))).thenReturn(mockCinemaResponse);

            CinemaDataResponse result = cinemaService.updateCinema(cinemaId, request);

            assertNotNull(result);
            assertEquals("New Address Only", mockCinema.getAddress());
            assertEquals(originalName, mockCinema.getName());
            assertEquals(originalHotline, mockCinema.getHotline());
            verify(cinemaRepo, never()).existsByNameAndIdNot(anyString(), any());
            verify(cinemaRepo).save(mockCinema);
        }

        /**
         * Test Case TC-5: Update only hotline
         */
        @Test
        @DisplayName("TC-5: Should update only cinema hotline")
        void testUpdateCinema_HotlineOnly() {
            UpdateCinemaRequest request = new UpdateCinemaRequest();
            request.setHotline("1900-7777");

            String originalName = mockCinema.getName();
            String originalAddress = mockCinema.getAddress();

            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.of(mockCinema));
            when(cinemaRepo.save(any(Cinema.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(cinemaMapper.toDataResponse(any(Cinema.class))).thenReturn(mockCinemaResponse);

            CinemaDataResponse result = cinemaService.updateCinema(cinemaId, request);

            assertNotNull(result);
            assertEquals("1900-7777", mockCinema.getHotline());
            assertEquals(originalName, mockCinema.getName());
            assertEquals(originalAddress, mockCinema.getAddress());
            verify(cinemaRepo, never()).existsByNameAndIdNot(anyString(), any());
            verify(cinemaRepo).save(mockCinema);
        }

        /**
         * Test Case TC-6: Cinema not found
         */
        @Test
        @DisplayName("TC-6: Should throw ResourceNotFoundException when cinema not found")
        void testUpdateCinema_CinemaNotFound() {
            UpdateCinemaRequest request = new UpdateCinemaRequest();
            request.setName("Test");

            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                cinemaService.updateCinema(cinemaId, request);
            });

            verify(cinemaRepo).findById(cinemaId);
            verify(cinemaRepo, never()).save(any());
        }
    }

    // ========================================================================
    // 3. deleteCinema() Tests
    // Cyclomatic Complexity: V(G) = 4
    // Minimum Test Cases Required: 4
    // Decision Nodes: findCinemaById, !isEmpty(rooms), !isEmpty(snacks)
    // ========================================================================

    @Nested
    @DisplayName("deleteCinema() - V(G)=4, Min Tests=4")
    class DeleteCinemaTests {

        /**
         * Test Case TC-1: Successfully delete cinema with no dependencies
         */
        @Test
        @DisplayName("TC-1: Should successfully delete cinema with no rooms or snacks")
        void testDeleteCinema_Success() {
            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.of(mockCinema));

            cinemaService.deleteCinema(cinemaId);

            verify(cinemaRepo).findById(cinemaId);
            verify(cinemaRepo).delete(mockCinema);
        }

        /**
         * Test Case TC-2: Cannot delete cinema with existing rooms
         */
        @Test
        @DisplayName("TC-2: Should throw EntityDeletionForbiddenException when cinema has rooms")
        void testDeleteCinema_HasRooms() {
            mockCinema.getRooms().add(mockRoom);

            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.of(mockCinema));

            assertThrows(EntityDeletionForbiddenException.class, () -> {
                cinemaService.deleteCinema(cinemaId);
            });

            verify(cinemaRepo).findById(cinemaId);
            verify(cinemaRepo, never()).delete(any());
        }

        /**
         * Test Case TC-3: Cannot delete cinema with existing snacks
         */
        @Test
        @DisplayName("TC-3: Should throw EntityDeletionForbiddenException when cinema has snacks")
        void testDeleteCinema_HasSnacks() {
            mockCinema.getSnacks().add(mockSnack);

            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.of(mockCinema));

            assertThrows(EntityDeletionForbiddenException.class, () -> {
                cinemaService.deleteCinema(cinemaId);
            });

            verify(cinemaRepo).findById(cinemaId);
            verify(cinemaRepo, never()).delete(any());
        }

        /**
         * Test Case TC-4: Cinema not found
         */
        @Test
        @DisplayName("TC-4: Should throw ResourceNotFoundException when cinema not found")
        void testDeleteCinema_CinemaNotFound() {
            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                cinemaService.deleteCinema(cinemaId);
            });

            verify(cinemaRepo).findById(cinemaId);
            verify(cinemaRepo, never()).delete(any());
        }
    }

    // ========================================================================
    // 4. getCinema() Tests
    // Cyclomatic Complexity: V(G) = 2
    // Minimum Test Cases Required: 2
    // Decision Nodes: findCinemaById
    // ========================================================================

    @Nested
    @DisplayName("getCinema() - V(G)=2, Min Tests=2")
    class GetCinemaTests {

        /**
         * Test Case TC-1: Successfully retrieve cinema by ID
         */
        @Test
        @DisplayName("TC-1: Should successfully retrieve cinema by ID")
        void testGetCinema_Success() {
            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.of(mockCinema));
            when(cinemaMapper.toDataResponse(mockCinema)).thenReturn(mockCinemaResponse);

            CinemaDataResponse result = cinemaService.getCinema(cinemaId);

            assertNotNull(result);
            assertEquals(mockCinemaResponse.getName(), result.getName());
            verify(cinemaRepo).findById(cinemaId);
            verify(cinemaMapper).toDataResponse(mockCinema);
        }

        /**
         * Test Case TC-2: Cinema not found
         */
        @Test
        @DisplayName("TC-2: Should throw ResourceNotFoundException when cinema not found")
        void testGetCinema_CinemaNotFound() {
            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                cinemaService.getCinema(cinemaId);
            });

            verify(cinemaRepo).findById(cinemaId);
        }
    }

    // ========================================================================
    // 5. addRoom() Tests
    // Cyclomatic Complexity: V(G) = 3
    // Minimum Test Cases Required: 3
    // Decision Nodes: findCinemaById, existsByCinemaIdAndRoomNumber
    // ========================================================================

    @Nested
    @DisplayName("addRoom() - V(G)=3, Min Tests=3")
    class AddRoomTests {

        /**
         * Test Case TC-1: Successfully add room to cinema
         */
        @Test
        @DisplayName("TC-1: Should successfully add room with unique room number")
        void testAddRoom_Success() {
            AddRoomRequest request = new AddRoomRequest();
            request.setCinemaId(cinemaId);
            request.setRoomType("Standard");
            request.setRoomNumber(2);

            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.of(mockCinema));
            when(roomRepo.existsByCinemaIdAndRoomNumber(cinemaId, 2)).thenReturn(false);
            when(roomMapper.toEntity(request)).thenReturn(mockRoom);
            when(roomRepo.save(mockRoom)).thenReturn(mockRoom);
            when(roomMapper.toDataResponse(mockRoom)).thenReturn(mockRoomResponse);

            RoomDataResponse result = cinemaService.addRoom(request);

            assertNotNull(result);
            verify(cinemaRepo).findById(cinemaId);
            verify(roomRepo).existsByCinemaIdAndRoomNumber(cinemaId, 2);
            verify(roomRepo).save(mockRoom);
        }

        /**
         * Test Case TC-2: Duplicate room number in same cinema
         */
        @Test
        @DisplayName("TC-2: Should throw DuplicateResourceException when room number exists in cinema")
        void testAddRoom_DuplicateRoomNumber() {
            AddRoomRequest request = new AddRoomRequest();
            request.setCinemaId(cinemaId);
            request.setRoomType("IMAX");
            request.setRoomNumber(1);

            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.of(mockCinema));
            when(roomRepo.existsByCinemaIdAndRoomNumber(cinemaId, 1)).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> {
                cinemaService.addRoom(request);
            });

            verify(roomRepo).existsByCinemaIdAndRoomNumber(cinemaId, 1);
            verify(roomRepo, never()).save(any());
        }

        /**
         * Test Case TC-3: Cinema not found
         */
        @Test
        @DisplayName("TC-3: Should throw ResourceNotFoundException when cinema not found")
        void testAddRoom_CinemaNotFound() {
            AddRoomRequest request = new AddRoomRequest();
            request.setCinemaId(cinemaId);
            request.setRoomType("IMAX");
            request.setRoomNumber(1);

            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                cinemaService.addRoom(request);
            });

            verify(cinemaRepo).findById(cinemaId);
            verify(roomRepo, never()).save(any());
        }
    }

    // ========================================================================
    // 6. updateRoom() Tests
    // Cyclomatic Complexity: V(G) = 5
    // Minimum Test Cases Required: 5
    // Decision Nodes: findRoomById, roomType!=null, roomNumber!=null,
    // existsByCinemaIdAndRoomNumberAndIdNot
    // ========================================================================

    @Nested
    @DisplayName("updateRoom() - V(G)=5, Min Tests=5")
    class UpdateRoomTests {

        /**
         * Test Case TC-1: Update both room type and room number
         */
        @Test
        @DisplayName("TC-1: Should update both room type and room number")
        void testUpdateRoom_BothFields() {
            UpdateRoomRequest request = new UpdateRoomRequest();
            request.setRoomType("4DX");
            request.setRoomNumber(3);

            when(roomRepo.findById(roomId)).thenReturn(Optional.of(mockRoom));
            when(roomRepo.existsByCinemaIdAndRoomNumberAndIdNot(cinemaId, 3, roomId)).thenReturn(false);
            when(roomRepo.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(roomMapper.toDataResponse(any(Room.class))).thenReturn(mockRoomResponse);

            RoomDataResponse result = cinemaService.updateRoom(roomId, request);

            assertNotNull(result);
            assertEquals("4DX", mockRoom.getRoomType());
            assertEquals(3, mockRoom.getRoomNumber());
            verify(roomRepo).existsByCinemaIdAndRoomNumberAndIdNot(cinemaId, 3, roomId);
            verify(roomRepo).save(mockRoom);
        }

        /**
         * Test Case TC-2: Update only room type
         */
        @Test
        @DisplayName("TC-2: Should update only room type")
        void testUpdateRoom_RoomTypeOnly() {
            UpdateRoomRequest request = new UpdateRoomRequest();
            request.setRoomType("VIP");

            int originalRoomNumber = mockRoom.getRoomNumber();

            when(roomRepo.findById(roomId)).thenReturn(Optional.of(mockRoom));
            when(roomRepo.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(roomMapper.toDataResponse(any(Room.class))).thenReturn(mockRoomResponse);

            RoomDataResponse result = cinemaService.updateRoom(roomId, request);

            assertNotNull(result);
            assertEquals("VIP", mockRoom.getRoomType());
            assertEquals(originalRoomNumber, mockRoom.getRoomNumber());
            verify(roomRepo, never()).existsByCinemaIdAndRoomNumberAndIdNot(any(), anyInt(), any());
            verify(roomRepo).save(mockRoom);
        }

        /**
         * Test Case TC-3: Update only room number with unique value
         */
        @Test
        @DisplayName("TC-3: Should update only room number when unique")
        void testUpdateRoom_RoomNumberOnly() {
            UpdateRoomRequest request = new UpdateRoomRequest();
            request.setRoomNumber(5);

            String originalRoomType = mockRoom.getRoomType();

            when(roomRepo.findById(roomId)).thenReturn(Optional.of(mockRoom));
            when(roomRepo.existsByCinemaIdAndRoomNumberAndIdNot(cinemaId, 5, roomId)).thenReturn(false);
            when(roomRepo.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(roomMapper.toDataResponse(any(Room.class))).thenReturn(mockRoomResponse);

            RoomDataResponse result = cinemaService.updateRoom(roomId, request);

            assertNotNull(result);
            assertEquals(5, mockRoom.getRoomNumber());
            assertEquals(originalRoomType, mockRoom.getRoomType());
            verify(roomRepo).existsByCinemaIdAndRoomNumberAndIdNot(cinemaId, 5, roomId);
            verify(roomRepo).save(mockRoom);
        }

        /**
         * Test Case TC-4: Duplicate room number during update
         */
        @Test
        @DisplayName("TC-4: Should throw DuplicateResourceException when updating to existing room number")
        void testUpdateRoom_DuplicateRoomNumber() {
            UpdateRoomRequest request = new UpdateRoomRequest();
            request.setRoomNumber(1);

            when(roomRepo.findById(roomId)).thenReturn(Optional.of(mockRoom));
            when(roomRepo.existsByCinemaIdAndRoomNumberAndIdNot(cinemaId, 1, roomId)).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> {
                cinemaService.updateRoom(roomId, request);
            });

            verify(roomRepo).existsByCinemaIdAndRoomNumberAndIdNot(cinemaId, 1, roomId);
            verify(roomRepo, never()).save(any());
        }

        /**
         * Test Case TC-5: Room not found
         */
        @Test
        @DisplayName("TC-5: Should throw ResourceNotFoundException when room not found")
        void testUpdateRoom_RoomNotFound() {
            UpdateRoomRequest request = new UpdateRoomRequest();
            request.setRoomType("Test");

            when(roomRepo.findById(roomId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                cinemaService.updateRoom(roomId, request);
            });

            verify(roomRepo).findById(roomId);
            verify(roomRepo, never()).save(any());
        }
    }

    // ========================================================================
    // 7. deleteRoom() Tests
    // Cyclomatic Complexity: V(G) = 4
    // Minimum Test Cases Required: 4
    // Decision Nodes: findRoomById, !isEmpty(seats), !isEmpty(showtimes)
    // ========================================================================

    @Nested
    @DisplayName("deleteRoom() - V(G)=4, Min Tests=4")
    class DeleteRoomTests {

        /**
         * Test Case TC-1: Successfully delete room with no dependencies
         */
        @Test
        @DisplayName("TC-1: Should successfully delete room with no seats or showtimes")
        void testDeleteRoom_Success() {
            when(roomRepo.findById(roomId)).thenReturn(Optional.of(mockRoom));

            cinemaService.deleteRoom(roomId);

            verify(roomRepo).findById(roomId);
            verify(roomRepo).delete(mockRoom);
        }

        /**
         * Test Case TC-2: Cannot delete room with existing seats
         */
        @Test
        @DisplayName("TC-2: Should throw EntityDeletionForbiddenException when room has seats")
        void testDeleteRoom_HasSeats() {
            mockRoom.getSeats().add(new com.api.moviebooking.models.entities.Seat());

            when(roomRepo.findById(roomId)).thenReturn(Optional.of(mockRoom));

            assertThrows(EntityDeletionForbiddenException.class, () -> {
                cinemaService.deleteRoom(roomId);
            });

            verify(roomRepo).findById(roomId);
            verify(roomRepo, never()).delete(any());
        }

        /**
         * Test Case TC-3: Cannot delete room with existing showtimes
         */
        @Test
        @DisplayName("TC-3: Should throw EntityDeletionForbiddenException when room has showtimes")
        void testDeleteRoom_HasShowtimes() {
            mockRoom.getShowtimes().add(new com.api.moviebooking.models.entities.Showtime());

            when(roomRepo.findById(roomId)).thenReturn(Optional.of(mockRoom));

            assertThrows(EntityDeletionForbiddenException.class, () -> {
                cinemaService.deleteRoom(roomId);
            });

            verify(roomRepo).findById(roomId);
            verify(roomRepo, never()).delete(any());
        }

        /**
         * Test Case TC-4: Room not found
         */
        @Test
        @DisplayName("TC-4: Should throw ResourceNotFoundException when room not found")
        void testDeleteRoom_RoomNotFound() {
            when(roomRepo.findById(roomId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                cinemaService.deleteRoom(roomId);
            });

            verify(roomRepo).findById(roomId);
            verify(roomRepo, never()).delete(any());
        }
    }

    // ========================================================================
    // 8. getRoom() Tests
    // Cyclomatic Complexity: V(G) = 2
    // Minimum Test Cases Required: 2
    // Decision Nodes: findRoomById
    // ========================================================================

    @Nested
    @DisplayName("getRoom() - V(G)=2, Min Tests=2")
    class GetRoomTests {

        /**
         * Test Case TC-1: Successfully retrieve room by ID
         */
        @Test
        @DisplayName("TC-1: Should successfully retrieve room by ID")
        void testGetRoom_Success() {
            when(roomRepo.findById(roomId)).thenReturn(Optional.of(mockRoom));
            when(roomMapper.toDataResponse(mockRoom)).thenReturn(mockRoomResponse);

            RoomDataResponse result = cinemaService.getRoom(roomId);

            assertNotNull(result);
            assertEquals(mockRoomResponse.getRoomType(), result.getRoomType());
            verify(roomRepo).findById(roomId);
            verify(roomMapper).toDataResponse(mockRoom);
        }

        /**
         * Test Case TC-2: Room not found
         */
        @Test
        @DisplayName("TC-2: Should throw ResourceNotFoundException when room not found")
        void testGetRoom_RoomNotFound() {
            when(roomRepo.findById(roomId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                cinemaService.getRoom(roomId);
            });

            verify(roomRepo).findById(roomId);
        }
    }

    // ========================================================================
    // 9. addSnack() Tests
    // Cyclomatic Complexity: V(G) = 3
    // Minimum Test Cases Required: 3
    // Decision Nodes: findCinemaById, existsByCinemaIdAndName
    // ========================================================================

    @Nested
    @DisplayName("addSnack() - V(G)=3, Min Tests=3")
    class AddSnackTests {

        /**
         * Test Case TC-1: Successfully add snack to cinema
         */
        @Test
        @DisplayName("TC-1: Should successfully add snack with unique name")
        void testAddSnack_Success() {
            AddSnackRequest request = new AddSnackRequest();
            request.setCinemaId(cinemaId);
            request.setName("Nachos");
            request.setDescription("Cheese nachos");
            request.setPrice(new BigDecimal("45000"));
            request.setType("FOOD");

            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.of(mockCinema));
            when(snackRepo.existsByCinemaIdAndName(cinemaId, "Nachos")).thenReturn(false);
            when(snackMapper.toEntity(request)).thenReturn(mockSnack);
            when(snackRepo.save(mockSnack)).thenReturn(mockSnack);
            when(snackMapper.toDataResponse(mockSnack)).thenReturn(mockSnackResponse);

            SnackDataResponse result = cinemaService.addSnack(request);

            assertNotNull(result);
            verify(cinemaRepo).findById(cinemaId);
            verify(snackRepo).existsByCinemaIdAndName(cinemaId, "Nachos");
            verify(snackRepo).save(mockSnack);
        }

        /**
         * Test Case TC-2: Duplicate snack name in same cinema
         */
        @Test
        @DisplayName("TC-2: Should throw DuplicateResourceException when snack name exists in cinema")
        void testAddSnack_DuplicateName() {
            AddSnackRequest request = new AddSnackRequest();
            request.setCinemaId(cinemaId);
            request.setName("Popcorn");
            request.setDescription("Large popcorn");
            request.setPrice(new BigDecimal("50000"));
            request.setType("FOOD");

            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.of(mockCinema));
            when(snackRepo.existsByCinemaIdAndName(cinemaId, "Popcorn")).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> {
                cinemaService.addSnack(request);
            });

            verify(snackRepo).existsByCinemaIdAndName(cinemaId, "Popcorn");
            verify(snackRepo, never()).save(any());
        }

        /**
         * Test Case TC-3: Cinema not found
         */
        @Test
        @DisplayName("TC-3: Should throw ResourceNotFoundException when cinema not found")
        void testAddSnack_CinemaNotFound() {
            AddSnackRequest request = new AddSnackRequest();
            request.setCinemaId(cinemaId);
            request.setName("Popcorn");
            request.setDescription("Large popcorn");
            request.setPrice(new BigDecimal("50000"));
            request.setType("FOOD");

            when(cinemaRepo.findById(cinemaId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                cinemaService.addSnack(request);
            });

            verify(cinemaRepo).findById(cinemaId);
            verify(snackRepo, never()).save(any());
        }
    }

    // ========================================================================
    // 10. updateSnack() Tests
    // Cyclomatic Complexity: V(G) = 7
    // Minimum Test Cases Required: 7
    // Decision Nodes: findSnackById, name!=null, existsByCinemaIdAndNameAndIdNot,
    // description!=null, price!=null, type!=null
    // ========================================================================

    @Nested
    @DisplayName("updateSnack() - V(G)=7, Min Tests=7")
    class UpdateSnackTests {

        /**
         * Test Case TC-1: Update all fields successfully
         */
        @Test
        @DisplayName("TC-1: Should update all snack fields successfully")
        void testUpdateSnack_AllFields() {
            UpdateSnackRequest request = new UpdateSnackRequest();
            request.setName("Updated Snack");
            request.setDescription("New description");
            request.setPrice(new BigDecimal("60000"));
            request.setType("DRINK");

            when(snackRepo.findById(snackId)).thenReturn(Optional.of(mockSnack));
            when(snackRepo.existsByCinemaIdAndNameAndIdNot(cinemaId, "Updated Snack", snackId)).thenReturn(false);
            when(snackRepo.save(any(Snack.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(snackMapper.toDataResponse(any(Snack.class))).thenReturn(mockSnackResponse);

            SnackDataResponse result = cinemaService.updateSnack(snackId, request);

            assertNotNull(result);
            assertEquals("Updated Snack", mockSnack.getName());
            assertEquals("New description", mockSnack.getDescription());
            assertEquals(new BigDecimal("60000"), mockSnack.getPrice());
            assertEquals("DRINK", mockSnack.getType());
            verify(snackRepo).existsByCinemaIdAndNameAndIdNot(cinemaId, "Updated Snack", snackId);
            verify(snackRepo).save(mockSnack);
        }

        /**
         * Test Case TC-2: Update only name
         */
        @Test
        @DisplayName("TC-2: Should update only snack name")
        void testUpdateSnack_NameOnly() {
            UpdateSnackRequest request = new UpdateSnackRequest();
            request.setName("New Snack Name");

            String originalDescription = mockSnack.getDescription();
            BigDecimal originalPrice = mockSnack.getPrice();
            String originalType = mockSnack.getType();

            when(snackRepo.findById(snackId)).thenReturn(Optional.of(mockSnack));
            when(snackRepo.existsByCinemaIdAndNameAndIdNot(cinemaId, "New Snack Name", snackId)).thenReturn(false);
            when(snackRepo.save(any(Snack.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(snackMapper.toDataResponse(any(Snack.class))).thenReturn(mockSnackResponse);

            SnackDataResponse result = cinemaService.updateSnack(snackId, request);

            assertNotNull(result);
            assertEquals("New Snack Name", mockSnack.getName());
            assertEquals(originalDescription, mockSnack.getDescription());
            assertEquals(originalPrice, mockSnack.getPrice());
            assertEquals(originalType, mockSnack.getType());
            verify(snackRepo).existsByCinemaIdAndNameAndIdNot(cinemaId, "New Snack Name", snackId);
            verify(snackRepo).save(mockSnack);
        }

        /**
         * Test Case TC-3: Duplicate name during update
         */
        @Test
        @DisplayName("TC-3: Should throw DuplicateResourceException when updating to existing name")
        void testUpdateSnack_DuplicateName() {
            UpdateSnackRequest request = new UpdateSnackRequest();
            request.setName("Existing Snack");

            when(snackRepo.findById(snackId)).thenReturn(Optional.of(mockSnack));
            when(snackRepo.existsByCinemaIdAndNameAndIdNot(cinemaId, "Existing Snack", snackId)).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> {
                cinemaService.updateSnack(snackId, request);
            });

            verify(snackRepo).existsByCinemaIdAndNameAndIdNot(cinemaId, "Existing Snack", snackId);
            verify(snackRepo, never()).save(any());
        }

        /**
         * Test Case TC-4: Update only description
         */
        @Test
        @DisplayName("TC-4: Should update only snack description")
        void testUpdateSnack_DescriptionOnly() {
            UpdateSnackRequest request = new UpdateSnackRequest();
            request.setDescription("New description only");

            String originalName = mockSnack.getName();
            BigDecimal originalPrice = mockSnack.getPrice();
            String originalType = mockSnack.getType();

            when(snackRepo.findById(snackId)).thenReturn(Optional.of(mockSnack));
            when(snackRepo.save(any(Snack.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(snackMapper.toDataResponse(any(Snack.class))).thenReturn(mockSnackResponse);

            SnackDataResponse result = cinemaService.updateSnack(snackId, request);

            assertNotNull(result);
            assertEquals("New description only", mockSnack.getDescription());
            assertEquals(originalName, mockSnack.getName());
            assertEquals(originalPrice, mockSnack.getPrice());
            assertEquals(originalType, mockSnack.getType());
            verify(snackRepo, never()).existsByCinemaIdAndNameAndIdNot(any(), anyString(), any());
            verify(snackRepo).save(mockSnack);
        }

        /**
         * Test Case TC-5: Update only price
         */
        @Test
        @DisplayName("TC-5: Should update only snack price")
        void testUpdateSnack_PriceOnly() {
            UpdateSnackRequest request = new UpdateSnackRequest();
            request.setPrice(new BigDecimal("55000"));

            String originalName = mockSnack.getName();
            String originalDescription = mockSnack.getDescription();
            String originalType = mockSnack.getType();

            when(snackRepo.findById(snackId)).thenReturn(Optional.of(mockSnack));
            when(snackRepo.save(any(Snack.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(snackMapper.toDataResponse(any(Snack.class))).thenReturn(mockSnackResponse);

            SnackDataResponse result = cinemaService.updateSnack(snackId, request);

            assertNotNull(result);
            assertEquals(new BigDecimal("55000"), mockSnack.getPrice());
            assertEquals(originalName, mockSnack.getName());
            assertEquals(originalDescription, mockSnack.getDescription());
            assertEquals(originalType, mockSnack.getType());
            verify(snackRepo, never()).existsByCinemaIdAndNameAndIdNot(any(), anyString(), any());
            verify(snackRepo).save(mockSnack);
        }

        /**
         * Test Case TC-6: Update only type
         */
        @Test
        @DisplayName("TC-6: Should update only snack type")
        void testUpdateSnack_TypeOnly() {
            UpdateSnackRequest request = new UpdateSnackRequest();
            request.setType("COMBO");

            String originalName = mockSnack.getName();
            String originalDescription = mockSnack.getDescription();
            BigDecimal originalPrice = mockSnack.getPrice();

            when(snackRepo.findById(snackId)).thenReturn(Optional.of(mockSnack));
            when(snackRepo.save(any(Snack.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(snackMapper.toDataResponse(any(Snack.class))).thenReturn(mockSnackResponse);

            SnackDataResponse result = cinemaService.updateSnack(snackId, request);

            assertNotNull(result);
            assertEquals("COMBO", mockSnack.getType());
            assertEquals(originalName, mockSnack.getName());
            assertEquals(originalDescription, mockSnack.getDescription());
            assertEquals(originalPrice, mockSnack.getPrice());
            verify(snackRepo, never()).existsByCinemaIdAndNameAndIdNot(any(), anyString(), any());
            verify(snackRepo).save(mockSnack);
        }

        /**
         * Test Case TC-7: Snack not found
         */
        @Test
        @DisplayName("TC-7: Should throw ResourceNotFoundException when snack not found")
        void testUpdateSnack_SnackNotFound() {
            UpdateSnackRequest request = new UpdateSnackRequest();
            request.setName("Test");

            when(snackRepo.findById(snackId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                cinemaService.updateSnack(snackId, request);
            });

            verify(snackRepo).findById(snackId);
            verify(snackRepo, never()).save(any());
        }
    }

    // ========================================================================
    // 11. deleteSnack() Tests
    // Cyclomatic Complexity: V(G) = 2
    // Minimum Test Cases Required: 2
    // Decision Nodes: findSnackById
    // ========================================================================

    @Nested
    @DisplayName("deleteSnack() - V(G)=2, Min Tests=2")
    class DeleteSnackTests {

        /**
         * Test Case TC-1: Successfully delete snack
         */
        @Test
        @DisplayName("TC-1: Should successfully delete snack")
        void testDeleteSnack_Success() {
            when(snackRepo.findById(snackId)).thenReturn(Optional.of(mockSnack));

            cinemaService.deleteSnack(snackId);

            verify(snackRepo).findById(snackId);
            verify(snackRepo).delete(mockSnack);
        }

        /**
         * Test Case TC-2: Snack not found
         */
        @Test
        @DisplayName("TC-2: Should throw ResourceNotFoundException when snack not found")
        void testDeleteSnack_SnackNotFound() {
            when(snackRepo.findById(snackId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                cinemaService.deleteSnack(snackId);
            });

            verify(snackRepo).findById(snackId);
            verify(snackRepo, never()).delete(any());
        }
    }

    // ========================================================================
    // 12. getSnack() Tests
    // Cyclomatic Complexity: V(G) = 2
    // Minimum Test Cases Required: 2
    // Decision Nodes: findSnackById
    // ========================================================================

    @Nested
    @DisplayName("getSnack() - V(G)=2, Min Tests=2")
    class GetSnackTests {

        /**
         * Test Case TC-1: Successfully retrieve snack by ID
         */
        @Test
        @DisplayName("TC-1: Should successfully retrieve snack by ID")
        void testGetSnack_Success() {
            when(snackRepo.findById(snackId)).thenReturn(Optional.of(mockSnack));
            when(snackMapper.toDataResponse(mockSnack)).thenReturn(mockSnackResponse);

            SnackDataResponse result = cinemaService.getSnack(snackId);

            assertNotNull(result);
            assertEquals(mockSnackResponse.getName(), result.getName());
            verify(snackRepo).findById(snackId);
            verify(snackMapper).toDataResponse(mockSnack);
        }

        /**
         * Test Case TC-2: Snack not found
         */
        @Test
        @DisplayName("TC-2: Should throw ResourceNotFoundException when snack not found")
        void testGetSnack_SnackNotFound() {
            when(snackRepo.findById(snackId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                cinemaService.getSnack(snackId);
            });

            verify(snackRepo).findById(snackId);
        }
    }

    // ========================================================================
    // 13. getAllCinemas() Tests
    // Cyclomatic Complexity: V(G) = 1
    // Minimum Test Cases Required: 1
    // Decision Nodes: None (straight-line code)
    // ========================================================================

    @Nested
    @DisplayName("getAllCinemas() - V(G)=1, Min Tests=1")
    class GetAllCinemasTests {

        /**
         * Test Case TC-1: Successfully retrieve all cinemas
         */
        @Test
        @DisplayName("TC-1: Should successfully retrieve all cinemas")
        void testGetAllCinemas_Success() {
            Cinema cinema1 = new Cinema();
            cinema1.setId(UUID.randomUUID());
            cinema1.setName("Cinema 1");

            Cinema cinema2 = new Cinema();
            cinema2.setId(UUID.randomUUID());
            cinema2.setName("Cinema 2");

            CinemaDataResponse response1 = new CinemaDataResponse();
            response1.setName("Cinema 1");

            CinemaDataResponse response2 = new CinemaDataResponse();
            response2.setName("Cinema 2");

            when(cinemaRepo.findAll()).thenReturn(Arrays.asList(cinema1, cinema2));
            when(cinemaMapper.toDataResponse(cinema1)).thenReturn(response1);
            when(cinemaMapper.toDataResponse(cinema2)).thenReturn(response2);

            List<CinemaDataResponse> result = cinemaService.getAllCinemas();

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(cinemaRepo).findAll();
            verify(cinemaMapper, times(2)).toDataResponse(any(Cinema.class));
        }
    }

    // ========================================================================
    // 14. getAllRooms() Tests
    // Cyclomatic Complexity: V(G) = 1
    // Minimum Test Cases Required: 1
    // Decision Nodes: None (straight-line code)
    // ========================================================================

    @Nested
    @DisplayName("getAllRooms() - V(G)=1, Min Tests=1")
    class GetAllRoomsTests {

        /**
         * Test Case TC-1: Successfully retrieve all rooms
         */
        @Test
        @DisplayName("TC-1: Should successfully retrieve all rooms")
        void testGetAllRooms_Success() {
            Room room1 = new Room();
            room1.setId(UUID.randomUUID());
            room1.setRoomType("IMAX");

            Room room2 = new Room();
            room2.setId(UUID.randomUUID());
            room2.setRoomType("Standard");

            RoomDataResponse response1 = new RoomDataResponse();
            response1.setRoomType("IMAX");

            RoomDataResponse response2 = new RoomDataResponse();
            response2.setRoomType("Standard");

            when(roomRepo.findAll()).thenReturn(Arrays.asList(room1, room2));
            when(roomMapper.toDataResponse(room1)).thenReturn(response1);
            when(roomMapper.toDataResponse(room2)).thenReturn(response2);

            List<RoomDataResponse> result = cinemaService.getAllRooms();

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(roomRepo).findAll();
            verify(roomMapper, times(2)).toDataResponse(any(Room.class));
        }
    }

    // ========================================================================
    // 15. getAllSnacks() Tests
    // Cyclomatic Complexity: V(G) = 1
    // Minimum Test Cases Required: 1
    // Decision Nodes: None (straight-line code)
    // ========================================================================

    @Nested
    @DisplayName("getAllSnacks() - V(G)=1, Min Tests=1")
    class GetAllSnacksTests {

        /**
         * Test Case TC-1: Successfully retrieve all snacks
         */
        @Test
        @DisplayName("TC-1: Should successfully retrieve all snacks")
        void testGetAllSnacks_Success() {
            Snack snack1 = new Snack();
            snack1.setId(UUID.randomUUID());
            snack1.setName("Popcorn");

            Snack snack2 = new Snack();
            snack2.setId(UUID.randomUUID());
            snack2.setName("Nachos");

            SnackDataResponse response1 = new SnackDataResponse();
            response1.setName("Popcorn");

            SnackDataResponse response2 = new SnackDataResponse();
            response2.setName("Nachos");

            when(snackRepo.findAll()).thenReturn(Arrays.asList(snack1, snack2));
            when(snackMapper.toDataResponse(snack1)).thenReturn(response1);
            when(snackMapper.toDataResponse(snack2)).thenReturn(response2);

            List<SnackDataResponse> result = cinemaService.getAllSnacks();

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(snackRepo).findAll();
            verify(snackMapper, times(2)).toDataResponse(any(Snack.class));
        }
    }
}
