// package com.api.moviebooking.services;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.util.Arrays;
// import java.util.List;
// import java.util.Optional;
// import java.util.UUID;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.test.util.ReflectionTestUtils;

// import com.api.moviebooking.helpers.exceptions.ConcurrentBookingException;
// import com.api.moviebooking.helpers.exceptions.LockExpiredException;
// import com.api.moviebooking.helpers.exceptions.MaxSeatsExceededException;
// import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
// import com.api.moviebooking.helpers.exceptions.SeatLockedException;
// import com.api.moviebooking.helpers.mapstructs.BookingMapper;
// import com.api.moviebooking.models.dtos.booking.LockSeatsRequest;
// import com.api.moviebooking.models.dtos.booking.LockSeatsResponse;
// import com.api.moviebooking.models.dtos.booking.BookingResponse;
// import com.api.moviebooking.models.entities.*;
// import com.api.moviebooking.models.enums.SeatStatus;
// import com.api.moviebooking.models.enums.SeatType;
// import com.api.moviebooking.repositories.*;

// /**
// * White-Box Testing for BookingService
// * Tests organized by method with cyclomatic complexity from decision tables
// * Total Methods: 7
// * Total Cyclomatic Complexity: 21
// * Total Test Cases: 21
// */
// @ExtendWith(MockitoExtension.class)
// class BookingServiceTest {

// @Mock
// private RedisLockService redisLockService;

// @Mock
// private SeatLockRepo seatLockRepo;

// @Mock
// private ShowtimeSeatRepo showtimeSeatRepo;

// @Mock
// private ShowtimeRepo showtimeRepo;

// @Mock
// private UserRepo userRepo;

// @Mock
// private BookingRepo bookingRepo;

// @Mock
// private PromotionService promotionService;

// @Mock
// private CheckoutLifecycleService checkoutLifecycleService;

// @Mock
// private BookingMapper bookingMapper;

// @InjectMocks
// private BookingService bookingService;

// private UUID userId;
// private UUID showtimeId;
// private UUID seatId1, seatId2;
// private User mockUser;
// private Showtime mockShowtime;
// private ShowtimeSeat mockSeat1, mockSeat2;
// private Cinema mockCinema;
// private Room mockRoom;
// private Movie mockMovie;

// @BeforeEach
// void setUp() {
// // Set configuration values
// ReflectionTestUtils.setField(bookingService, "lockDurationMinutes", 10);
// ReflectionTestUtils.setField(bookingService, "maxSeatsPerBooking", 10);

// // Initialize test data
// userId = UUID.randomUUID();
// showtimeId = UUID.randomUUID();
// seatId1 = UUID.randomUUID();
// seatId2 = UUID.randomUUID();

// mockUser = new User();
// mockUser.setId(userId);
// mockUser.setEmail("test@example.com");

// mockCinema = new Cinema();
// mockCinema.setId(UUID.randomUUID());
// mockCinema.setName("Test Cinema");

// mockRoom = new Room();
// mockRoom.setId(UUID.randomUUID());
// mockRoom.setRoomNumber(1);
// mockRoom.setRoomType("IMAX");
// mockRoom.setCinema(mockCinema);

// mockMovie = new Movie();
// mockMovie.setId(UUID.randomUUID());
// mockMovie.setTitle("Test Movie");
// mockMovie.setDuration(120);

// mockShowtime = new Showtime();
// mockShowtime.setId(showtimeId);
// mockShowtime.setRoom(mockRoom);
// mockShowtime.setMovie(mockMovie);
// mockShowtime.setStartTime(LocalDateTime.now().plusHours(2));

// Seat seat1 = new Seat();
// seat1.setId(UUID.randomUUID());
// seat1.setRowLabel("A");
// seat1.setSeatNumber(1);
// seat1.setSeatType(SeatType.NORMAL);

// Seat seat2 = new Seat();
// seat2.setId(UUID.randomUUID());
// seat2.setRowLabel("A");
// seat2.setSeatNumber(2);
// seat2.setSeatType(SeatType.NORMAL);

// mockSeat1 = new ShowtimeSeat();
// mockSeat1.setId(seatId1);
// mockSeat1.setSeat(seat1);
// mockSeat1.setShowtime(mockShowtime);
// mockSeat1.setStatus(SeatStatus.AVAILABLE);
// mockSeat1.setPrice(new BigDecimal("10.00"));

