package com.api.moviebooking.services;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.ConcurrentBookingException;
import com.api.moviebooking.helpers.exceptions.LockExpiredException;
import com.api.moviebooking.helpers.exceptions.MaxSeatsExceededException;
import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.exceptions.SeatLockedException;
import com.api.moviebooking.models.dtos.booking.BookingResponse;
import com.api.moviebooking.models.dtos.booking.LockSeatsRequest;
import com.api.moviebooking.models.dtos.booking.LockSeatsResponse;
import com.api.moviebooking.models.dtos.booking.SeatAvailabilityResponse;
import com.api.moviebooking.models.entities.Booking;
import com.api.moviebooking.models.entities.BookingPromotion;
import com.api.moviebooking.models.entities.Promotion;
import com.api.moviebooking.models.entities.SeatLock;
import com.api.moviebooking.models.entities.Showtime;
import com.api.moviebooking.models.entities.ShowtimeSeat;
import com.api.moviebooking.models.entities.User;
import com.api.moviebooking.models.enums.BookingStatus;
import com.api.moviebooking.models.enums.SeatStatus;
import com.api.moviebooking.repositories.BookingRepo;
import com.api.moviebooking.repositories.SeatLockRepo;
import com.api.moviebooking.repositories.ShowtimeSeatRepo;
import com.api.moviebooking.repositories.ShowtimeRepo;
import com.api.moviebooking.repositories.UserRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing seat booking with distributed locking
 * Implements 10-minute seat lock with Redis-based concurrency control
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

        private final RedisLockService redisLockService;
        private final SeatLockRepo seatLockRepo;
        private final ShowtimeSeatRepo showtimeSeatRepo;
        private final ShowtimeRepo showtimeRepo;
        private final UserRepo userRepo;
        private final BookingRepo bookingRepo;
        private final PromotionService promotionService;
        private final UserService userService;

        @Value("${seat.lock.duration.minutes}")
        private int lockDurationMinutes;

        @Value("${seat.lock.max.seats.per.booking}")
        private int maxSeatsPerBooking;

        /**
         * Predicate nodes (d): 9 -> V(G) = d + 1 = 10
         * Nodes: size>max, !isEmpty(existingLocks), isPresent(sameShowtimeLock),
         * find user, find showtime,
         * size!=expected(seats), !isEmpty(unavailableSeats), !redisLocked,
         * try-catch
         */
        @Transactional
        public LockSeatsResponse lockSeats(UUID userId, LockSeatsRequest request) {
                log.info("User {} attempting to lock {} seats for showtime {}",
                                userId, request.getShowtimeSeatIds().size(), request.getShowtimeId());

                // Validate request
                if (request.getShowtimeSeatIds().size() > maxSeatsPerBooking) {
                        throw new MaxSeatsExceededException(maxSeatsPerBooking, request.getShowtimeSeatIds().size());
                }

                // Safety check: Handle existing locks
                List<SeatLock> existingLocks = seatLockRepo.findAllActiveLocksForUser(userId);

                if (!existingLocks.isEmpty()) {
                        // Check if user has lock for THIS showtime (multi-tab scenario)
                        Optional<SeatLock> sameShowtimeLock = existingLocks.stream()
                                        .filter(lock -> lock.getShowtime().getId().equals(request.getShowtimeId()))
                                        .findFirst();

                        if (sameShowtimeLock.isPresent()) {
                                // User has active lock for SAME showtime in another tab
                                // This means they went to checkout in Tab 1, then tried checkout in Tab 2
                                // We should prevent this to avoid race conditions
                                throw new ConcurrentBookingException(
                                                "You have an active booking in progress for this showtime. " +
                                                                "Please complete or cancel your current booking before starting a new one.");
                        }

                        // Release locks for DIFFERENT showtimes
                        log.warn("User {} has {} active lock(s) for other showtimes - releasing them",
                                        userId, existingLocks.size());
                        existingLocks.forEach(lock -> releaseSeatsInternal(lock, false));
                }

                // Fetch entities
                User user = userRepo.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                Showtime showtime = showtimeRepo.findById(request.getShowtimeId())
                                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id",
                                                request.getShowtimeId()));
                List<ShowtimeSeat> seats = showtimeSeatRepo.findByIdsAndShowtime(
                                request.getShowtimeSeatIds(), request.getShowtimeId());

                // Validate seats exist
                if (seats.size() != request.getShowtimeSeatIds().size()) {
                        throw new ResourceNotFoundException("One or more seats not found");
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
                List<UUID> seatIds = request.getShowtimeSeatIds();
                boolean redisLocked = redisLockService.acquireMultipleSeatsLock(
                                request.getShowtimeId(), seatIds, lockToken, ttlSeconds);

                if (!redisLocked) {
                        throw new ConcurrentBookingException(
                                        "Unable to lock seats due to concurrent booking attempt. Please try again.");
                }

                try {
                        // Update database seat status to LOCKED
                        showtimeSeatRepo.updateMultipleSeatsStatus(seatIds, SeatStatus.LOCKED);

                        // Create SeatLock record
                        SeatLock seatLock = new SeatLock();
                        seatLock.setLockKey(lockToken); // Store the actual Redis lock token
                        seatLock.setUser(user);
                        seatLock.setShowtime(showtime);
                        seatLock.setLockedSeats(seats);
                        seatLock.setExpiresAt(LocalDateTime.now().plusMinutes(lockDurationMinutes));
                        seatLock.setActive(true);

                        seatLockRepo.save(seatLock);

                        log.info("Successfully locked {} seats for user {}, lockId: {}",
                                        seats.size(), userId, seatLock.getId());

                        // Build response
                        return buildLockResponse(seatLock, seats, lockDurationMinutes);

                } catch (Exception e) {
                        // Rollback: release Redis locks
                        log.error("Error creating seat lock, rolling back", e);
                        redisLockService.releaseMultipleSeatsLock(
                                        request.getShowtimeId(), seatIds, lockToken);
                        throw e;
                }
        }

        /**
         * Handle user pressing back button - immediately releases all locked seats
         */
        @Transactional
        public void handleBackButton(UUID userId, UUID showtimeId) {
                log.info("User {} pressed back button for showtime {}", userId, showtimeId);

                SeatLock activeLock = seatLockRepo.findActiveLockByUserAndShowtime(userId, showtimeId)
                                .orElseThrow(() -> new ResourceNotFoundException("No active lock found"));

                // Simply release the seats immediately
                releaseSeatsInternal(activeLock, false);

                log.info("Released lock {} for user {} after back button press", activeLock.getId(), userId);
        }

        /**
         * Confirm booking and transition from LOCKED to BOOKED (without promotion)
         * Predicate nodes (d): 4 -> V(G) = d + 1 = 5
         * Nodes: findSeatLock, !equals(userId), !isActive, isAfter(expiresAt)
         */
        @Transactional
        public BookingResponse confirmBooking(UUID userId, UUID lockId) {
                return confirmBooking(userId, lockId, null);
        }

        /**
         * Confirm booking with optional promotion
         * Predicate nodes (d): 5 -> V(G) = d + 1 = 6
         * Nodes: findSeatLock, !equals(userId), !isActive, isAfter(expiresAt),
         * promotionCode != null
         */
        @Transactional
        public BookingResponse confirmBooking(UUID userId, UUID lockId, String promotionCode) {
                log.info("User {} confirming booking for lock {}", userId, lockId);

                // Find and validate lock
                SeatLock seatLock = seatLockRepo.findById(lockId)
                                .orElseThrow(() -> new ResourceNotFoundException("Seat lock not found"));

                if (!seatLock.getUser().getId().equals(userId)) {
                        throw new IllegalArgumentException("Lock does not belong to this user");
                }

                if (!seatLock.isActive()) {
                        throw new LockExpiredException("Lock is no longer active");
                }

                if (LocalDateTime.now().isAfter(seatLock.getExpiresAt())) {
                        releaseSeatsInternal(seatLock, false);
                        throw new LockExpiredException("Lock has expired. Please lock seats again.");
                }

                // Update seats to BOOKED
                List<UUID> seatIds = seatLock.getLockedSeats().stream()
                                .map(ShowtimeSeat::getId)
                                .collect(Collectors.toList());
                showtimeSeatRepo.updateMultipleSeatsStatus(seatIds, SeatStatus.BOOKED);

                // Calculate total price
                BigDecimal totalPrice = seatLock.getLockedSeats().stream()
                                .map(ShowtimeSeat::getPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Create booking record
                Booking booking = new Booking();
                booking.setUser(seatLock.getUser());
                booking.setShowtime(seatLock.getShowtime());
                // Create a new list to avoid shared collection references
                booking.setBookedSeats(new ArrayList<>(seatLock.getLockedSeats()));
                booking.setTotalPrice(totalPrice);
                booking.setFinalPrice(totalPrice); // Default to total price
                booking.setStatus(BookingStatus.PENDING); // Pending payment

                // Apply membership tier discount first
                applyMembershipTierDiscount(booking);

                // Apply promotion if provided (stacks with membership discount)
                if (promotionCode != null && !promotionCode.isBlank()) {
                        applyPromotionToBooking(booking, promotionCode, userId);
                }

                // QR code generation can be added here

                bookingRepo.save(booking);

                // Add loyalty points based on final price
                userService.addLoyaltyPoints(userId, booking.getFinalPrice());

                // Deactivate lock and release Redis
                seatLock.setActive(false);
                seatLockRepo.save(seatLock);

                // Release Redis locks
                String lockToken = seatLock.getLockKey();
                redisLockService.releaseMultipleSeatsLock(
                                seatLock.getShowtime().getId(), seatIds, lockToken);

                log.info("Booking confirmed: {} for user {}", booking.getId(), userId);

                return buildBookingResponse(booking);
        }

        /**
         * Release seats manually (user cancels or navigates away)
         * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
         * Nodes: seatLock != null
         */
        @Transactional
        public void releaseSeats(UUID userId, UUID showtimeId) {
                log.info("User {} releasing seats for showtime {}", userId, showtimeId);

                SeatLock seatLock = seatLockRepo.findActiveLockByUserAndShowtime(userId, showtimeId)
                                .orElse(null);

                if (seatLock != null) {
                        releaseSeatsInternal(seatLock, false);
                }
        }

        /**
         * Check seat availability for a showtime
         * User must be authenticated - releases any existing locks for fresh selection
         * Predicate nodes (d): 4 -> V(G) = d + 1 = 5
         * Nodes: !isEmpty(locks), switch(3 cases)
         */
        @Transactional
        public SeatAvailabilityResponse checkAvailability(UUID showtimeId, UUID userId) {
                // Check for and release ANY existing locks for authenticated user
                List<SeatLock> existingLocks = seatLockRepo.findAllActiveLocksForUser(userId);

                if (!existingLocks.isEmpty()) {
                        log.info("User {} viewing showtime {}, releasing {} existing lock(s) to allow fresh selection",
                                        userId, showtimeId, existingLocks.size());
                        existingLocks.forEach(lock -> releaseSeatsInternal(lock, false));
                }

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

                return SeatAvailabilityResponse.builder()
                                .showtimeId(showtimeId)
                                .availableSeats(available)
                                .lockedSeats(locked)
                                .bookedSeats(booked)
                                .message("Seat availability retrieved successfully")
                                .build();
        }

        /**
         * Get user's booking history
         */
        public List<BookingResponse> getUserBookings(UUID userId) {
                List<Booking> bookings = bookingRepo.findByUserId(userId);
                return bookings.stream()
                                .map(this::buildBookingResponse)
                                .collect(Collectors.toList());
        }

        // ========== Private Helper Methods ==========

        /**
         * Apply membership tier discount to booking
         * This is applied before promotion discount
         */
        private void applyMembershipTierDiscount(Booking booking) {
                User user = booking.getUser();

                // Check if user has membership tier with discount
                if (user.getMembershipTier() == null) {
                        log.warn("User {} has no membership tier assigned", user.getId());
                        return;
                }

                var membershipTier = user.getMembershipTier();

                // Check if tier has discount configured
                if (membershipTier.getDiscountType() == null || membershipTier.getDiscountValue() == null) {
                        log.debug("Membership tier {} has no discount configured", membershipTier.getName());
                        return;
                }

                // Calculate discount based on tier
                BigDecimal currentPrice = booking.getFinalPrice(); // Use finalPrice to allow stacking
                BigDecimal tierDiscount = BigDecimal.ZERO;

                if (membershipTier.getDiscountType() == com.api.moviebooking.models.enums.DiscountType.PERCENTAGE) {
                        // Percentage discount: (currentPrice * discountValue) / 100
                        tierDiscount = currentPrice.multiply(membershipTier.getDiscountValue())
                                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                } else {
                        // Fixed amount discount
                        tierDiscount = membershipTier.getDiscountValue().min(currentPrice);
                }

                BigDecimal newFinalPrice = currentPrice.subtract(tierDiscount);

                // Ensure final price is not negative
                if (newFinalPrice.compareTo(BigDecimal.ZERO) < 0) {
                        newFinalPrice = BigDecimal.ZERO;
                }

                // Update booking with tier discount
                String tierDiscountReason = "Membership Tier: " + membershipTier.getName() + "(-" + tierDiscount + ")";

                if (booking.getDiscountReason() != null && !booking.getDiscountReason().isEmpty()) {
                        // Append to existing discount reason
                        booking.setDiscountReason(tierDiscountReason + "; " + booking.getDiscountReason());
                } else {
                        booking.setDiscountReason(tierDiscountReason);
                }

                // Update discount value (cumulative if there are other discounts)
                BigDecimal totalDiscount = booking.getDiscountValue() != null
                                ? booking.getDiscountValue().add(tierDiscount)
                                : tierDiscount;
                booking.setDiscountValue(totalDiscount);
                booking.setFinalPrice(newFinalPrice);

                log.info("Applied membership tier discount for {} tier. Discount: {}, New price: {}",
                                membershipTier.getName(), tierDiscount, newFinalPrice);
        }

        /**
         * Apply promotion to booking
         * This method validates promotion and calculates discount
         * Applied after membership tier discount
         */
        private void applyPromotionToBooking(Booking booking, String promotionCode, UUID userId) {
                // Validate and get promotion
                Promotion promotion = promotionService.validateAndGetPromotion(promotionCode, userId);

                // Calculate discount based on current final price (after membership discount)
                BigDecimal currentPrice = booking.getFinalPrice();
                BigDecimal promotionDiscount = promotionService.calculateDiscount(promotion, currentPrice);
                BigDecimal newFinalPrice = currentPrice.subtract(promotionDiscount);

                // Ensure final price is not negative
                if (newFinalPrice.compareTo(BigDecimal.ZERO) < 0) {
                        newFinalPrice = BigDecimal.ZERO;
                }

                // Create and add BookingPromotion entity (intermediary)
                BookingPromotion bookingPromotion = new BookingPromotion();
                BookingPromotion.BookingPromotionId id = new BookingPromotion.BookingPromotionId(
                                booking.getId(),
                                promotion.getId());
                bookingPromotion.setId(id);
                bookingPromotion.setBooking(booking);
                bookingPromotion.setPromotion(promotion);
                // appliedAt will be auto-populated by @CreationTimestamp

                booking.getBookingPromotions().add(bookingPromotion);

                // Update discount information (append to existing)
                String promotionDiscountReason = "Promotion: " + promotion.getName() + " - " + promotion.getCode()
                                + "(-" + promotionDiscount + ")";

                if (booking.getDiscountReason() != null && !booking.getDiscountReason().isEmpty()) {
                        booking.setDiscountReason(booking.getDiscountReason() + "; " + promotionDiscountReason);
                } else {
                        booking.setDiscountReason(promotionDiscountReason);
                }

                // Update total discount value (cumulative)
                BigDecimal totalDiscount = booking.getDiscountValue() != null
                                ? booking.getDiscountValue().add(promotionDiscount)
                                : promotionDiscount;
                booking.setDiscountValue(totalDiscount);
                booking.setFinalPrice(newFinalPrice);

                log.info("Applied promotion {} to booking. Current price before promotion: {}, Promotion discount: {}, New final price: {}",
                                promotionCode, currentPrice, promotionDiscount, newFinalPrice);
        }

        private BookingResponse buildBookingResponse(Booking booking) {
                List<BookingResponse.SeatDetail> seatDetails = booking.getBookedSeats().stream()
                                .map(s -> BookingResponse.SeatDetail.builder()
                                                .rowLabel(s.getSeat().getRowLabel())
                                                .seatNumber(s.getSeat().getSeatNumber())
                                                .seatType(s.getSeat().getSeatType().toString())
                                                .price(s.getPrice())
                                                .build())
                                .collect(Collectors.toList());

                return BookingResponse.builder()
                                .bookingId(booking.getId())
                                .showtimeId(booking.getShowtime().getId())
                                .movieTitle(booking.getShowtime().getMovie().getTitle())
                                .showtimeStartTime(booking.getShowtime().getStartTime())
                                .cinemaName(booking.getShowtime().getRoom().getCinema().getName())
                                .roomName("Room " + booking.getShowtime().getRoom().getRoomNumber() +
                                                " (" + booking.getShowtime().getRoom().getRoomType() + ")")
                                .seats(seatDetails)
                                .totalPrice(booking.getTotalPrice())
                                .discountReason(booking.getDiscountReason())
                                .discountValue(booking.getDiscountValue())
                                .finalPrice(booking.getFinalPrice())
                                .status(booking.getStatus())
                                .bookedAt(booking.getBookedAt())
                                .qrCode(booking.getQrCode())
                                .message("Booking created successfully")
                                .build();
        }

        private LockSeatsResponse buildLockResponse(SeatLock seatLock, List<ShowtimeSeat> seats,
                        int lockDurationMinutes) {

                List<LockSeatsResponse.SeatInfo> seatInfos = seats.stream()
                                .map(s -> LockSeatsResponse.SeatInfo.builder()
                                                .seatId(s.getId())
                                                .rowLabel(s.getSeat().getRowLabel())
                                                .seatNumber(s.getSeat().getSeatNumber())
                                                .seatType(s.getSeat().getSeatType().toString())
                                                .price(s.getPrice())
                                                .build())
                                .collect(Collectors.toList());

                BigDecimal totalPrice = seats.stream()
                                .map(ShowtimeSeat::getPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                long remainingSeconds = Duration.between(
                                LocalDateTime.now(), seatLock.getExpiresAt()).getSeconds();

                return LockSeatsResponse.builder()
                                .lockId(seatLock.getId())
                                .lockKey(seatLock.getLockKey())
                                .showtimeId(seatLock.getShowtime().getId())
                                .lockedSeats(seatInfos)
                                .totalPrice(totalPrice)
                                .expiresAt(seatLock.getExpiresAt())
                                .remainingSeconds(remainingSeconds)
                                .message("Seats locked successfully. Please complete booking within "
                                                + lockDurationMinutes
                                                + " minutes.")
                                .build();
        }

        private void releaseSeatsInternal(SeatLock seatLock, boolean expiredCleanup) {
                try {
                        // Update seat status to AVAILABLE
                        List<UUID> seatIds = seatLock.getLockedSeats().stream()
                                        .map(ShowtimeSeat::getId)
                                        .collect(Collectors.toList());

                        showtimeSeatRepo.updateMultipleSeatsStatus(seatIds, SeatStatus.AVAILABLE);

                        // Release Redis locks
                        redisLockService.releaseMultipleSeatsLock(
                                        seatLock.getShowtime().getId(), seatIds, seatLock.getLockKey());

                        // Deactivate lock
                        seatLock.setActive(false);
                        seatLockRepo.save(seatLock);

                        log.info("Released seat lock {} (expired: {})", seatLock.getId(), expiredCleanup);
                } catch (Exception e) {
                        log.error("Error releasing seat lock {}", seatLock.getId(), e);
                }
        }

        /**
         * Cleanup expired locks (called by scheduler)
         * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
         * Nodes: for loop
         */
        @Transactional
        public void cleanupExpiredLocks() {
                List<SeatLock> expiredLocks = seatLockRepo.findExpiredLocks(LocalDateTime.now());

                log.info("Found {} expired locks to cleanup", expiredLocks.size());

                for (SeatLock lock : expiredLocks) {
                        releaseSeatsInternal(lock, true);
                }
        }

        public Booking getBookingById(UUID bookingId) {
                return bookingRepo.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        }
}
