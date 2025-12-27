package com.api.moviebooking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.api.moviebooking.helpers.exceptions.ConcurrentBookingException;
import com.api.moviebooking.helpers.exceptions.MaxSeatsExceededException;
import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.exceptions.SeatLockedException;
import com.api.moviebooking.helpers.mapstructs.BookingMapper;
import com.api.moviebooking.models.dtos.SessionContext;
import com.api.moviebooking.models.dtos.booking.DiscountResult;
import com.api.moviebooking.models.dtos.booking.LockSeatsRequest;
import com.api.moviebooking.models.dtos.booking.LockSeatsResponse;
import com.api.moviebooking.models.dtos.booking.PricePreviewRequest;
import com.api.moviebooking.models.dtos.booking.PricePreviewResponse;
import com.api.moviebooking.models.dtos.booking.SeatAvailabilityResponse;
import com.api.moviebooking.models.entities.*;
import com.api.moviebooking.models.enums.LockOwnerType;
import com.api.moviebooking.models.enums.SeatStatus;
import com.api.moviebooking.models.enums.SeatType;
import com.api.moviebooking.repositories.*;
import com.api.moviebooking.tags.RegressionTest;

@ExtendWith(MockitoExtension.class)
@RegressionTest
class BookingServiceTest {

        @Mock
        private SeatLockRepo seatLockRepo;

        @Mock
        private ShowtimeRepo showtimeRepo;

        @Mock
        private ShowtimeSeatRepo showtimeSeatRepo;

        @Mock
        private UserRepo userRepo;

        @Mock
        private TicketTypeRepo ticketTypeRepo;

        @Mock
        private BookingRepo bookingRepo;

        @Mock
        private SnackRepo snackRepo;

        @Mock
        private RedisLockService redisLockService;

        @Mock
        private PriceCalculationService priceCalculationService;

        @Mock
        private TicketTypeService ticketTypeService;

        @Mock
        private BookingMapper bookingMapper;

        @InjectMocks
        private BookingService bookingService;

        private UUID userId;
        private UUID showtimeId;
        private UUID seatId1, seatId2;
        private UUID ticketTypeId;
        private User mockUser;
        private Showtime mockShowtime;
        private ShowtimeSeat mockSeat1, mockSeat2;
        private TicketType mockTicketType;
        private SessionContext mockSession;

