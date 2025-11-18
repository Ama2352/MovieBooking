package com.api.moviebooking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
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
import com.api.moviebooking.helpers.mapstructs.ShowtimeSeatMapper;
import com.api.moviebooking.models.dtos.showtimeSeat.ShowtimeSeatDataResponse;
import com.api.moviebooking.models.dtos.showtimeSeat.UpdateShowtimeSeatRequest;
import com.api.moviebooking.models.entities.*;
import com.api.moviebooking.models.enums.SeatStatus;
import com.api.moviebooking.models.enums.SeatType;
import com.api.moviebooking.repositories.ShowtimeRepo;
import com.api.moviebooking.repositories.ShowtimeSeatRepo;

/**
 * Unit tests for ShowtimeSeatService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ShowtimeSeatService Unit Tests")
class ShowtimeSeatServiceTest {

    @Mock
    private ShowtimeSeatRepo showtimeSeatRepo;

    @Mock
    private ShowtimeRepo showtimeRepo;

    @Mock
    private ShowtimeSeatMapper showtimeSeatMapper;

    @Mock
    private PriceCalculationService priceCalculationService;

    @InjectMocks
    private ShowtimeSeatService showtimeSeatService;

    private UUID showtimeId;
    private UUID showtimeSeatId;
    private Showtime showtime;
    private ShowtimeSeat showtimeSeat;
    private Room room;
    private Seat seat1, seat2;

    @BeforeEach
    void setUp() {
        showtimeId = UUID.randomUUID();
        showtimeSeatId = UUID.randomUUID();

        room = new Room();
        room.setId(UUID.randomUUID());

        seat1 = new Seat();
        seat1.setId(UUID.randomUUID());
        seat1.setRoom(room);
        seat1.setRowLabel("A");
        seat1.setSeatNumber(1);
        seat1.setSeatType(SeatType.NORMAL);

        seat2 = new Seat();
        seat2.setId(UUID.randomUUID());
        seat2.setRoom(room);
        seat2.setRowLabel("A");
        seat2.setSeatNumber(2);
        seat2.setSeatType(SeatType.VIP);

        room.setSeats(Arrays.asList(seat1, seat2));

        showtime = new Showtime();
        showtime.setId(showtimeId);
        showtime.setRoom(room);

        showtimeSeat = new ShowtimeSeat();
        showtimeSeat.setId(showtimeSeatId);
        showtimeSeat.setShowtime(showtime);
        showtimeSeat.setSeat(seat1);
        showtimeSeat.setStatus(SeatStatus.AVAILABLE);
        showtimeSeat.setPrice(new BigDecimal("100000"));
    }

    // ========================================================================
    // Generate Showtime Seats Tests
    // ========================================================================

    @Nested
    @DisplayName("Generate Showtime Seats")
    class GenerateSeatsTests {

        @Test
        @DisplayName("Should generate showtime seats successfully")
        void testGenerateShowtimeSeats_Success() {
            Object[] priceData = new Object[] { new BigDecimal("100000"), "{}" };

            when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(showtime));
            when(priceCalculationService.calculatePriceWithBreakdown(any(), any())).thenReturn(priceData);
            when(showtimeSeatRepo.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
            when(showtimeSeatMapper.toDataResponse(any())).thenReturn(new ShowtimeSeatDataResponse());

            List<ShowtimeSeatDataResponse> result = showtimeSeatService
                    .generateShowtimeSeats(showtimeId);

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(showtimeSeatRepo).saveAll(anyList());
            verify(priceCalculationService, times(2)).calculatePriceWithBreakdown(any(), any());
        }

        @Test
        @DisplayName("Should throw exception when showtime not found")
        void testGenerateShowtimeSeats_ShowtimeNotFound() {
            when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                showtimeSeatService.generateShowtimeSeats(showtimeId);
            });

            verify(showtimeSeatRepo, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw exception when room has no seats")
        void testGenerateShowtimeSeats_NoSeats() {
            room.setSeats(Collections.emptyList());

            when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(showtime));

            assertThrows(IllegalStateException.class, () -> {
                showtimeSeatService.generateShowtimeSeats(showtimeId);
            });

            verify(showtimeSeatRepo, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("Should set initial status to AVAILABLE")
        void testGenerateShowtimeSeats_InitialStatus() {
            Object[] priceData = new Object[] { new BigDecimal("100000"), "{}" };

            when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(showtime));
            when(priceCalculationService.calculatePriceWithBreakdown(any(), any())).thenReturn(priceData);
            when(showtimeSeatRepo.saveAll(anyList())).thenAnswer(invocation -> {
                List<ShowtimeSeat> seats = invocation.getArgument(0);
                for (ShowtimeSeat ss : seats) {
                    assertEquals(SeatStatus.AVAILABLE, ss.getStatus());
                }
                return seats;
            });
            when(showtimeSeatMapper.toDataResponse(any())).thenReturn(new ShowtimeSeatDataResponse());

            showtimeSeatService.generateShowtimeSeats(showtimeId);

            verify(showtimeSeatRepo).saveAll(anyList());
        }
    }

    // ========================================================================
    // Update Showtime Seat Tests
    // ========================================================================

    @Nested
    @DisplayName("Update Showtime Seat")
    class UpdateSeatTests {

        @Test
        @DisplayName("Should update seat status")
        void testUpdateShowtimeSeat_Status() {
            UpdateShowtimeSeatRequest request = new UpdateShowtimeSeatRequest();
            request.setStatus("LOCKED");

            when(showtimeSeatRepo.findById(showtimeSeatId)).thenReturn(Optional.of(showtimeSeat));
            when(showtimeSeatMapper.toDataResponse(showtimeSeat))
                    .thenReturn(new ShowtimeSeatDataResponse());

            ShowtimeSeatDataResponse result = showtimeSeatService.updateShowtimeSeat(showtimeSeatId,
                    request);

            assertNotNull(result);
            assertEquals(SeatStatus.LOCKED, showtimeSeat.getStatus());
            verify(showtimeSeatRepo).save(showtimeSeat);
        }

        @Test
        @DisplayName("Should update seat price")
        void testUpdateShowtimeSeat_Price() {
            UpdateShowtimeSeatRequest request = new UpdateShowtimeSeatRequest();
            request.setPrice(new BigDecimal("120000"));

            when(showtimeSeatRepo.findById(showtimeSeatId)).thenReturn(Optional.of(showtimeSeat));
            when(showtimeSeatMapper.toDataResponse(showtimeSeat))
                    .thenReturn(new ShowtimeSeatDataResponse());

            showtimeSeatService.updateShowtimeSeat(showtimeSeatId, request);

            assertEquals(new BigDecimal("120000"), showtimeSeat.getPrice());
        }

        @Test
        @DisplayName("Should update both status and price")
        void testUpdateShowtimeSeat_Both() {
            UpdateShowtimeSeatRequest request = new UpdateShowtimeSeatRequest();
            request.setStatus("BOOKED");
            request.setPrice(new BigDecimal("110000"));

            when(showtimeSeatRepo.findById(showtimeSeatId)).thenReturn(Optional.of(showtimeSeat));
            when(showtimeSeatMapper.toDataResponse(showtimeSeat))
                    .thenReturn(new ShowtimeSeatDataResponse());

            showtimeSeatService.updateShowtimeSeat(showtimeSeatId, request);

            assertEquals(SeatStatus.BOOKED, showtimeSeat.getStatus());
            assertEquals(new BigDecimal("110000"), showtimeSeat.getPrice());
        }

        @Test
        @DisplayName("Should throw exception for invalid status")
        void testUpdateShowtimeSeat_InvalidStatus() {
            UpdateShowtimeSeatRequest request = new UpdateShowtimeSeatRequest();
            request.setStatus("INVALID");

            when(showtimeSeatRepo.findById(showtimeSeatId)).thenReturn(Optional.of(showtimeSeat));

            assertThrows(IllegalArgumentException.class, () -> {
                showtimeSeatService.updateShowtimeSeat(showtimeSeatId, request);
            });
        }

        @Test
        @DisplayName("Should handle case-insensitive status")
        void testUpdateShowtimeSeat_CaseInsensitiveStatus() {
            UpdateShowtimeSeatRequest request = new UpdateShowtimeSeatRequest();
            request.setStatus("locked");

            when(showtimeSeatRepo.findById(showtimeSeatId)).thenReturn(Optional.of(showtimeSeat));
            when(showtimeSeatMapper.toDataResponse(showtimeSeat))
                    .thenReturn(new ShowtimeSeatDataResponse());

            showtimeSeatService.updateShowtimeSeat(showtimeSeatId, request);

            assertEquals(SeatStatus.LOCKED, showtimeSeat.getStatus());
        }
    }

    // ========================================================================
    // Reset Status Tests
    // ========================================================================

    @Nested
    @DisplayName("Reset Seat Status")
    class ResetStatusTests {

        @Test
        @DisplayName("Should reset locked seat to available")
        void testResetShowtimeSeatStatus_Locked() {
            showtimeSeat.setStatus(SeatStatus.LOCKED);

            when(showtimeSeatRepo.findById(showtimeSeatId)).thenReturn(Optional.of(showtimeSeat));
            when(showtimeSeatMapper.toDataResponse(showtimeSeat))
                    .thenReturn(new ShowtimeSeatDataResponse());

            ShowtimeSeatDataResponse result = showtimeSeatService
                    .resetShowtimeSeatStatus(showtimeSeatId);

            assertNotNull(result);
            assertEquals(SeatStatus.AVAILABLE, showtimeSeat.getStatus());
            verify(showtimeSeatRepo).save(showtimeSeat);
        }

        @Test
        @DisplayName("Should reset booked seat to available")
        void testResetShowtimeSeatStatus_Booked() {
            showtimeSeat.setStatus(SeatStatus.BOOKED);

            when(showtimeSeatRepo.findById(showtimeSeatId)).thenReturn(Optional.of(showtimeSeat));
            when(showtimeSeatMapper.toDataResponse(showtimeSeat))
                    .thenReturn(new ShowtimeSeatDataResponse());

            showtimeSeatService.resetShowtimeSeatStatus(showtimeSeatId);

            assertEquals(SeatStatus.AVAILABLE, showtimeSeat.getStatus());
        }
    }

    // ========================================================================
    // Query Tests
    // ========================================================================

    @Nested
    @DisplayName("Query Showtime Seats")
    class QueryTests {

        @Test
        @DisplayName("Should get showtime seat by ID")
        void testGetShowtimeSeat() {
            when(showtimeSeatRepo.findById(showtimeSeatId)).thenReturn(Optional.of(showtimeSeat));
            when(showtimeSeatMapper.toDataResponse(showtimeSeat))
                    .thenReturn(new ShowtimeSeatDataResponse());

            ShowtimeSeatDataResponse result = showtimeSeatService.getShowtimeSeat(showtimeSeatId);

            assertNotNull(result);
            verify(showtimeSeatRepo).findById(showtimeSeatId);
        }

        @Test
        @DisplayName("Should get seats by showtime")
        void testGetSeatsByShowtime() {
            List<ShowtimeSeat> seats = Arrays.asList(showtimeSeat);
            when(showtimeSeatRepo.findByShowtimeId(showtimeId)).thenReturn(seats);
            when(showtimeSeatMapper.toDataResponse(any())).thenReturn(new ShowtimeSeatDataResponse());

            List<ShowtimeSeatDataResponse> result = showtimeSeatService
                    .getShowtimeSeatsByShowtime(showtimeId);

            assertEquals(1, result.size());
            verify(showtimeSeatRepo).findByShowtimeId(showtimeId);
        }

        @Test
        @DisplayName("Should get available seats only")
        void testGetAvailableSeats() {
            List<ShowtimeSeat> availableSeats = Arrays.asList(showtimeSeat);
            when(showtimeSeatRepo.findByShowtimeIdAndStatus(showtimeId, SeatStatus.AVAILABLE))
                    .thenReturn(availableSeats);
            when(showtimeSeatMapper.toDataResponse(any())).thenReturn(new ShowtimeSeatDataResponse());

            List<ShowtimeSeatDataResponse> result = showtimeSeatService
                    .getAvailableShowtimeSeats(showtimeId);

            assertEquals(1, result.size());
            verify(showtimeSeatRepo).findByShowtimeIdAndStatus(showtimeId, SeatStatus.AVAILABLE);
        }

        @Test
        @DisplayName("Should throw exception for non-existent seat")
        void testGetShowtimeSeat_NotFound() {
            when(showtimeSeatRepo.findById(showtimeSeatId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                showtimeSeatService.getShowtimeSeat(showtimeSeatId);
            });
        }
    }

    // ========================================================================
    // Recalculate Prices Tests
    // ========================================================================

    @Nested
    @DisplayName("Recalculate Prices")
    class RecalculatePricesTests {

        @Test
        @DisplayName("Should recalculate prices for all seats")
        void testRecalculatePrices_Success() {
            List<ShowtimeSeat> seats = Arrays.asList(showtimeSeat);
            Object[] priceData = new Object[] { new BigDecimal("95000"), "{\"final\":95000}" };

            when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(showtime));
            when(showtimeSeatRepo.findByShowtimeId(showtimeId)).thenReturn(seats);
            when(priceCalculationService.calculatePriceWithBreakdown(showtime, seat1))
                    .thenReturn(priceData);
            when(showtimeSeatRepo.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
            when(showtimeSeatMapper.toDataResponse(any())).thenReturn(new ShowtimeSeatDataResponse());

            List<ShowtimeSeatDataResponse> result = showtimeSeatService.recalculatePrices(showtimeId);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(new BigDecimal("95000"), showtimeSeat.getPrice());
            verify(showtimeSeatRepo).saveAll(anyList());
        }

        @Test
        @DisplayName("Should update price breakdown")
        void testRecalculatePrices_UpdatesBreakdown() {
            List<ShowtimeSeat> seats = Arrays.asList(showtimeSeat);
            String expectedBreakdown = "{\"base\":80000,\"modifiers\":[]}";
            Object[] priceData = new Object[] { new BigDecimal("80000"), expectedBreakdown };

            when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(showtime));
            when(showtimeSeatRepo.findByShowtimeId(showtimeId)).thenReturn(seats);
            when(priceCalculationService.calculatePriceWithBreakdown(any(), any())).thenReturn(priceData);
            when(showtimeSeatRepo.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
            when(showtimeSeatMapper.toDataResponse(any())).thenReturn(new ShowtimeSeatDataResponse());

            showtimeSeatService.recalculatePrices(showtimeId);

            assertEquals(expectedBreakdown, showtimeSeat.getPriceBreakdown());
        }

        @Test
        @DisplayName("Should throw exception when showtime not found")
        void testRecalculatePrices_ShowtimeNotFound() {
            when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                showtimeSeatService.recalculatePrices(showtimeId);
            });

            verify(showtimeSeatRepo, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("Should recalculate multiple seats")
        void testRecalculatePrices_MultipleSeats() {
            ShowtimeSeat seat2Instance = new ShowtimeSeat();
            seat2Instance.setId(UUID.randomUUID());
            seat2Instance.setShowtime(showtime);
            seat2Instance.setSeat(seat2);
            seat2Instance.setStatus(SeatStatus.AVAILABLE);

            List<ShowtimeSeat> seats = Arrays.asList(showtimeSeat, seat2Instance);

            when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(showtime));
            when(showtimeSeatRepo.findByShowtimeId(showtimeId)).thenReturn(seats);
            when(priceCalculationService.calculatePriceWithBreakdown(any(), any()))
                    .thenReturn(new Object[] { new BigDecimal("100000"), "{}" });
            when(showtimeSeatRepo.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
            when(showtimeSeatMapper.toDataResponse(any())).thenReturn(new ShowtimeSeatDataResponse());

            List<ShowtimeSeatDataResponse> result = showtimeSeatService.recalculatePrices(showtimeId);

            assertEquals(2, result.size());
            verify(priceCalculationService, times(2)).calculatePriceWithBreakdown(any(), any());
        }
    }
}