// mockSeat2 = new ShowtimeSeat();
// mockSeat2.setId(seatId2);
// mockSeat2.setSeat(seat2);
// mockSeat2.setShowtime(mockShowtime);
// mockSeat2.setStatus(SeatStatus.AVAILABLE);
// mockSeat2.setPrice(new BigDecimal("10.00"));
// }

// // ========================================================================
// // 1. lockSeats() Tests
// // Cyclomatic Complexity: V(G) = 10
// // Minimum Test Cases Required: 10
// // Decision Nodes: size>max, !isEmpty(existingLocks),
// // isPresent(sameShowtimeLock),
// // size!=expected(seats), !isEmpty(unavailableSeats), !redisLocked,
// // try-catch, for-each(existingLocks), for-each(unavailableSeats)
// // ========================================================================

// @Nested
// @DisplayName("lockSeats() - V(G)=10, Min Tests=10")
// class LockSeatsTests {

// /**
// * Test Case TC-1: Happy path with all validations passing
// */
// @Test
// @DisplayName("TC-1: Should successfully lock available seats")
// void testLockSeats_Success() {
// // Arrange
// LockSeatsRequest request = new LockSeatsRequest(showtimeId, userId,
// Arrays.asList(seatId1, seatId2));

// when(seatLockRepo.findAllActiveLocksForUser(userId))
// .thenReturn(Arrays.asList()); // No existing locks
// when(userRepo.findById(userId)).thenReturn(Optional.of(mockUser));
// when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(mockShowtime));
// when(showtimeSeatRepo.findByIdsAndShowtime(anyList(), eq(showtimeId)))
// .thenReturn(Arrays.asList(mockSeat1, mockSeat2));
// when(redisLockService.acquireMultipleSeatsLock(eq(showtimeId), anyList(),
// anyString(), anyLong()))
// .thenReturn(true);
// when(seatLockRepo.save(any(SeatLock.class)))
// .thenAnswer(invocation -> {
// SeatLock lock = invocation.getArgument(0);
// lock.setId(UUID.randomUUID());
// return lock;
// });

// // Act
// LockSeatsResponse response = bookingService.lockSeats(request);

// // Assert
// assertNotNull(response);
// assertEquals(showtimeId, response.getShowtimeId());
// assertEquals(2, response.getLockedSeats().size());
// assertEquals(new BigDecimal("20.00"), response.getTotalPrice());
// assertTrue(response.getRemainingSeconds() > 0);

// verify(showtimeSeatRepo).updateMultipleSeatsStatus(anyList(),
// eq(SeatStatus.LOCKED));
// verify(seatLockRepo).save(any(SeatLock.class));
// }

// /**
// * Test Case TC-2: Exceeds maximum seats limit
// */
// @Test
// @DisplayName("TC-2: Should throw MaxSeatsExceededException when requesting
// too many seats")
// void testLockSeats_TooManySeats() {
// // Arrange
// List<UUID> tooManySeats = Arrays.asList(
// UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
// UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
// UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
// UUID.randomUUID(), UUID.randomUUID() // 11 seats
// );
// LockSeatsRequest request = new LockSeatsRequest(showtimeId, userId,
// tooManySeats);

// // Act & Assert
// assertThrows(MaxSeatsExceededException.class, () -> {
// bookingService.lockSeats(request);
// });
// }

// /**
// * Test Case TC-3: Prevents duplicate booking for same showtime
// */
// @Test
// @DisplayName("TC-3: Should throw ConcurrentBookingException when user has
// lock for same showtime")
// void testLockSeats_UserHasLockForSameShowtime() {
// // Arrange
// LockSeatsRequest request = new LockSeatsRequest(showtimeId, userId,
// Arrays.asList(seatId1));

// SeatLock existingLock = new SeatLock();
// existingLock.setId(UUID.randomUUID());
// existingLock.setUser(mockUser);
// existingLock.setShowtime(mockShowtime);
// existingLock.setActive(true);

// when(seatLockRepo.findAllActiveLocksForUser(userId))
// .thenReturn(Arrays.asList(existingLock));

// // Act & Assert
// assertThrows(ConcurrentBookingException.class, () -> {
// bookingService.lockSeats(request);
// });

// verify(userRepo, never()).findById(any());
// }

