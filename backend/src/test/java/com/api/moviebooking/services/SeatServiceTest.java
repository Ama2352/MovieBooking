package com.api.moviebooking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.mapstructs.SeatMapper;
import com.api.moviebooking.models.dtos.seat.*;
import com.api.moviebooking.models.entities.Room;
import com.api.moviebooking.models.entities.Seat;
import com.api.moviebooking.models.enums.SeatType;
import com.api.moviebooking.repositories.RoomRepo;
import com.api.moviebooking.repositories.SeatRepo;

/**
 * Unit tests for SeatService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SeatService Unit Tests")
class SeatServiceTest {

    @Mock
    private SeatRepo seatRepo;

    @Mock
    private RoomRepo roomRepo;

    @Mock
    private SeatMapper seatMapper;

    @InjectMocks
    private SeatService seatService;

    private UUID roomId;
    private UUID seatId;
    private Room room;
    private Seat seat;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        seatId = UUID.randomUUID();

        room = new Room();
        room.setId(roomId);
        room.setSeats(new ArrayList<>());

        seat = new Seat();
        seat.setId(seatId);
        seat.setRoom(room);
        seat.setRowLabel("A");
        seat.setSeatNumber(1);
        seat.setSeatType(SeatType.NORMAL);
        seat.setShowtimeSeats(new ArrayList<>());
    }

    // ========================================================================
    // Add Seat Tests
    // ========================================================================

    @Nested
    @DisplayName("Add Seat")
    class AddSeatTests {

        @Test
        @DisplayName("Should add seat successfully")
        void testAddSeat_Success() {
            AddSeatRequest request = new AddSeatRequest();
            request.setRoomId(roomId);
            request.setRowLabel("A");
            request.setSeatNumber(1);
            request.setSeatType("NORMAL");

            when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));
            when(seatMapper.toEntity(request)).thenReturn(seat);
            when(seatMapper.toDataResponse(seat)).thenReturn(new SeatDataResponse());

            SeatDataResponse result = seatService.addSeat(request);

            assertNotNull(result);
            verify(seatRepo).save(seat);
        }

        @Test
        @DisplayName("Should throw exception for non-existent room")
        void testAddSeat_RoomNotFound() {
            AddSeatRequest request = new AddSeatRequest();
            request.setRoomId(roomId);

            when(roomRepo.findById(roomId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                seatService.addSeat(request);
            });

            verify(seatRepo, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception for duplicate seat")
        void testAddSeat_Duplicate() {
            AddSeatRequest request = new AddSeatRequest();
            request.setRoomId(roomId);
            request.setRowLabel("A");
            request.setSeatNumber(1);

            Seat existingSeat = new Seat();
            existingSeat.setRowLabel("A");
            existingSeat.setSeatNumber(1);
            room.getSeats().add(existingSeat);

            when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));

            assertThrows(IllegalArgumentException.class, () -> {
                seatService.addSeat(request);
            });

            verify(seatRepo, never()).save(any());
        }

        @Test
        @DisplayName("Should allow case-insensitive row label check")
        void testAddSeat_CaseInsensitiveDuplicate() {
            AddSeatRequest request = new AddSeatRequest();
            request.setRoomId(roomId);
            request.setRowLabel("a"); // lowercase
            request.setSeatNumber(1);

            Seat existingSeat = new Seat();
            existingSeat.setRowLabel("A"); // uppercase
            existingSeat.setSeatNumber(1);
            room.getSeats().add(existingSeat);

            when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));

            assertThrows(IllegalArgumentException.class, () -> {
                seatService.addSeat(request);
            });
        }
    }

    // ========================================================================
    // Update Seat Tests
    // ========================================================================

    @Nested
    @DisplayName("Update Seat")
    class UpdateSeatTests {

        @Test
        @DisplayName("Should update seat number")
        void testUpdateSeat_SeatNumber() {
            UpdateSeatRequest request = new UpdateSeatRequest();
            request.setSeatNumber(5);

            when(seatRepo.findById(seatId)).thenReturn(Optional.of(seat));
            when(seatMapper.toDataResponse(seat)).thenReturn(new SeatDataResponse());

            SeatDataResponse result = seatService.updateSeat(seatId, request);

            assertNotNull(result);
            assertEquals(5, seat.getSeatNumber());
            verify(seatRepo).save(seat);
        }

        @Test
        @DisplayName("Should update row label")
        void testUpdateSeat_RowLabel() {
            UpdateSeatRequest request = new UpdateSeatRequest();
            request.setRowLabel("B");

            when(seatRepo.findById(seatId)).thenReturn(Optional.of(seat));
            when(seatMapper.toDataResponse(seat)).thenReturn(new SeatDataResponse());

            seatService.updateSeat(seatId, request);

            assertEquals("B", seat.getRowLabel());
        }

        @Test
        @DisplayName("Should update seat type")
        void testUpdateSeat_SeatType() {
            UpdateSeatRequest request = new UpdateSeatRequest();
            request.setSeatType("VIP");

            when(seatRepo.findById(seatId)).thenReturn(Optional.of(seat));
            when(seatMapper.toDataResponse(seat)).thenReturn(new SeatDataResponse());

            seatService.updateSeat(seatId, request);

            assertEquals(SeatType.VIP, seat.getSeatType());
        }

        @Test
        @DisplayName("Should throw exception for non-existent seat")
        void testUpdateSeat_NotFound() {
            UpdateSeatRequest request = new UpdateSeatRequest();

            when(seatRepo.findById(seatId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                seatService.updateSeat(seatId, request);
            });
        }
    }

    // ========================================================================
    // Delete Seat Tests
    // ========================================================================

    @Nested
    @DisplayName("Delete Seat")
    class DeleteSeatTests {

        @Test
        @DisplayName("Should delete seat with no showtimes")
        void testDeleteSeat_Success() {
            when(seatRepo.findById(seatId)).thenReturn(Optional.of(seat));

            assertDoesNotThrow(() -> {
                seatService.deleteSeat(seatId);
            });

            verify(seatRepo).delete(seat);
        }

        @Test
        @DisplayName("Should throw exception when seat has showtimes")
        void testDeleteSeat_HasShowtimes() {
            seat.getShowtimeSeats().add(new com.api.moviebooking.models.entities.ShowtimeSeat());

            when(seatRepo.findById(seatId)).thenReturn(Optional.of(seat));

            assertThrows(IllegalStateException.class, () -> {
                seatService.deleteSeat(seatId);
            });

            verify(seatRepo, never()).delete(seat);
        }
    }

    // ========================================================================
    // Bulk Generation Tests
    // ========================================================================

    @Nested
    @DisplayName("Bulk Seat Generation")
    class BulkGenerationTests {

        @Test
        @DisplayName("Should generate seats successfully")
        void testGenerateSeats_Success() {
            GenerateSeatsRequest request = new GenerateSeatsRequest();
            request.setRoomId(roomId);
            request.setRows(10);
            request.setSeatsPerRow(15);

            when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));
            when(seatRepo.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
            when(seatMapper.toDataResponse(any())).thenReturn(new SeatDataResponse());

            BulkSeatResponse result = seatService.generateSeats(request);

            assertNotNull(result);
            assertEquals(150, result.getTotalSeatsGenerated());
            assertEquals(150, result.getNormalSeats());
            assertEquals(0, result.getVipSeats());
            assertEquals(0, result.getCoupleSeats());
            verify(seatRepo).saveAll(anyList());
        }

        @Test
        @DisplayName("Should generate seats with VIP rows")
        void testGenerateSeats_WithVipRows() {
            GenerateSeatsRequest request = new GenerateSeatsRequest();
            request.setRoomId(roomId);
            request.setRows(10);
            request.setSeatsPerRow(10);
            request.setVipRows(Arrays.asList("A", "B"));

            when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));
            when(seatRepo.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
            when(seatMapper.toDataResponse(any())).thenReturn(new SeatDataResponse());

            BulkSeatResponse result = seatService.generateSeats(request);

            assertEquals(100, result.getTotalSeatsGenerated());
            assertEquals(20, result.getVipSeats()); // 2 rows * 10 seats
            assertEquals(80, result.getNormalSeats()); // 8 rows * 10 seats
        }

        @Test
        @DisplayName("Should generate seats with couple rows")
        void testGenerateSeats_WithCoupleRows() {
            GenerateSeatsRequest request = new GenerateSeatsRequest();
            request.setRoomId(roomId);
            request.setRows(5);
            request.setSeatsPerRow(10);
            request.setCoupleRows(Arrays.asList("E"));

            when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));
            when(seatRepo.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
            when(seatMapper.toDataResponse(any())).thenReturn(new SeatDataResponse());

            BulkSeatResponse result = seatService.generateSeats(request);

            assertEquals(50, result.getTotalSeatsGenerated());
            assertEquals(10, result.getCoupleSeats());
            assertEquals(40, result.getNormalSeats());
        }

        @Test
        @DisplayName("Should throw exception for room with existing seats")
        void testGenerateSeats_RoomHasSeats() {
            room.getSeats().add(seat);

            GenerateSeatsRequest request = new GenerateSeatsRequest();
            request.setRoomId(roomId);
            request.setRows(10);
            request.setSeatsPerRow(10);

            when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));

            assertThrows(IllegalStateException.class, () -> {
                seatService.generateSeats(request);
            });

            verify(seatRepo, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw exception for invalid VIP row label")
        void testGenerateSeats_InvalidVipRow() {
            GenerateSeatsRequest request = new GenerateSeatsRequest();
            request.setRoomId(roomId);
            request.setRows(5);
            request.setSeatsPerRow(10);
            request.setVipRows(Arrays.asList("Z")); // Out of range

            when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));

            assertThrows(IllegalArgumentException.class, () -> {
                seatService.generateSeats(request);
            });
        }
    }

    // ========================================================================
    // Row Labels Tests
    // ========================================================================

    @Nested
    @DisplayName("Row Label Generation")
    class RowLabelsTests {

        @Test
        @DisplayName("Should generate row labels preview")
        void testGetRowLabelsPreview() {
            RowLabelsResponse result = seatService.getRowLabelsPreview(10);

            assertNotNull(result);
            assertEquals(10, result.getTotalRows());
            assertEquals(10, result.getRowLabels().size());
            assertEquals("A", result.getRowLabels().get(0));
            assertEquals("J", result.getRowLabels().get(9));
        }

        @Test
        @DisplayName("Should generate extended row labels (AA, AB, etc.)")
        void testGetRowLabelsPreview_Extended() {
            RowLabelsResponse result = seatService.getRowLabelsPreview(30);

            assertEquals(30, result.getTotalRows());
            assertEquals("A", result.getRowLabels().get(0));
            assertEquals("Z", result.getRowLabels().get(25));
            assertEquals("AA", result.getRowLabels().get(26));
            assertEquals("AB", result.getRowLabels().get(27));
            assertEquals("AD", result.getRowLabels().get(29));
        }

        @Test
        @DisplayName("Should throw exception for invalid row count")
        void testGetRowLabelsPreview_InvalidCount() {
            assertThrows(IllegalArgumentException.class, () -> {
                seatService.getRowLabelsPreview(0);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                seatService.getRowLabelsPreview(101);
            });
        }
    }

    // ========================================================================
    // Query Tests
    // ========================================================================

    @Nested
    @DisplayName("Query Seats")
    class QueryTests {

        @Test
        @DisplayName("Should get seat by ID")
        void testGetSeat() {
            when(seatRepo.findById(seatId)).thenReturn(Optional.of(seat));
            when(seatMapper.toDataResponse(seat)).thenReturn(new SeatDataResponse());

            SeatDataResponse result = seatService.getSeat(seatId);

            assertNotNull(result);
            verify(seatRepo).findById(seatId);
        }

        @Test
        @DisplayName("Should get all seats")
        void testGetAllSeats() {
            List<Seat> seats = Arrays.asList(seat, seat);
            when(seatRepo.findAll()).thenReturn(seats);
            when(seatMapper.toDataResponse(any())).thenReturn(new SeatDataResponse());

            List<SeatDataResponse> result = seatService.getAllSeats();

            assertEquals(2, result.size());
            verify(seatRepo).findAll();
        }

        @Test
        @DisplayName("Should get seats by room")
        void testGetSeatsByRoom() {
            room.getSeats().addAll(Arrays.asList(seat, seat));
            when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));
            when(seatMapper.toDataResponse(any())).thenReturn(new SeatDataResponse());

            List<SeatDataResponse> result = seatService.getSeatsByRoom(roomId);

            assertEquals(2, result.size());
            verify(roomRepo).findById(roomId);
        }

        @Test
        @DisplayName("Should throw exception for non-existent room")
        void testGetSeatsByRoom_RoomNotFound() {
            when(roomRepo.findById(roomId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                seatService.getSeatsByRoom(roomId);
            });
        }
    }
}
