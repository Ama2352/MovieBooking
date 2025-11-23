package com.api.moviebooking.services;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.ConcurrentBookingException;
import com.api.moviebooking.helpers.exceptions.MaxSeatsExceededException;
import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.exceptions.SeatLockedException;
import com.api.moviebooking.models.dtos.SessionContext;
import com.api.moviebooking.models.dtos.booking.BookingResponse;
import com.api.moviebooking.models.dtos.booking.LockSeatsRequest;
import com.api.moviebooking.models.dtos.booking.LockSeatsResponse;
import com.api.moviebooking.models.dtos.booking.SeatAvailabilityResponse;
import com.api.moviebooking.models.entities.Booking;
import com.api.moviebooking.models.entities.SeatLock;
import com.api.moviebooking.models.entities.SeatLockSeat;
import com.api.moviebooking.models.entities.Showtime;
import com.api.moviebooking.models.entities.ShowtimeSeat;
import com.api.moviebooking.models.entities.TicketType;
import com.api.moviebooking.models.entities.User;
import com.api.moviebooking.models.enums.SeatStatus;
import com.api.moviebooking.repositories.BookingRepo;
import com.api.moviebooking.repositories.SeatLockRepo;
import com.api.moviebooking.repositories.ShowtimeRepo;
import com.api.moviebooking.repositories.ShowtimeSeatRepo;
import com.api.moviebooking.repositories.TicketTypeRepo;
import com.api.moviebooking.repositories.UserRepo;
import com.api.moviebooking.helpers.mapstructs.BookingMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

        private final SeatLockRepo seatLockRepo;
        private final ShowtimeRepo showtimeRepo;
        private final ShowtimeSeatRepo showtimeSeatRepo;
        private final TicketTypeRepo ticketTypeRepo;
        private final UserRepo userRepo;
        private final RedisLockService redisLockService;
        private final PriceCalculationService priceCalculationService;
        private final TicketTypeService ticketTypeService;
        private final BookingRepo bookingRepo;
        private final BookingMapper bookingMapper;

        @Value("${booking.lock.duration.minutes:10}")
        private Integer lockDurationMinutes;

        @Value("${booking.max.seats:10}")
        private Integer maxSeatsPerBooking;

        public Booking getBookingById(UUID bookingId) {
                return bookingRepo.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        }

        /**
         * Get all bookings for a user
         */
        @Transactional(readOnly = true)
        public List<BookingResponse> getUserBookings(UUID userId) {
                List<Booking> bookings = bookingRepo.findByUserId(userId);
                return bookings.stream()
                                .map(bookingMapper::toBookingResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Get a specific booking by ID for a user (authorization check)
         */
        @Transactional(readOnly = true)
        public BookingResponse getBookingByIdForUser(UUID bookingId, UUID userId) {
                Booking booking = bookingRepo.findByIdAndUserId(bookingId, userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Booking not found or you don't have permission to view it"));
                return bookingMapper.toBookingResponse(booking);
        }

        /**
         * Update QR code URL for a booking
         */
        @Transactional
        public BookingResponse updateQrCode(UUID bookingId, UUID userId, String qrCodeUrl) {
                Booking booking = bookingRepo.findByIdAndUserId(bookingId, userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Booking not found or you don't have permission to update it"));

                booking.setQrCode(qrCodeUrl);
                bookingRepo.save(booking);

                log.info("Updated QR code for booking {} by user {}", bookingId, userId);
                return bookingMapper.toBookingResponse(booking);
        }

        /**
         * Lock seats for a session (authenticated user or guest)
         */
        @Transactional
        public LockSeatsResponse lockSeats(LockSeatsRequest request, SessionContext session) {
                log.info("Session {} (type: {}) attempting to lock {} seats for showtime {}",
                                session.getLockOwnerId(), session.getLockOwnerType(),
                                request.getSeats().size(), request.getShowtimeId());

                // Validate request
                if (request.getSeats().size() > maxSeatsPerBooking) {
                        throw new MaxSeatsExceededException(maxSeatsPerBooking, request.getSeats().size());
                }

                // Extract seat IDs for processing
                List<UUID> showtimeSeatIds = request.getSeats().stream()
                                .map(LockSeatsRequest.SeatWithTicketType::getShowtimeSeatId)
                                .collect(Collectors.toList());

                // Safety check: Handle existing locks
                List<SeatLock> existingLocks = seatLockRepo.findAllActiveLocksForOwner(session.getLockOwnerId());
                if (!existingLocks.isEmpty()) {
                        // Check if session has lock for THIS showtime (multi-tab scenario)
                        Optional<SeatLock> sameShowtimeLock = existingLocks.stream()
                                        .filter(lock -> lock.getShowtime().getId().equals(request.getShowtimeId()))
                                        .findFirst();

                        if (sameShowtimeLock.isPresent()) {
                                // Session has active lock for SAME showtime in another tab
                                // This means they went to checkout in Tab 1, then tried checkout in Tab 2
                                // We should prevent this to avoid race conditions
                                throw new ConcurrentBookingException(
                                                "You have an active booking in progress for this showtime. " +
                                                                "Please complete or cancel your current booking before starting a new one.");
                        }

                        // Release locks for DIFFERENT showtimes
                        log.warn("Session {} has {} active lock(s) for other showtimes - releasing them",
                                        session.getLockOwnerId(), existingLocks.size());
                        existingLocks.forEach(lock -> releaseSeatsInternal(lock));
                }

                // Fetch entities
                Showtime showtime = showtimeRepo.findById(request.getShowtimeId())
                                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id",
                                                request.getShowtimeId()));

                List<ShowtimeSeat> seats = showtimeSeatRepo.findByIdsAndShowtime(
                                showtimeSeatIds, request.getShowtimeId());

                // Validate seats exist
                if (seats.size() != showtimeSeatIds.size()) {
                        throw new ResourceNotFoundException("One or more seats not found");
                }

                // Validate ticket types belong to this showtime
                List<UUID> ticketTypeIds = request.getSeats().stream()
                                .map(LockSeatsRequest.SeatWithTicketType::getTicketTypeId)
                                .distinct()
                                .collect(Collectors.toList());

                for (UUID ticketTypeId : ticketTypeIds) {
                        ticketTypeService.validateTicketTypeForShowtime(request.getShowtimeId(), ticketTypeId);
                }

                // Check for already locked/booked seats
                List<UUID> unavailableSeats = seats.stream()
                                .filter(s -> s.getStatus() != SeatStatus.AVAILABLE)
                                .map(ShowtimeSeat::getId)
                                .collect(Collectors.toList());

                if (!unavailableSeats.isEmpty()) {
                        throw new SeatLockedException(
                                        "Some seats are already locked or booked by other users",
                                        unavailableSeats);
                }

                // Generate unique lock token
                String lockToken = UUID.randomUUID().toString();
                long ttlSeconds = lockDurationMinutes * 60L;

                // Attempt distributed lock with Redis
                boolean redisLocked = redisLockService.acquireMultipleSeatsLock(
                                request.getShowtimeId(), showtimeSeatIds, lockToken, ttlSeconds);

                if (!redisLocked) {
                        throw new ConcurrentBookingException(
                                        "Unable to lock seats due to concurrent booking attempt. Please try again.");
                }

                try {
                        // Update database seat status to LOCKED
                        showtimeSeatRepo.updateMultipleSeatsStatus(showtimeSeatIds, SeatStatus.LOCKED);

                        // Create SeatLock record
                        SeatLock seatLock = new SeatLock();
                        seatLock.setLockKey(lockToken);
                        seatLock.setLockOwnerId(session.getLockOwnerId());
                        seatLock.setLockOwnerType(session.getLockOwnerType());

                        // Set user reference if authenticated
                        if (session.isAuthenticated()) {
                                User user = userRepo.findById(session.getUserId())
                                                .orElseThrow(() -> new ResourceNotFoundException("User", "id",
                                                                session.getUserId()));
                                seatLock.setUser(user);
                        } else {
                                // Guest session - user is null until checkout
                                seatLock.setUser(null);
                        }

                        seatLock.setShowtime(showtime);
                        seatLock.setExpiresAt(LocalDateTime.now().plusMinutes(lockDurationMinutes));
                        seatLock.setActive(true);

                        seatLockRepo.save(seatLock);

                        // Create SeatLockSeat entries with ticket type and calculated price
                        BigDecimal totalPrice = BigDecimal.ZERO;

                        // Create a map for quick lookup of ticket types
                        Map<UUID, UUID> seatToTicketTypeMap = request.getSeats().stream()
                                        .collect(Collectors.toMap(
                                                        LockSeatsRequest.SeatWithTicketType::getShowtimeSeatId,
                                                        LockSeatsRequest.SeatWithTicketType::getTicketTypeId));

                        for (ShowtimeSeat showtimeSeat : seats) {
                                UUID ticketTypeId = seatToTicketTypeMap.get(showtimeSeat.getId());
                                if (ticketTypeId == null) {
                                        throw new IllegalArgumentException(
                                                        "Ticket type not specified for seat: " + showtimeSeat.getId());
                                }

                                TicketType ticketType = ticketTypeRepo.findById(ticketTypeId)
                                                .orElseThrow(() -> new ResourceNotFoundException("TicketType", "id",
                                                                ticketTypeId));

                                // Calculate base price (seat + showtime modifiers only)
                                BigDecimal basePrice = priceCalculationService.calculatePrice(showtime,
                                                showtimeSeat.getSeat());

                                // Apply ticket type modifier
                                BigDecimal finalPrice = ticketTypeService.applyTicketTypeModifier(basePrice,
                                                ticketType);

                                // Create SeatLockSeat entry
                                SeatLockSeat seatLockSeat = new SeatLockSeat();
                                seatLockSeat.setSeatLock(seatLock);
                                seatLockSeat.setShowtimeSeat(showtimeSeat);
                                seatLockSeat.setTicketType(ticketType);
                                seatLockSeat.setPrice(finalPrice);

                                seatLock.getSeatLockSeats().add(seatLockSeat);
                                totalPrice = totalPrice.add(finalPrice);
                        }

                        seatLockRepo.save(seatLock);

                        log.info("Successfully locked {} seats for session {}, lockId: {}",
                                        seats.size(), session.getLockOwnerId(), seatLock.getId());

                        // Build response
                        return buildLockResponse(seatLock, totalPrice, lockDurationMinutes, session);

                } catch (Exception e) {
                        // Rollback: release Redis locks
                        log.error("Error creating seat lock, rolling back", e);
                        redisLockService.releaseMultipleSeatsLock(
                                        request.getShowtimeId(), showtimeSeatIds, lockToken);
                        throw e;
                }
        }

        /**
         * Release seats for a session
         */
        @Transactional
        public void releaseSeats(String lockOwnerId, UUID showtimeId) {
                log.info("Session {} releasing seats for showtime {}", lockOwnerId, showtimeId);

                Optional<SeatLock> seatLockOpt = seatLockRepo.findActiveLockByOwnerAndShowtime(lockOwnerId, showtimeId);

                if (seatLockOpt.isPresent()) {
                        releaseSeatsInternal(seatLockOpt.get());
                        log.info("Released lock {} for session {} and showtime {}",
                                        seatLockOpt.get().getId(), lockOwnerId, showtimeId);
                } else {
                        log.info("No active lock found for session {} and showtime {} - nothing to release",
                                        lockOwnerId, showtimeId);
                }
        }

        /**
         * Check seat availability
         * 
         * @param session optional session context to include user's locks
         */
        @Transactional(readOnly = true)
        public SeatAvailabilityResponse checkAvailability(UUID showtimeId, SessionContext session) {
                // Validate showtime
                if (!showtimeRepo.existsById(showtimeId)) {
                        throw new ResourceNotFoundException("Showtime", "id", showtimeId);
                }

                // Get all seats for showtime
                List<ShowtimeSeat> allSeats = showtimeSeatRepo.findByShowtimeId(showtimeId);

                List<UUID> available = new ArrayList<>();
                List<UUID> locked = new ArrayList<>();
                List<UUID> booked = new ArrayList<>();

                for (ShowtimeSeat seat : allSeats) {
                        switch (seat.getStatus()) {
                                case AVAILABLE -> available.add(seat.getId());
                                case LOCKED -> locked.add(seat.getId());
                                case BOOKED -> booked.add(seat.getId());
                        }
                }

                // If session provided, check for active locks
                SeatAvailabilityResponse.SessionLockInfo sessionLockInfo = null;
                if (session != null) {
                        Optional<SeatLock> activeLock = seatLockRepo.findActiveLockByOwnerAndShowtime(
                                        session.getLockOwnerId(), showtimeId);

                        if (activeLock.isPresent()) {
                                SeatLock lock = activeLock.get();
                                List<UUID> myLockedSeats = lock.getSeatLockSeats().stream()
                                                .map(sls -> sls.getShowtimeSeat().getId())
                                                .collect(Collectors.toList());

                                Duration remaining = Duration.between(LocalDateTime.now(), lock.getExpiresAt());
                                int remainingSeconds = (int) Math.max(0, remaining.getSeconds());

                                sessionLockInfo = SeatAvailabilityResponse.SessionLockInfo.builder()
                                                .lockId(lock.getId())
                                                .myLockedSeats(myLockedSeats)
                                                .remainingSeconds(remainingSeconds)
                                                .build();
                        }
                }

                return SeatAvailabilityResponse.builder()
                                .showtimeId(showtimeId)
                                .availableSeats(available)
                                .lockedSeats(locked)
                                .bookedSeats(booked)
                                .sessionLockInfo(sessionLockInfo)
                                .message("Seat availability retrieved successfully")
                                .build();
        }

        /**
         * Internal method to release seats and clean up Redis
         */
        private void releaseSeatsInternal(SeatLock seatLock) {
                List<UUID> seatIds = seatLock.getSeatLockSeats().stream()
                                .map(sls -> sls.getShowtimeSeat().getId())
                                .collect(Collectors.toList());

                // Release Redis locks
                redisLockService.releaseMultipleSeatsLock(
                                seatLock.getShowtime().getId(), seatIds, seatLock.getLockKey());

                // Update seat status to AVAILABLE
                showtimeSeatRepo.updateMultipleSeatsStatus(seatIds, SeatStatus.AVAILABLE);

                // Deactivate lock
                seatLock.setActive(false);
                seatLockRepo.save(seatLock);
        }

        private LockSeatsResponse buildLockResponse(SeatLock seatLock, BigDecimal totalPrice,
                        Integer lockDuration, SessionContext session) {

                List<LockSeatsResponse.LockedSeatInfo> lockedSeats = seatLock.getSeatLockSeats().stream()
                                .map(sls -> LockSeatsResponse.LockedSeatInfo.builder()
                                                .showtimeSeatId(sls.getShowtimeSeat().getId())
                                                .seatRow(sls.getShowtimeSeat().getSeat().getRowLabel())
                                                .seatNumber(sls.getShowtimeSeat().getSeat().getSeatNumber())
                                                .seatType(sls.getShowtimeSeat().getSeat().getSeatType().name())
                                                .ticketTypeId(sls.getTicketType().getId())
                                                .ticketTypeLabel(sls.getTicketType().getLabel())
                                                .price(sls.getPrice())
                                                .build())
                                .collect(Collectors.toList());

                return LockSeatsResponse.builder()
                                .lockId(seatLock.getId())
                                .showtimeId(seatLock.getShowtime().getId())
                                .lockOwnerId(session.getLockOwnerId())
                                .lockOwnerType(session.getLockOwnerType().name())
                                .lockedSeats(lockedSeats)
                                .totalPrice(totalPrice)
                                .expiresAt(seatLock.getExpiresAt())
                                .lockDurationMinutes(lockDuration)
                                .message("Seats locked successfully. Complete booking before expiry.")
                                .build();
        }

        /**
         * Cleanup expired seat locks (called by scheduler)
         */
        @Transactional
        public void cleanupExpiredLocks() {
                LocalDateTime now = LocalDateTime.now();
                List<SeatLock> expiredLocks = seatLockRepo.findExpiredLocks(now);

                if (!expiredLocks.isEmpty()) {
                        log.info("Cleaning up {} expired locks", expiredLocks.size());
                        for (SeatLock lock : expiredLocks) {
                                lock.setActive(false);
                                // Release seats
                                for (SeatLockSeat sls : lock.getSeatLockSeats()) {
                                        ShowtimeSeat seat = sls.getShowtimeSeat();
                                        seat.setStatus(SeatStatus.AVAILABLE);
                                        showtimeSeatRepo.save(seat);
                                }
                                seatLockRepo.save(lock);
                        }
                }
        }
}