// /**
// * Test Case TC-4: Auto-cleanup previous locks for different showtimes
// */
// @Test
// @DisplayName("TC-4: Should release locks for different showtimes before
// locking new seats")
// void testLockSeats_ReleasesLocksForDifferentShowtimes() {
// // Arrange
// UUID differentShowtimeId = UUID.randomUUID();
// LockSeatsRequest request = new LockSeatsRequest(showtimeId, userId,
// Arrays.asList(seatId1));

// Showtime differentShowtime = new Showtime();
// differentShowtime.setId(differentShowtimeId);
// differentShowtime.setRoom(mockRoom);
// differentShowtime.setMovie(mockMovie);

// SeatLock existingLock = new SeatLock();
// existingLock.setId(UUID.randomUUID());
// existingLock.setUser(mockUser);
// existingLock.setShowtime(differentShowtime); // Different showtime
// existingLock.setLockedSeats(Arrays.asList(mockSeat2));
// existingLock.setActive(true);
// existingLock.setLockKey("old-lock-key");

// when(seatLockRepo.findAllActiveLocksForUser(userId))
// .thenReturn(Arrays.asList(existingLock));
// when(userRepo.findById(userId)).thenReturn(Optional.of(mockUser));
// when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(mockShowtime));
// when(showtimeSeatRepo.findByIdsAndShowtime(anyList(), eq(showtimeId)))
// .thenReturn(Arrays.asList(mockSeat1));
// when(redisLockService.acquireMultipleSeatsLock(eq(showtimeId), anyList(),
// anyString(), anyLong()))
// .thenReturn(true);
// when(seatLockRepo.save(any(SeatLock.class)))
// .thenAnswer(invocation -> {
// SeatLock lock = invocation.getArgument(0);
// lock.setId(UUID.randomUUID());
// return lock;
// });

// // Act
// bookingService.lockSeats(request);

// // Assert - Old lock should be released
// verify(showtimeSeatRepo, atLeastOnce()).updateMultipleSeatsStatus(anyList(),
// eq(SeatStatus.AVAILABLE));
// verify(redisLockService).releaseMultipleSeatsLock(eq(differentShowtimeId),
// anyList(), eq("old-lock-key"));
// }

// /**
// * Test Case TC-5: Invalid user ID
// */
// @Test
// @DisplayName("TC-5: Should throw ResourceNotFoundException when user not
// found")
// void testLockSeats_UserNotFound() {
// // Arrange
// LockSeatsRequest request = new LockSeatsRequest(showtimeId, userId,
// Arrays.asList(seatId1));

// when(seatLockRepo.findAllActiveLocksForUser(userId))
// .thenReturn(Arrays.asList());
// when(userRepo.findById(userId)).thenReturn(Optional.empty());

// // Act & Assert
// assertThrows(ResourceNotFoundException.class, () -> {
// bookingService.lockSeats(request);
// });
// }

// /**
// * Test Case TC-6: Invalid showtime ID
// */
// @Test
// @DisplayName("TC-6: Should throw ResourceNotFoundException when showtime not
// found")
// void testLockSeats_ShowtimeNotFound() {
// // Arrange
// LockSeatsRequest request = new LockSeatsRequest(showtimeId, userId,
// Arrays.asList(seatId1));

// when(seatLockRepo.findAllActiveLocksForUser(userId))
// .thenReturn(Arrays.asList());
// when(userRepo.findById(userId)).thenReturn(Optional.of(mockUser));
// when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.empty());

// // Act & Assert
// assertThrows(ResourceNotFoundException.class, () -> {
// bookingService.lockSeats(request);
// });
// }

// /**
// * Test Case TC-7: Invalid seat IDs
// */
// @Test
// @DisplayName("TC-7: Should throw ResourceNotFoundException when seats not
// found")
// void testLockSeats_SeatsNotFound() {
// // Arrange
// LockSeatsRequest request = new LockSeatsRequest(showtimeId, userId,
// Arrays.asList(seatId1, seatId2));

// when(seatLockRepo.findAllActiveLocksForUser(userId))
// .thenReturn(Arrays.asList());
// when(userRepo.findById(userId)).thenReturn(Optional.of(mockUser));
// when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(mockShowtime));
// when(showtimeSeatRepo.findByIdsAndShowtime(anyList(), eq(showtimeId)))
// .thenReturn(Arrays.asList(mockSeat1)); // Only 1 seat returned instead of 2

// // Act & Assert
// assertThrows(ResourceNotFoundException.class, () -> {
// bookingService.lockSeats(request);
// });
// }

