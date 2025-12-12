package com.api.moviebooking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
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
import com.api.moviebooking.models.dtos.booking.LockSeatsRequest;
import com.api.moviebooking.models.dtos.booking.LockSeatsResponse;
import com.api.moviebooking.models.dtos.booking.SeatAvailabilityResponse;
import com.api.moviebooking.models.entities.*;
import com.api.moviebooking.models.enums.LockOwnerType;
import com.api.moviebooking.models.enums.SeatStatus;
import com.api.moviebooking.models.enums.SeatType;
import com.api.moviebooking.repositories.*;
import com.api.moviebooking.tags.RegressionTest;
import com.api.moviebooking.tags.SanityTest;
import com.api.moviebooking.tags.SmokeTest;

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
    }

    @Nested
    @DisplayName("lockSeats()")
    class LockSeatsTests {

        @Test
        @DisplayName("Should successfully lock available seats")
        void testLockSeats_Success() {
            // Arrange
            List<LockSeatsRequest.SeatWithTicketType> seatRequests = Arrays.asList(
                    new LockSeatsRequest.SeatWithTicketType(seatId1, ticketTypeId),
                    new LockSeatsRequest.SeatWithTicketType(seatId2, ticketTypeId));
            LockSeatsRequest request = new LockSeatsRequest(showtimeId, seatRequests);

            when(seatLockRepo.findAllActiveLocksForOwner(anyString()))
                    .thenReturn(Collections.emptyList());
            when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(mockShowtime));
            when(showtimeSeatRepo.findByIdsAndShowtime(anyList(), eq(showtimeId)))
                    .thenReturn(Arrays.asList(mockSeat1, mockSeat2));
            when(ticketTypeRepo.findById(ticketTypeId)).thenReturn(Optional.of(mockTicketType));
            when(redisLockService.acquireMultipleSeatsLock(eq(showtimeId), anyList(), anyString(), anyLong()))
                    .thenReturn(true);
            when(userRepo.findById(userId)).thenReturn(Optional.of(mockUser));
            when(seatLockRepo.save(any(SeatLock.class)))
                    .thenAnswer(invocation -> {
                        SeatLock lock = invocation.getArgument(0);
                        lock.setId(UUID.randomUUID());
                        return lock;
                    });

            // Mock price calculation
            when(priceCalculationService.calculatePrice(any(), any())).thenReturn(new BigDecimal("10.00"));
            when(ticketTypeService.applyTicketTypeModifier(any(), any())).thenReturn(new BigDecimal("10.00"));

            // Act
            LockSeatsResponse response = bookingService.lockSeats(request, mockSession);

            // Assert
            assertNotNull(response);
            assertEquals(showtimeId, response.getShowtimeId());
            assertEquals(2, response.getLockedSeats().size());
            assertEquals(new BigDecimal("20.00"), response.getTotalPrice());

            verify(showtimeSeatRepo).updateMultipleSeatsStatus(anyList(), eq(SeatStatus.LOCKED));
            verify(seatLockRepo, atLeastOnce()).save(any(SeatLock.class));
        }

        @Test
        @DisplayName("Should release locks for different showtimes before locking new seats")
        void testLockSeats_ReleasesLocksForDifferentShowtimes() {
            // Arrange
            UUID differentShowtimeId = UUID.randomUUID();
            List<LockSeatsRequest.SeatWithTicketType> seatRequests = Arrays.asList(
                    new LockSeatsRequest.SeatWithTicketType(seatId1, ticketTypeId));
            LockSeatsRequest request = new LockSeatsRequest(showtimeId, seatRequests);

            Showtime differentShowtime = new Showtime();
            differentShowtime.setId(differentShowtimeId);

            SeatLock existingLock = new SeatLock();
            existingLock.setId(UUID.randomUUID());
            existingLock.setLockOwnerId(userId.toString());
            existingLock.setShowtime(differentShowtime);
            existingLock.setActive(true);
            existingLock.setLockKey("old-lock-key");

            // Create proper SeatLockSeat structure for release
            SeatLockSeat existingLockSeat = new SeatLockSeat();
            existingLockSeat.setShowtimeSeat(mockSeat2);
            existingLock.setSeatLockSeats(new ArrayList<>(Arrays.asList(existingLockSeat)));

            when(seatLockRepo.findAllActiveLocksForOwner(anyString()))
                    .thenReturn(Arrays.asList(existingLock));
            when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(mockShowtime));
            when(showtimeSeatRepo.findByIdsAndShowtime(anyList(), eq(showtimeId)))
                    .thenReturn(Arrays.asList(mockSeat1));
            when(ticketTypeRepo.findById(ticketTypeId)).thenReturn(Optional.of(mockTicketType));
            when(redisLockService.acquireMultipleSeatsLock(any(), anyList(), anyString(), anyLong()))
                    .thenReturn(true);
            when(userRepo.findById(userId)).thenReturn(Optional.of(mockUser));
            when(priceCalculationService.calculatePrice(any(), any())).thenReturn(new BigDecimal("10.00"));
            when(ticketTypeService.applyTicketTypeModifier(any(), any())).thenReturn(new BigDecimal("10.00"));

            // Act
            bookingService.lockSeats(request, mockSession);

            // Assert
            verify(redisLockService).releaseMultipleSeatsLock(eq(differentShowtimeId), anyList(), eq("old-lock-key"));
            verify(showtimeSeatRepo).updateMultipleSeatsStatus(anyList(), eq(SeatStatus.AVAILABLE));
        }
    }

    @Nested
    @DisplayName("checkAvailability()")
    class CheckAvailabilityTests {

        @Test
        @DisplayName("Should include session lock info when session is provided")
        void testCheckAvailability_WithSessionLock() {
            // Arrange
            SeatLock activeLock = new SeatLock();
            activeLock.setId(UUID.randomUUID());
            activeLock.setLockOwnerId(userId.toString());
            activeLock.setExpiresAt(LocalDateTime.now().plusMinutes(5));
            activeLock.setShowtime(mockShowtime);

            // Create proper SeatLockSeat structure
            SeatLockSeat lockSeat = new SeatLockSeat();
            lockSeat.setShowtimeSeat(mockSeat1);
            activeLock.setSeatLockSeats(new ArrayList<>(Arrays.asList(lockSeat)));

            when(showtimeRepo.existsById(showtimeId)).thenReturn(true);
            when(showtimeSeatRepo.findByShowtimeId(showtimeId))
                    .thenReturn(Arrays.asList(mockSeat1, mockSeat2));
            when(seatLockRepo.findActiveLockByOwnerAndShowtime(eq(userId.toString()), eq(showtimeId)))
                    .thenReturn(Optional.of(activeLock));

            // Act
            SeatAvailabilityResponse response = bookingService.checkAvailability(showtimeId, mockSession);

            // Assert
            assertNotNull(response.getSessionLockInfo());
            assertEquals(activeLock.getId(), response.getSessionLockInfo().getLockId());
            assertEquals(1, response.getSessionLockInfo().getMyLockedSeats().size());
        }
    }

    @Nested
    @DisplayName("releaseSeats()")
    class ReleaseSeatsTests {

        @Test
        @DisplayName("Should successfully release seats")
        void testReleaseSeats_Success() {
            // Arrange
            SeatLock activeLock = new SeatLock();
            activeLock.setId(UUID.randomUUID());
            activeLock.setLockOwnerId(userId.toString());
            activeLock.setShowtime(mockShowtime);
            activeLock.setLockKey("test-lock-token");

            SeatLockSeat lockSeat = new SeatLockSeat();
            lockSeat.setShowtimeSeat(mockSeat1);
            activeLock.setSeatLockSeats(new ArrayList<>(Arrays.asList(lockSeat)));

            when(seatLockRepo.findActiveLockByOwnerAndShowtime(eq(userId.toString()), eq(showtimeId)))
                    .thenReturn(Optional.of(activeLock));

            // Act
            bookingService.releaseSeats(userId.toString(), showtimeId);

            // Assert
            verify(showtimeSeatRepo).updateMultipleSeatsStatus(anyList(), eq(SeatStatus.AVAILABLE));
            verify(redisLockService).releaseMultipleSeatsLock(eq(showtimeId), anyList(), eq("test-lock-token"));
            verify(seatLockRepo).save(argThat(lock -> !lock.isActive()));
        }
    }
}