        @BeforeEach
        void setUp() {
                // Set configuration values
                ReflectionTestUtils.setField(bookingService, "lockDurationMinutes", 10);
                ReflectionTestUtils.setField(bookingService, "maxSeatsPerBooking", 10);

                // Initialize test data
                userId = UUID.randomUUID();
                showtimeId = UUID.randomUUID();
                seatId1 = UUID.randomUUID();
                seatId2 = UUID.randomUUID();
                ticketTypeId = UUID.randomUUID();

                mockUser = new User();
                mockUser.setId(userId);
                mockUser.setEmail("test@example.com");

                Cinema mockCinema = new Cinema();
                mockCinema.setId(UUID.randomUUID());
                mockCinema.setName("Test Cinema");

                Room mockRoom = new Room();
                mockRoom.setId(UUID.randomUUID());
                mockRoom.setRoomNumber(1);
                mockRoom.setRoomType("IMAX");
                mockRoom.setCinema(mockCinema);

                Movie mockMovie = new Movie();
                mockMovie.setId(UUID.randomUUID());
                mockMovie.setTitle("Test Movie");
                mockMovie.setDuration(120);

                mockShowtime = new Showtime();
                mockShowtime.setId(showtimeId);
                mockShowtime.setRoom(mockRoom);
                mockShowtime.setMovie(mockMovie);
                mockShowtime.setStartTime(LocalDateTime.now().plusHours(2));

                Seat seat1 = new Seat();
                seat1.setId(UUID.randomUUID());
                seat1.setRowLabel("A");
                seat1.setSeatNumber(1);
                seat1.setSeatType(SeatType.NORMAL);

                Seat seat2 = new Seat();
                seat2.setId(UUID.randomUUID());
                seat2.setRowLabel("A");
                seat2.setSeatNumber(2);
                seat2.setSeatType(SeatType.NORMAL);

                mockSeat1 = new ShowtimeSeat();
                mockSeat1.setId(seatId1);
                mockSeat1.setSeat(seat1);
                mockSeat1.setShowtime(mockShowtime);
                mockSeat1.setStatus(SeatStatus.AVAILABLE);
                mockSeat1.setPrice(new BigDecimal("10.00"));

                mockSeat2 = new ShowtimeSeat();
                mockSeat2.setId(seatId2);
                mockSeat2.setSeat(seat2);
                mockSeat2.setShowtime(mockShowtime);
                mockSeat2.setStatus(SeatStatus.AVAILABLE);
                mockSeat2.setPrice(new BigDecimal("10.00"));

                mockTicketType = new TicketType();
                mockTicketType.setId(ticketTypeId);
                mockTicketType.setCode("adult");
                mockTicketType.setLabel("Adult Standard");
                mockTicketType.setModifierType(com.api.moviebooking.models.enums.ModifierType.FIXED_AMOUNT);
                mockTicketType.setModifierValue(BigDecimal.ZERO);

                // Mock Session
                mockSession = new SessionContext();
                mockSession.setUserId(userId);
                mockSession.setLockOwnerId(userId.toString());
                mockSession.setLockOwnerType(LockOwnerType.USER);

                // Common mocks for basic flow
                lenient().when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(mockShowtime));
                lenient().when(userRepo.findById(userId)).thenReturn(Optional.of(mockUser));
                lenient().when(ticketTypeRepo.findById(ticketTypeId)).thenReturn(Optional.of(mockTicketType));
                lenient().when(seatLockRepo.save(any(SeatLock.class))).thenAnswer(i -> {
                        SeatLock l = i.getArgument(0);
                        if (l.getId() == null)
                                l.setId(UUID.randomUUID());
                        if (l.getSeatLockSeats() == null)
                                l.setSeatLockSeats(new ArrayList<>());
                        return l;
                });
        }

        // ==================== Price Preview Tests (V(G)=5) ====================

        @Nested
        @DisplayName("calculatePricePreview()")
        class PricePreviewTests {

                @Test
                @DisplayName("Test 1/5: Should calculate preview for valid active lock")
                void testPricePreview_ValidLock() {
                        // Arrange
                        SeatLock seatLock = new SeatLock();
                        seatLock.setId(UUID.randomUUID());
                        seatLock.setLockOwnerId(userId.toString());
                        seatLock.setActive(true);
                        seatLock.setShowtime(mockShowtime);
                        seatLock.setSeatLockSeats(new ArrayList<>());

                        SeatLockSeat sls = new SeatLockSeat();
                        sls.setPrice(new BigDecimal("100.00"));
                        seatLock.getSeatLockSeats().add(sls);

                        PricePreviewRequest request = PricePreviewRequest.builder()
                                        .lockId(seatLock.getId())
                                        .build();

                        when(seatLockRepo.findById(request.getLockId())).thenReturn(Optional.of(seatLock));
                        when(priceCalculationService.calculateDiscounts(any(), any(), any()))
                                        .thenReturn(DiscountResult.builder()
                                                        .totalDiscount(BigDecimal.ZERO)
                                                        .membershipDiscount(BigDecimal.ZERO)
                                                        .promotionDiscount(BigDecimal.ZERO)
                                                        .discountReason("")
                                                        .build());

                        // Act
                        PricePreviewResponse response = bookingService.calculatePricePreview(request, mockSession);

                        // Assert
                        assertNotNull(response);
                        assertEquals(new BigDecimal("100.00"), response.getSubtotal());
                }

                @Test
                @DisplayName("Test 2/5: Should fail when lock ownership doesn't match")
                void testPricePreview_OwnershipMismatch() {
                        SeatLock seatLock = new SeatLock();
                        seatLock.setId(UUID.randomUUID());
                        seatLock.setLockOwnerId("other-user-uuid");
                        seatLock.setActive(true);

                        PricePreviewRequest request = PricePreviewRequest.builder()
                                        .lockId(seatLock.getId())
                                        .build();

                        when(seatLockRepo.findById(request.getLockId())).thenReturn(Optional.of(seatLock));

                        assertThrows(IllegalArgumentException.class,
                                        () -> bookingService.calculatePricePreview(request, mockSession));
                }

                @Test
                @DisplayName("Test 3/5: Should fail when lock is inactive")
                void testPricePreview_InactiveLock() {
                        SeatLock seatLock = new SeatLock();
                        seatLock.setId(UUID.randomUUID());
                        seatLock.setLockOwnerId(userId.toString());
                        seatLock.setActive(false);

                        PricePreviewRequest request = PricePreviewRequest.builder()
                                        .lockId(seatLock.getId())
                                        .build();

                        when(seatLockRepo.findById(request.getLockId())).thenReturn(Optional.of(seatLock));

                        assertThrows(IllegalArgumentException.class,
                                        () -> bookingService.calculatePricePreview(request, mockSession));
                }

                @Test
                @DisplayName("Test 4/5: Should include snacks in calculation")
                void testPricePreview_WithSnacks() {
                        SeatLock seatLock = new SeatLock();
                        seatLock.setId(UUID.randomUUID());
                        seatLock.setLockOwnerId(userId.toString());
                        seatLock.setActive(true);
                        seatLock.setSeatLockSeats(new ArrayList<>());

                        Snack snack = new Snack();
                        snack.setId(UUID.randomUUID());
                        snack.setPrice(new BigDecimal("50.00"));

                        PricePreviewRequest request = PricePreviewRequest.builder()
                                        .lockId(seatLock.getId())
                                        .snacks(List.of(PricePreviewRequest.SnackItem.builder()
                                                        .snackId(snack.getId())
                                                        .quantity(2)
                                                        .build()))
                                        .build();

                        when(seatLockRepo.findById(request.getLockId())).thenReturn(Optional.of(seatLock));
                        when(snackRepo.findById(snack.getId())).thenReturn(Optional.of(snack));
                        when(priceCalculationService.calculateDiscounts(any(), any(), any()))
                                        .thenReturn(DiscountResult.builder()
                                                        .totalDiscount(BigDecimal.ZERO)
                                                        .membershipDiscount(BigDecimal.ZERO)
                                                        .promotionDiscount(BigDecimal.ZERO)
                                                        .discountReason("")
                                                        .build());

                        PricePreviewResponse response = bookingService.calculatePricePreview(request, mockSession);

                        // 0 (seats) + 50*2 (snacks) = 100
                        assertEquals(new BigDecimal("100.00"), response.getSubtotal());
                }

                @Test
                @DisplayName("Test 5/5: Should apply discounts correctly")
                void testPricePreview_WithDiscounts() {
                        SeatLock seatLock = new SeatLock();
                        seatLock.setId(UUID.randomUUID());
                        seatLock.setLockOwnerId(userId.toString());
                        seatLock.setActive(true);
                        seatLock.setSeatLockSeats(new ArrayList<>());

                        SeatLockSeat sls = new SeatLockSeat();
                        sls.setPrice(new BigDecimal("100.00"));
                        seatLock.getSeatLockSeats().add(sls);

                        PricePreviewRequest request = PricePreviewRequest.builder()
                                        .lockId(seatLock.getId())
                                        .build();

                        when(seatLockRepo.findById(request.getLockId())).thenReturn(Optional.of(seatLock));
                        when(priceCalculationService.calculateDiscounts(any(), any(), any()))
                                        .thenReturn(DiscountResult.builder()
                                                        .totalDiscount(new BigDecimal("10.00"))
                                                        .membershipDiscount(new BigDecimal("10.00"))
                                                        .promotionDiscount(BigDecimal.ZERO)
                                                        .discountReason("PROMO")
                                                        .build());

                        PricePreviewResponse response = bookingService.calculatePricePreview(request, mockSession);

                        assertEquals(new BigDecimal("100.00"), response.getSubtotal());
                        assertEquals(new BigDecimal("10.00"), response.getDiscount());
                        assertEquals(new BigDecimal("90.00"), response.getTotal());
                }
        }

        // ==================== Lock Seats Tests (V(G)=13) ====================

        @Nested
        @DisplayName("lockSeats()")
        class LockSeatsTests {

                private LockSeatsRequest createRequest(List<UUID> seats) {
                        List<LockSeatsRequest.SeatWithTicketType> seatRequests = new ArrayList<>();
                        for (UUID seatId : seats) {
                                seatRequests.add(new LockSeatsRequest.SeatWithTicketType(seatId,
                                                ticketTypeId));
                        }
                        return new LockSeatsRequest(showtimeId, seatRequests);
                }

                private List<UUID> randomIds(int count) {
                        List<UUID> ids = new ArrayList<>();
                        for (int i = 0; i < count; i++)
                                ids.add(UUID.randomUUID());
                        return ids;
                }

                @Test
                @DisplayName("Test 1/13: Should throw exception when requested seats exceed max limit")
                void testLockSeats_MaxSeatsExceeded() {
                        LockSeatsRequest request = createRequest(randomIds(11)); // Max is 10
                        assertThrows(MaxSeatsExceededException.class,
                                        () -> bookingService.lockSeats(request, mockSession));
                }

                @Test
                @DisplayName("Test 2/13: Should handle existing locks - different showtime release")
                void testLockSeats_ReleaseDifferentShowtimeLocks() {
                        // Arrange
                        LockSeatsRequest request = createRequest(List.of(seatId1));

                        SeatLock existingLock = new SeatLock();
                        existingLock.setId(UUID.randomUUID());
                        existingLock.setLockOwnerId(userId.toString());
                        existingLock.setShowtime(new Showtime()); // Different showtime
                        existingLock.getShowtime().setId(UUID.randomUUID());
                        existingLock.setActive(true);
                        existingLock.setLockKey("old-key");
                        existingLock.setSeatLockSeats(new ArrayList<>());

                        SeatLockSeat sls = new SeatLockSeat();
                        sls.setShowtimeSeat(mockSeat1);
                        existingLock.getSeatLockSeats().add(sls);

                        when(seatLockRepo.findAllActiveLocksForOwner(userId.toString()))
                                        .thenReturn(List.of(existingLock));
                        when(showtimeSeatRepo.findByIdsAndShowtime(any(), eq(showtimeId)))
                                        .thenReturn(List.of(mockSeat1));
                        when(redisLockService.acquireMultipleSeatsLock(any(), any(), any(), anyLong()))
                                        .thenReturn(true);
                        when(priceCalculationService.calculatePrice(any(), any())).thenReturn(BigDecimal.TEN);
                        when(ticketTypeService.applyTicketTypeModifier(any(), any())).thenReturn(BigDecimal.TEN);

                        // Act
                        bookingService.lockSeats(request, mockSession);

                        // Assert
                        verify(redisLockService).releaseMultipleSeatsLock(any(), any(), eq("old-key"));
                }

                @Test
                @DisplayName("Test 3/13: Should throw exception for concurrent booking (same showtime lock)")
                void testLockSeats_ConcurrentBookingSameShowtime() {
                        LockSeatsRequest request = createRequest(List.of(seatId1));

                        SeatLock existingLock = new SeatLock();
                        existingLock.setShowtime(mockShowtime); // Same showtime
                        existingLock.setActive(true);

                        when(seatLockRepo.findAllActiveLocksForOwner(userId.toString()))
                                        .thenReturn(List.of(existingLock));

                        assertThrows(ConcurrentBookingException.class,
                                        () -> bookingService.lockSeats(request, mockSession));
                }

                @Test
                @DisplayName("Test 4/13: Should throw exception when showtime not found")
                void testLockSeats_ShowtimeNotFound() {
                        LockSeatsRequest request = createRequest(List.of(seatId1));
                        when(seatLockRepo.findAllActiveLocksForOwner(any())).thenReturn(Collections.emptyList());
                        when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.empty());

                        assertThrows(ResourceNotFoundException.class,
                                        () -> bookingService.lockSeats(request, mockSession));
                }

                @Test
                @DisplayName("Test 5/13: Should throw exception when some seats not found")
                void testLockSeats_SeatsNotFound() {
                        LockSeatsRequest request = createRequest(List.of(seatId1, seatId2));
                        when(seatLockRepo.findAllActiveLocksForOwner(any())).thenReturn(Collections.emptyList());
                        when(showtimeSeatRepo.findByIdsAndShowtime(any(), eq(showtimeId)))
                                        .thenReturn(List.of(mockSeat1)); // Found 1 but requested 2

                        assertThrows(ResourceNotFoundException.class,
                                        () -> bookingService.lockSeats(request, mockSession));
                }

                @Test
                @DisplayName("Test 6/13: Should validate ticket types for showtime")
                void testLockSeats_ValidateTicketTypes() {
                        LockSeatsRequest request = createRequest(List.of(seatId1));

                        when(seatLockRepo.findAllActiveLocksForOwner(any())).thenReturn(Collections.emptyList());
                        when(showtimeSeatRepo.findByIdsAndShowtime(any(), eq(showtimeId)))
                                        .thenReturn(List.of(mockSeat1));

                        doThrow(new IllegalArgumentException("Invalid ticket type"))
                                        .when(ticketTypeService).validateTicketTypeForShowtime(any(), any());

                        assertThrows(IllegalArgumentException.class,
                                        () -> bookingService.lockSeats(request, mockSession));
                }

                @Test
                @DisplayName("Test 7/13: Should throw exception when seats already locked/booked")
                void testLockSeats_SeatsNotAvailable() {
                        LockSeatsRequest request = createRequest(List.of(seatId1));
                        mockSeat1.setStatus(SeatStatus.LOCKED);

                        when(seatLockRepo.findAllActiveLocksForOwner(any())).thenReturn(Collections.emptyList());
                        when(showtimeSeatRepo.findByIdsAndShowtime(any(), eq(showtimeId)))
                                        .thenReturn(List.of(mockSeat1));

                        assertThrows(SeatLockedException.class, () -> bookingService.lockSeats(request, mockSession));
                }

                @Test
                @DisplayName("Test 8/13: Should throw exception when Redis lock acquisition fails")
                void testLockSeats_RedisLockFailed() {
                        LockSeatsRequest request = createRequest(List.of(seatId1));

                        when(seatLockRepo.findAllActiveLocksForOwner(any())).thenReturn(Collections.emptyList());
                        when(showtimeSeatRepo.findByIdsAndShowtime(any(), eq(showtimeId)))
                                        .thenReturn(List.of(mockSeat1));
                        when(redisLockService.acquireMultipleSeatsLock(any(), any(), any(), anyLong()))
                                        .thenReturn(false);

                        assertThrows(ConcurrentBookingException.class,
                                        () -> bookingService.lockSeats(request, mockSession));
                }

                @Test
                @DisplayName("Test 9/13: Should handle authenticated user linkage")
                void testLockSeats_AuthenticatedUser() {
                        LockSeatsRequest request = createRequest(List.of(seatId1));

                        when(seatLockRepo.findAllActiveLocksForOwner(any())).thenReturn(Collections.emptyList());
                        when(showtimeSeatRepo.findByIdsAndShowtime(any(), eq(showtimeId)))
                                        .thenReturn(List.of(mockSeat1));
                        when(redisLockService.acquireMultipleSeatsLock(any(), any(), any(), anyLong()))
                                        .thenReturn(true);
                        when(priceCalculationService.calculatePrice(any(), any())).thenReturn(BigDecimal.TEN);
                        when(ticketTypeService.applyTicketTypeModifier(any(), any())).thenReturn(BigDecimal.TEN);

                        bookingService.lockSeats(request, mockSession);

                        verify(userRepo).findById(userId);
                }

                @Test
                @DisplayName("Test 10/13: Should throw exception when internal mapping logic fails")
                void testLockSeats_TicketTypeMappingError() {
                        // This is hard to trigger with valid request construction, but simulating
                        // internal error
                        // Using a request where we don't map correctly?
                        // Just use a random ID that won't match mockSeat1's ID
                        LockSeatsRequest request = createRequest(randomIds(1));
                        // We'll mock ticketTypeRepo to fail finding it

                        when(seatLockRepo.findAllActiveLocksForOwner(any())).thenReturn(Collections.emptyList());
                        when(showtimeSeatRepo.findByIdsAndShowtime(any(), eq(showtimeId)))
                                        .thenReturn(List.of(mockSeat1));
                        when(redisLockService.acquireMultipleSeatsLock(any(), any(), any(), anyLong()))
                                        .thenReturn(true);

                        assertThrows(IllegalArgumentException.class,
                                        () -> bookingService.lockSeats(request, mockSession));
                }

                @Test
                @DisplayName("Test 11/13: Should throw exception when ticket type not found in repo")
                void testLockSeats_TicketTypeNotFound() {
                        LockSeatsRequest request = createRequest(List.of(seatId1));

                        when(seatLockRepo.findAllActiveLocksForOwner(any())).thenReturn(Collections.emptyList());
                        when(showtimeSeatRepo.findByIdsAndShowtime(any(), eq(showtimeId)))
                                        .thenReturn(List.of(mockSeat1));
                        when(redisLockService.acquireMultipleSeatsLock(any(), any(), any(), anyLong()))
                                        .thenReturn(true);
                        when(ticketTypeRepo.findById(any())).thenReturn(Optional.empty());

                        assertThrows(ResourceNotFoundException.class,
                                        () -> bookingService.lockSeats(request, mockSession));
                }

                @Test
                @DisplayName("Test 12/13: Should rollback Redis lock on database exception")
                void testLockSeats_RollbackOnException() {
                        LockSeatsRequest request = createRequest(List.of(seatId1));

                        when(seatLockRepo.findAllActiveLocksForOwner(any())).thenReturn(Collections.emptyList());
                        when(showtimeSeatRepo.findByIdsAndShowtime(any(), eq(showtimeId)))
                                        .thenReturn(List.of(mockSeat1));
                        when(redisLockService.acquireMultipleSeatsLock(any(), any(), any(), anyLong()))
                                        .thenReturn(true);

                        // Throw exception during save
                        when(seatLockRepo.save(any(SeatLock.class))).thenThrow(new RuntimeException("DB Error"));

                        assertThrows(RuntimeException.class, () -> bookingService.lockSeats(request, mockSession));

                        // Verify rollback
                        verify(redisLockService).releaseMultipleSeatsLock(any(), any(), any());
                }

                @Test
                @DisplayName("Test 13/13: Happy path - Successful lock")
                void testLockSeats_Success() {
                        LockSeatsRequest request = createRequest(List.of(seatId1, seatId2));

                        when(seatLockRepo.findAllActiveLocksForOwner(any())).thenReturn(Collections.emptyList());
                        when(showtimeSeatRepo.findByIdsAndShowtime(any(), eq(showtimeId)))
                                        .thenReturn(List.of(mockSeat1, mockSeat2));
                        when(redisLockService.acquireMultipleSeatsLock(any(), any(), any(), anyLong()))
                                        .thenReturn(true);
                        when(priceCalculationService.calculatePrice(any(), any())).thenReturn(BigDecimal.TEN);
                        when(ticketTypeService.applyTicketTypeModifier(any(), any())).thenReturn(BigDecimal.TEN);

                        LockSeatsResponse response = bookingService.lockSeats(request, mockSession);

                        assertNotNull(response);
                        assertEquals(2, response.getLockedSeats().size());
                        verify(seatLockRepo, atLeastOnce()).save(any(SeatLock.class));
                }
        }

        // ==================== Check Availability Tests (V(G)=6) ====================

        @Nested
        @DisplayName("checkAvailability()")
        class CheckAvailabilityTests {

                @Test
                @DisplayName("Test 1/6: Should throw exception if showtime not found")
                void testCheckAvailability_ShowtimeNotFound() {
                        when(showtimeRepo.existsById(showtimeId)).thenReturn(false);
                        assertThrows(ResourceNotFoundException.class,
                                        () -> bookingService.checkAvailability(showtimeId, mockSession));
                }

                @Test
                @DisplayName("Test 2/6: Should categorize AVAILABLE seats")
                void testCheckAvailability_Available() {
                        mockSeat1.setStatus(SeatStatus.AVAILABLE);
                        when(showtimeRepo.existsById(showtimeId)).thenReturn(true);
                        when(showtimeSeatRepo.findByShowtimeId(showtimeId)).thenReturn(List.of(mockSeat1));

                        SeatAvailabilityResponse response = bookingService.checkAvailability(showtimeId, null);

                        assertTrue(response.getAvailableSeats().contains(mockSeat1.getId()));
                }

                @Test
                @DisplayName("Test 3/6: Should categorize LOCKED seats")
                void testCheckAvailability_Locked() {
                        mockSeat1.setStatus(SeatStatus.LOCKED);
                        when(showtimeRepo.existsById(showtimeId)).thenReturn(true);
                        when(showtimeSeatRepo.findByShowtimeId(showtimeId)).thenReturn(List.of(mockSeat1));

                        SeatAvailabilityResponse response = bookingService.checkAvailability(showtimeId, null);

                        assertTrue(response.getLockedSeats().contains(mockSeat1.getId()));
                }

                @Test
                @DisplayName("Test 4/6: Should categorize BOOKED seats")
                void testCheckAvailability_Booked() {
                        mockSeat1.setStatus(SeatStatus.BOOKED);
                        when(showtimeRepo.existsById(showtimeId)).thenReturn(true);
                        when(showtimeSeatRepo.findByShowtimeId(showtimeId)).thenReturn(List.of(mockSeat1));

                        SeatAvailabilityResponse response = bookingService.checkAvailability(showtimeId, null);

                        assertTrue(response.getBookedSeats().contains(mockSeat1.getId()));
                }

                @Test
                @DisplayName("Test 5/6: Should handle null session")
                void testCheckAvailability_NullSession() {
                        mockSeat1.setStatus(SeatStatus.AVAILABLE);
                        when(showtimeRepo.existsById(showtimeId)).thenReturn(true);
                        when(showtimeSeatRepo.findByShowtimeId(showtimeId)).thenReturn(List.of(mockSeat1));

                        SeatAvailabilityResponse response = bookingService.checkAvailability(showtimeId, null);

                        assertNull(response.getSessionLockInfo());
                }

                @Test
                @DisplayName("Test 6/6: Should include session lock info when active lock exists")
                void testCheckAvailability_WithActiveLock() {
                        SeatLock activeLock = new SeatLock();
                        activeLock.setId(UUID.randomUUID());
                        activeLock.setLockOwnerId(userId.toString());
                        activeLock.setExpiresAt(LocalDateTime.now().plusMinutes(5));
                        activeLock.setShowtime(mockShowtime);

                        SeatLockSeat sls = new SeatLockSeat();
                        sls.setShowtimeSeat(mockSeat1);
                        activeLock.setSeatLockSeats(new ArrayList<>(List.of(sls)));

                        when(showtimeRepo.existsById(showtimeId)).thenReturn(true);
                        when(showtimeSeatRepo.findByShowtimeId(showtimeId)).thenReturn(List.of(mockSeat1));
                        when(seatLockRepo.findActiveLockByOwnerAndShowtime(userId.toString(), showtimeId))
                                        .thenReturn(Optional.of(activeLock));

                        SeatAvailabilityResponse response = bookingService.checkAvailability(showtimeId, mockSession);

                        assertNotNull(response.getSessionLockInfo());
                        assertEquals(1, response.getSessionLockInfo().getMyLockedSeats().size());
                }
        }

        // ==================== Release Seats Tests (V(G)=2) ====================

        @Nested
        @DisplayName("releaseSeats()")
        class ReleaseSeatsTests {

                @Test
                @DisplayName("Test 1/2: Should release seats when lock exists")
                void testReleaseSeats_LockExists() {
                        SeatLock activeLock = new SeatLock();
                        activeLock.setId(UUID.randomUUID());
                        activeLock.setLockOwnerId(userId.toString());
                        activeLock.setShowtime(mockShowtime);
                        activeLock.setLockKey("key");
                        activeLock.setSeatLockSeats(new ArrayList<>());

                        SeatLockSeat sls = new SeatLockSeat();
                        sls.setShowtimeSeat(mockSeat1);
                        activeLock.getSeatLockSeats().add(sls);

                        when(seatLockRepo.findActiveLockByOwnerAndShowtime(userId.toString(), showtimeId))
                                        .thenReturn(Optional.of(activeLock));

                        bookingService.releaseSeats(userId.toString(), showtimeId);

                        verify(showtimeSeatRepo).updateMultipleSeatsStatus(any(), eq(SeatStatus.AVAILABLE));
                        verify(redisLockService).releaseMultipleSeatsLock(any(), any(), any());
                }

                @Test
                @DisplayName("Test 2/2: Should do nothing when no lock exists")
                void testReleaseSeats_NoLock() {
                        when(seatLockRepo.findActiveLockByOwnerAndShowtime(any(), any()))
                                        .thenReturn(Optional.empty());

                        bookingService.releaseSeats(userId.toString(), showtimeId);

                        verify(showtimeSeatRepo, never()).updateMultipleSeatsStatus(any(), any());
                }
        }
}