// /**
// * Test Case TC-8: Seats already locked/booked by others
// */
// @Test
// @DisplayName("TC-8: Should throw SeatLockedException when seats are already
// locked")
// void testLockSeats_SeatsAlreadyLocked() {
// // Arrange
// mockSeat1.setStatus(SeatStatus.LOCKED);
// LockSeatsRequest request = new LockSeatsRequest(showtimeId, userId,
// Arrays.asList(seatId1, seatId2));

// when(seatLockRepo.findAllActiveLocksForUser(userId))
// .thenReturn(Arrays.asList()); // No existing locks
// when(userRepo.findById(userId)).thenReturn(Optional.of(mockUser));
// when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(mockShowtime));
// when(showtimeSeatRepo.findByIdsAndShowtime(anyList(), eq(showtimeId)))
// .thenReturn(Arrays.asList(mockSeat1, mockSeat2));

// // Act & Assert
// assertThrows(SeatLockedException.class, () -> {
// bookingService.lockSeats(request);
// });

// verify(redisLockService, never()).acquireMultipleSeatsLock(any(), any(),
// any(), anyLong());
// }

// /**
// * Test Case TC-9: Redis lock acquisition fails
// */
// @Test
// @DisplayName("TC-9: Should throw ConcurrentBookingException when Redis lock
// fails")
// void testLockSeats_RedisLockFails() {
// // Arrange
// LockSeatsRequest request = new LockSeatsRequest(showtimeId, userId,
// Arrays.asList(seatId1, seatId2));

// when(seatLockRepo.findAllActiveLocksForUser(userId))
// .thenReturn(Arrays.asList()); // No existing locks
// when(userRepo.findById(userId)).thenReturn(Optional.of(mockUser));
// when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(mockShowtime));
// when(showtimeSeatRepo.findByIdsAndShowtime(anyList(), eq(showtimeId)))
// .thenReturn(Arrays.asList(mockSeat1, mockSeat2));
// when(redisLockService.acquireMultipleSeatsLock(eq(showtimeId), anyList(),
// anyString(), anyLong()))
// .thenReturn(false); // Redis lock fails

// // Act & Assert
// assertThrows(ConcurrentBookingException.class, () -> {
// bookingService.lockSeats(request);
// });

// verify(showtimeSeatRepo, never()).updateMultipleSeatsStatus(any(), any());
// verify(seatLockRepo, never()).save(any());
// }

// /**
// * Test Case TC-10: Database error triggers rollback of Redis lock
// */
// @Test
// @DisplayName("TC-10: Should rollback Redis lock on database error")
// void testLockSeats_RollbackOnDatabaseError() {
// // Arrange
// LockSeatsRequest request = new LockSeatsRequest(showtimeId, userId,
// Arrays.asList(seatId1));

// when(seatLockRepo.findAllActiveLocksForUser(userId))
// .thenReturn(Arrays.asList());
// when(userRepo.findById(userId)).thenReturn(Optional.of(mockUser));
// when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(mockShowtime));
// when(showtimeSeatRepo.findByIdsAndShowtime(anyList(), eq(showtimeId)))
// .thenReturn(Arrays.asList(mockSeat1));
// when(redisLockService.acquireMultipleSeatsLock(eq(showtimeId), anyList(),
// anyString(), anyLong()))
// .thenReturn(true);
// // Simulate database error when updating seat status
// doThrow(new RuntimeException("Database error"))
// .when(showtimeSeatRepo).updateMultipleSeatsStatus(anyList(), any());

// // Act & Assert
// assertThrows(RuntimeException.class, () -> {
// bookingService.lockSeats(request);
// });

// // Verify rollback: Redis lock should be released
// verify(redisLockService).acquireMultipleSeatsLock(eq(showtimeId), anyList(),
// anyString(), anyLong());
// verify(redisLockService).releaseMultipleSeatsLock(eq(showtimeId), anyList(),
// anyString());
// verify(seatLockRepo, never()).save(any()); // Should not save lock due to
// error
// }
// }

// // ========================================================================
// // 2. confirmBooking() Tests
// // Cyclomatic Complexity: V(G) = 5
// // Minimum Test Cases Required: 5
// // Decision Nodes: !equals(userId), !isActive, isAfter(expiresAt)
// // ========================================================================

// @Nested
// @DisplayName("confirmBooking() - V(G)=5, Min Tests=5")
// class ConfirmBookingTests {

// /**
// * Test Case TC-1: Valid lock confirmation creates booking
// */
// @Test
// @DisplayName("TC-1: Should successfully confirm booking with valid lock")
// void testConfirmBooking_Success() {
// // Arrange
// UUID lockId = UUID.randomUUID();
// SeatLock mockLock = new SeatLock();
// mockLock.setId(lockId);
// mockLock.setUser(mockUser);
// mockLock.setShowtime(mockShowtime);
// mockLock.setLockedSeats(Arrays.asList(mockSeat1, mockSeat2));
// mockLock.setActive(true);
// mockLock.setExpiresAt(LocalDateTime.now().plusMinutes(5));
// mockLock.setLockKey("test-lock-key");

// when(seatLockRepo.findById(lockId)).thenReturn(Optional.of(mockLock));
// when(bookingRepo.save(any(Booking.class)))
// .thenAnswer(invocation -> {
// Booking booking = invocation.getArgument(0);
// booking.setId(UUID.randomUUID());
// return booking;
// });

// // Mock bookingMapper response
// BookingResponse mockResponse = BookingResponse.builder()
// .showtimeId(showtimeId)
// .movieTitle("Test Movie")
// .totalPrice(new BigDecimal("20.00"))
// .build();
// when(bookingMapper.toBookingResponse(any(Booking.class))).thenReturn(mockResponse);

// // Act
// com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest confirmRequest
// = new com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest();
// confirmRequest.setLockId(lockId);
// confirmRequest.setUserId(userId);
// var response = bookingService.confirmBooking(confirmRequest);

// // Assert
// assertNotNull(response);
// assertEquals(showtimeId, response.getShowtimeId());
// assertEquals("Test Movie", response.getMovieTitle());
// assertEquals(new BigDecimal("20.00"), response.getTotalPrice());

// verify(showtimeSeatRepo).updateMultipleSeatsStatus(anyList(),
// eq(SeatStatus.BOOKED));
// verify(bookingRepo).save(any(Booking.class));
// verify(seatLockRepo).save(argThat(lock -> !lock.isActive()));
// }

// /**
// * Test Case TC-2: Inactive lock rejected
// */
// @Test
// @DisplayName("TC-2: Should throw LockExpiredException when lock is not
// active")
// void testConfirmBooking_LockNotActive() {
// // Arrange
// UUID lockId = UUID.randomUUID();
// SeatLock inactiveLock = new SeatLock();
// inactiveLock.setId(lockId);
// inactiveLock.setUser(mockUser);
// inactiveLock.setShowtime(mockShowtime);
// inactiveLock.setActive(false); // Not active
// inactiveLock.setExpiresAt(LocalDateTime.now().plusMinutes(5));

// when(seatLockRepo.findById(lockId)).thenReturn(Optional.of(inactiveLock));

// // Act & Assert
// assertThrows(LockExpiredException.class, () -> {
// com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest confirmRequest
// = new com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest();
// confirmRequest.setLockId(lockId);
// confirmRequest.setUserId(userId);
// bookingService.confirmBooking(confirmRequest);
// });
// }

// /**
// * Test Case TC-3: Authorization check fails
// */
// @Test
// @DisplayName("TC-3: Should throw IllegalArgumentException when lock does not
// belong to user")
// void testConfirmBooking_LockDoesNotBelongToUser() {
// // Arrange
// UUID lockId = UUID.randomUUID();
// UUID differentUserId = UUID.randomUUID();
// User differentUser = new User();
// differentUser.setId(differentUserId);

// SeatLock mockLock = new SeatLock();
// mockLock.setId(lockId);
// mockLock.setUser(differentUser); // Different user
// mockLock.setShowtime(mockShowtime);
// mockLock.setActive(true);

// when(seatLockRepo.findById(lockId)).thenReturn(Optional.of(mockLock));

// // Act & Assert
// assertThrows(IllegalArgumentException.class, () -> {
// com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest confirmRequest
// = new com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest();
// confirmRequest.setLockId(lockId);
// confirmRequest.setUserId(userId);
// bookingService.confirmBooking(confirmRequest);
// });
// }

// /**
// * Test Case TC-4: Invalid lock ID
// */
// @Test
// @DisplayName("TC-4: Should throw ResourceNotFoundException when lock not
// found")
// void testConfirmBooking_LockNotFound() {
// // Arrange
// UUID lockId = UUID.randomUUID();

// when(seatLockRepo.findById(lockId)).thenReturn(Optional.empty());

// // Act & Assert
// assertThrows(ResourceNotFoundException.class, () -> {
// com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest confirmRequest
// = new com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest();
// confirmRequest.setLockId(lockId);
// confirmRequest.setUserId(userId);
// bookingService.confirmBooking(confirmRequest);
// });
// }

// /**
// * Test Case TC-5: Expired lock
// */
// @Test
// @DisplayName("TC-5: Should throw LockExpiredException when lock is expired")
// void testConfirmBooking_LockExpired() {
// // Arrange
// UUID lockId = UUID.randomUUID();
// SeatLock expiredLock = new SeatLock();
// expiredLock.setId(lockId);
// expiredLock.setUser(mockUser);
// expiredLock.setShowtime(mockShowtime);
// expiredLock.setActive(true);
// expiredLock.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // Expired

// when(seatLockRepo.findById(lockId)).thenReturn(Optional.of(expiredLock));

// // Act & Assert
// assertThrows(LockExpiredException.class, () -> {
// com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest confirmRequest
// = new com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest();
// confirmRequest.setLockId(lockId);
// confirmRequest.setUserId(userId);
// bookingService.confirmBooking(confirmRequest);
// });
// }
// }

// // ========================================================================
// // 3. handleBackButton() Tests
// // Cyclomatic Complexity: V(G) = 1
// // Minimum Test Cases Required: 1
// // Decision Nodes: None (straight-line code)
// // ========================================================================

// @Nested
// @DisplayName("handleBackButton() - V(G)=1, Min Tests=1")
// class HandleBackButtonTests {

// /**
// * Test Case TC-1: User navigates away, seats released instantly
// */
// @Test
// @DisplayName("TC-1: Should handle back button and release seats immediately")
// void testHandleBackButton_Success() {
// // Arrange
// SeatLock activeLock = new SeatLock();
// activeLock.setId(UUID.randomUUID());
// activeLock.setUser(mockUser);
// activeLock.setShowtime(mockShowtime);
// activeLock.setLockedSeats(Arrays.asList(mockSeat1, mockSeat2));
// activeLock.setActive(true);
// activeLock.setExpiresAt(LocalDateTime.now().plusMinutes(5));
// activeLock.setLockKey("test-lock-token");

// when(seatLockRepo.findActiveLockByUserAndShowtime(userId, showtimeId))
// .thenReturn(Optional.of(activeLock));

// // Act
// bookingService.handleBackButton(userId, showtimeId);

// // Assert - Seats released immediately, no grace period
// verify(showtimeSeatRepo).updateMultipleSeatsStatus(anyList(),
// eq(SeatStatus.AVAILABLE));
// verify(redisLockService).releaseMultipleSeatsLock(eq(showtimeId), anyList(),
// eq("test-lock-token"));
// verify(seatLockRepo).save(argThat(lock -> !lock.isActive()));
// }
// }

// // ========================================================================
// // 4. checkAvailability() Tests
// // Cyclomatic Complexity: V(G) = 3
// // Minimum Test Cases Required: 3
// // Decision Nodes: !isEmpty(locks), switch statement (3 cases)
// // ========================================================================

// @Nested
// @DisplayName("checkAvailability() - V(G)=3, Min Tests=3")
// class CheckAvailabilityTests {

// /**
// * Test Case TC-1: Auto-release locks for authenticated user
// */
// @Test
// @DisplayName("TC-1: Should release existing locks and return availability for
// authenticated user")
// void testCheckAvailability_AuthenticatedUserWithExistingLocks() {
// // Arrange
// SeatLock existingLock = new SeatLock();
// existingLock.setId(UUID.randomUUID());
// existingLock.setUser(mockUser);
// existingLock.setShowtime(mockShowtime);
// existingLock.setLockedSeats(Arrays.asList(mockSeat1));
// existingLock.setActive(true);
// existingLock.setLockKey("existing-lock");

// when(seatLockRepo.findAllActiveLocksForUser(userId))
// .thenReturn(Arrays.asList(existingLock));
// when(showtimeSeatRepo.findByShowtimeId(showtimeId))
// .thenReturn(Arrays.asList(mockSeat1, mockSeat2));

// // Act
// var response = bookingService.checkAvailability(showtimeId, userId);

// // Assert
// assertNotNull(response);
// verify(showtimeSeatRepo).updateMultipleSeatsStatus(anyList(),
// eq(SeatStatus.AVAILABLE));
// verify(redisLockService).releaseMultipleSeatsLock(any(), anyList(),
// eq("existing-lock"));
// }

// /**
// * Test Case TC-2: Simple query for authenticated user with no locks
// */
// @Test
// @DisplayName("TC-2: Should not release locks when user has no active locks")
// void testCheckAvailability_NoActiveLocks() {
// // Arrange
// when(seatLockRepo.findAllActiveLocksForUser(userId))
// .thenReturn(Arrays.asList());
// when(showtimeSeatRepo.findByShowtimeId(showtimeId))
// .thenReturn(Arrays.asList(mockSeat1, mockSeat2));

// // Act
// bookingService.checkAvailability(showtimeId, userId);

// // Assert - No release operations
// verify(redisLockService, never()).releaseMultipleSeatsLock(any(), any(),
// any());
// }

// /**
// * Test Case TC-3: Proper status categorization
// */
// @Test
// @DisplayName("TC-3: Should return availability with all seat statuses")
// void testCheckAvailability_AllSeatStatuses() {
// // Arrange
// ShowtimeSeat bookedSeat = new ShowtimeSeat();
// bookedSeat.setId(UUID.randomUUID());
// bookedSeat.setStatus(SeatStatus.BOOKED);

// mockSeat1.setStatus(SeatStatus.AVAILABLE);
// mockSeat2.setStatus(SeatStatus.LOCKED);

// when(seatLockRepo.findAllActiveLocksForUser(userId))
// .thenReturn(Arrays.asList());
// when(showtimeSeatRepo.findByShowtimeId(showtimeId))
// .thenReturn(Arrays.asList(mockSeat1, mockSeat2, bookedSeat));

// // Act
// var response = bookingService.checkAvailability(showtimeId, userId);

// // Assert
// assertEquals(1, response.getAvailableSeats().size());
// assertEquals(1, response.getLockedSeats().size());
// assertEquals(1, response.getBookedSeats().size());
// }
// }

// // ========================================================================
// // 5. releaseSeats() Tests
// // Cyclomatic Complexity: V(G) = 2
// // Minimum Test Cases Required: 2
// // Decision Nodes: seatLock != null
// // ========================================================================

// @Nested
// @DisplayName("releaseSeats() - V(G)=2, Min Tests=2")
// class ReleaseSeatsTests {

// /**
// * Test Case TC-1: Explicit release by user
// */
// @Test
// @DisplayName("TC-1: Should successfully release seats")
// void testReleaseSeats_Success() {
// // Arrange
// SeatLock activeLock = new SeatLock();
// activeLock.setId(UUID.randomUUID());
// activeLock.setUser(mockUser);
// activeLock.setShowtime(mockShowtime);
// activeLock.setLockedSeats(Arrays.asList(mockSeat1, mockSeat2));
// activeLock.setActive(true);
// activeLock.setLockKey("test-lock-key");

// when(seatLockRepo.findActiveLockByUserAndShowtime(userId, showtimeId))
// .thenReturn(Optional.of(activeLock));

// // Act
// bookingService.releaseSeats(userId, showtimeId);

// // Assert
// verify(showtimeSeatRepo).updateMultipleSeatsStatus(anyList(),
// eq(SeatStatus.AVAILABLE));
// verify(redisLockService).releaseMultipleSeatsLock(eq(showtimeId), anyList(),
// eq("test-lock-key"));
// verify(seatLockRepo).save(argThat(lock -> !lock.isActive()));
// }

// /**
// * Test Case TC-2: Idempotent operation, no error
// */
// @Test
// @DisplayName("TC-2: Should handle release seats when no active lock exists")
// void testReleaseSeats_NoActiveLock() {
// // Arrange
// when(seatLockRepo.findActiveLockByUserAndShowtime(userId, showtimeId))
// .thenReturn(Optional.empty());

// // Act
// bookingService.releaseSeats(userId, showtimeId);

// // Assert - No operations should be performed
// verify(showtimeSeatRepo, never()).updateMultipleSeatsStatus(any(), any());
// verify(redisLockService, never()).releaseMultipleSeatsLock(any(), any(),
// any());
// }
// }

// // ========================================================================
// // 6. cleanupExpiredLocks() Tests
// // Cyclomatic Complexity: V(G) = 2
// // Minimum Test Cases Required: 2
// // Decision Nodes: for loop iteration
// // ========================================================================

// @Nested
// @DisplayName("cleanupExpiredLocks() - V(G)=2, Min Tests=2")
// class CleanupExpiredLocksTests {

// /**
// * Test Case TC-1: Batch cleanup of multiple expired locks
// */
// @Test
// @DisplayName("TC-1: Should cleanup expired locks")
// void testCleanupExpiredLocks() {
// // Arrange
// SeatLock expiredLock1 = new SeatLock();
// expiredLock1.setId(UUID.randomUUID());
// expiredLock1.setShowtime(mockShowtime);
// expiredLock1.setLockedSeats(Arrays.asList(mockSeat1));
// expiredLock1.setActive(true);
// expiredLock1.setLockKey("lock1");

// SeatLock expiredLock2 = new SeatLock();
// expiredLock2.setId(UUID.randomUUID());
// expiredLock2.setShowtime(mockShowtime);
// expiredLock2.setLockedSeats(Arrays.asList(mockSeat2));
// expiredLock2.setActive(true);
// expiredLock2.setLockKey("lock2");

// when(seatLockRepo.findExpiredLocks(any(LocalDateTime.class)))
// .thenReturn(Arrays.asList(expiredLock1, expiredLock2));

// // Act
// bookingService.cleanupExpiredLocks();

// // Assert
// verify(showtimeSeatRepo, times(2)).updateMultipleSeatsStatus(anyList(),
// eq(SeatStatus.AVAILABLE));
// verify(seatLockRepo, times(2)).save(any(SeatLock.class));
// }

// /**
// * Test Case TC-2: No-op when nothing to clean
// */
// @Test
// @DisplayName("TC-2: Should handle cleanup when no expired locks exist")
// void testCleanupExpiredLocks_NoExpiredLocks() {
// // Arrange
// when(seatLockRepo.findExpiredLocks(any(LocalDateTime.class)))
// .thenReturn(Arrays.asList());

// // Act
// bookingService.cleanupExpiredLocks();

// // Assert - No cleanup operations should be performed
// verify(showtimeSeatRepo, never()).updateMultipleSeatsStatus(any(), any());
// verify(seatLockRepo, never()).save(any(SeatLock.class));
// }
// }

// // ========================================================================
// // 7. getUserBookings() Tests
// // Cyclomatic Complexity: V(G) = 1
// // Minimum Test Cases Required: 1
// // Decision Nodes: None (straight-line code)
// // ========================================================================

// @Nested
// @DisplayName("getUserBookings() - V(G)=1, Min Tests=1")
// class GetUserBookingsTests {

// /**
// * Test Case TC-1: Returns user's booking history
// */
// @Test
// @DisplayName("TC-1: Should return user booking history")
// void testGetUserBookings() {
// // Arrange
// Booking booking1 = new Booking();
// booking1.setId(UUID.randomUUID());
// booking1.setUser(mockUser);
// booking1.setShowtime(mockShowtime);

// // Create BookingSeat entities with the new structure
// com.api.moviebooking.models.entities.BookingSeat bookingSeat1 =
// new com.api.moviebooking.models.entities.BookingSeat();
// bookingSeat1.setBooking(booking1);
// bookingSeat1.setShowtimeSeat(mockSeat1);
// bookingSeat1.setPriceFinal(new BigDecimal("10.00"));

// com.api.moviebooking.models.entities.BookingSeat bookingSeat2 =
// new com.api.moviebooking.models.entities.BookingSeat();
// bookingSeat2.setBooking(booking1);
// bookingSeat2.setShowtimeSeat(mockSeat2);
// bookingSeat2.setPriceFinal(new BigDecimal("10.00"));

// booking1.setBookingSeats(Arrays.asList(bookingSeat1, bookingSeat2));
// booking1.setTotalPrice(new BigDecimal("20.00"));

// when(bookingRepo.findByUserId(userId))
// .thenReturn(Arrays.asList(booking1));

// // Mock bookingMapper response
// BookingResponse mockResponse = BookingResponse.builder()
// .bookingId(booking1.getId())
// .build();
// when(bookingMapper.toBookingResponse(booking1)).thenReturn(mockResponse);

// // Act
// List<BookingResponse> responses = bookingService.getUserBookings(userId);

// // Assert
// assertNotNull(responses);
// assertEquals(1, responses.size());
// assertEquals(booking1.getId(), responses.get(0).getBookingId());
// verify(bookingRepo).findByUserId(userId);
// }
// }
// }
