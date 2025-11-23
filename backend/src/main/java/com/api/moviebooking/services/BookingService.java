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
import com.api.moviebooking.helpers.exceptions.CustomException;
import com.api.moviebooking.helpers.exceptions.LockExpiredException;
import com.api.moviebooking.helpers.exceptions.MaxSeatsExceededException;
import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.exceptions.SeatLockedException;
import com.api.moviebooking.helpers.mapstructs.BookingMapper;
import com.api.moviebooking.models.dtos.booking.BookingResponse;
import com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest;
import com.api.moviebooking.models.dtos.booking.LockSeatsRequest;
import com.api.moviebooking.models.dtos.booking.LockSeatsResponse;
import com.api.moviebooking.models.dtos.booking.SeatAvailabilityResponse;
import com.api.moviebooking.models.entities.*;
import com.api.moviebooking.models.enums.BookingStatus;
import com.api.moviebooking.models.enums.SeatStatus;
import com.api.moviebooking.models.enums.UserRole;
import com.api.moviebooking.repositories.*;

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
        private final SnackRepo snackRepo;
        private final ShowtimeSeatRepo showtimeSeatRepo;
        private final ShowtimeRepo showtimeRepo;
        private final UserRepo userRepo;
        private final BookingRepo bookingRepo;
        private final TicketTypeRepo ticketTypeRepo;
        private final PromotionService promotionService;
        private final CheckoutLifecycleService checkoutLifecycleService;
        private final PriceCalculationService priceCalculationService;
        private final TicketTypeService ticketTypeService;
        private final BookingMapper bookingMapper;

        @Value("${seat.lock.duration.minutes}")
        private int lockDurationMinutes;

        @Value("${seat.lock.max.seats.per.booking}")
        private int maxSeatsPerBooking;

        @Value("${payment.timeout.minutes}")
        private int paymentTimeoutMinutes;

        /**
         * Predicate nodes (d): 9 -> V(G) = d + 1 = 10
         * Nodes: size>max, !isEmpty(existingLocks), isPresent(sameShowtimeLock),
         * find user, find showtime,
         * size!=expected(seats), !isEmpty(unavailableSeats), !redisLocked,
         * try-catch, validate ticket types
         */
        @Transactional
        public LockSeatsResponse lockSeats(LockSeatsRequest request) {
                log.info("User {} attempting to lock {} seats for showtime {}",
                                request.getUserId(), request.getSeats().size(), request.getShowtimeId());

                // Validate request
                if (request.getSeats().size() > maxSeatsPerBooking) {
                        throw new MaxSeatsExceededException(maxSeatsPerBooking, request.getSeats().size());
                }

                // Extract seat IDs for processing
                List<UUID> showtimeSeatIds = request.getSeats().stream()
                                .map(LockSeatsRequest.SeatWithTicketType::getShowtimeSeatId)
                                .collect(Collectors.toList());

                // Safety check: Handle existing locks
                List<SeatLock> existingLocks = seatLockRepo.findAllActiveLocksForUser(request.getUserId());
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
                                        request.getUserId(), existingLocks.size());
                        existingLocks.forEach(lock -> releaseSeatsInternal(lock, false));
                }

                // Fetch entities
                User user = userRepo.findById(request.getUserId())
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
                Showtime showtime = showtimeRepo.findById(request.getShowtimeId())
                                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id",
                                                request.getShowtimeId()));
                List<ShowtimeSeat> seats = showtimeSeatRepo.findByIdsAndShowtime(
                                showtimeSeatIds, request.getShowtimeId());

                // Validate seats exist
                if (seats.size() != showtimeSeatIds.size()) {
                        throw new ResourceNotFoundException("One or more seats not found");
                }

                // Validate that all ticket types belong to this showtime
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
                        seatLock.setLockKey(lockToken); // Store the actual Redis lock token
                        seatLock.setUser(user);
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

                        log.info("Successfully locked {} seats for user {}, lockId: {}",
                                        seats.size(), request.getUserId(), seatLock.getId());

                        // Build response
                        return buildLockResponse(seatLock, totalPrice, lockDurationMinutes);

                } catch (Exception e) {
                        // Rollback: release Redis locks
                        log.error("Error creating seat lock, rolling back", e);
                        redisLockService.releaseMultipleSeatsLock(
                                        request.getShowtimeId(), showtimeSeatIds, lockToken);
                        throw e;
                }
        }

        /**
         * Handle user pressing back button (API: POST /bookings/lock/back)
         * Delegates to releaseSeats() - both actions have the same effect
         * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
         * Nodes: delegated to releaseSeats
         */
        @Transactional
        public void handleBackButton(UUID userId, UUID showtimeId) {
                releaseSeats(userId, showtimeId);
        }

        /**
         * Confirm booking with optional promotion (API: POST /bookings/confirm)
         * Predicate nodes (d): 5 -> V(G) = d + 1 = 6
         * Nodes: findSeatLock, !equals(userId), !isActive, isAfter(expiresAt),
         * promotionCode != null
         */
        @Transactional
        public BookingResponse confirmBooking(ConfirmBookingRequest request) {
                log.info("User {} confirming booking for lock {}", request.getUserId(), request.getLockId());

                // Find and validate lock
                SeatLock seatLock = seatLockRepo.findById(request.getLockId())
                                .orElseThrow(() -> new ResourceNotFoundException("Seat lock not found"));

                List<UUID> snackIds = request.getSnackCombos() == null ? List.of()
                                : request.getSnackCombos().stream()
                                                .map(ConfirmBookingRequest.SnackComboItem::getSnackId)
                                                .collect(Collectors.toList());
                List<Snack> snacks = snackRepo.findAllById(snackIds);
                if (snacks.size() != snackIds.size()) {
                        throw new ResourceNotFoundException("One or more snacks not found");
                }

                if (!seatLock.getUser().getId().equals(request.getUserId())) {
                        throw new IllegalArgumentException("Lock does not belong to this user");
                }

                if (!seatLock.isActive()) {
                        throw new LockExpiredException("Lock is no longer active");
                }

                if (LocalDateTime.now().isAfter(seatLock.getExpiresAt())) {
                        releaseSeatsInternal(seatLock, false);
                        throw new LockExpiredException("Lock has expired. Please lock seats again.");
                }

                // Get seat IDs from SeatLockSeat entries
                List<UUID> seatIds = seatLock.getSeatLockSeats().stream()
                                .map(sls -> sls.getShowtimeSeat().getId())
                                .collect(Collectors.toList());

                // Update seats to BOOKED to prevent concurrent purchases
                showtimeSeatRepo.updateMultipleSeatsStatus(seatIds, SeatStatus.BOOKED);

                // Create booking record first (before BookingSeats)
                Booking booking = new Booking();
                booking.setUser(seatLock.getUser());
                booking.setShowtime(seatLock.getShowtime());
                booking.setStatus(BookingStatus.PENDING_PAYMENT);
                booking.setPaymentExpiresAt(LocalDateTime.now().plusMinutes(paymentTimeoutMinutes));
                booking.setQrPayload(null);
                booking.setQrCode(null);
                booking.setLoyaltyPointsAwarded(false);
                booking.setRefunded(false);
                booking.setRefundReason(null);
                booking.setRefundedAt(null);

                Map<UUID, Snack> snackMap = snacks.stream().collect(
                                Collectors.toMap(Snack::getId, snack -> snack));
                List<BookingSnack> bookingSnacks = request.getSnackCombos().stream()
                                .map(item -> {
                                        Snack snack = snackMap.get(item.getSnackId());
                                        Integer quantity = item.getQuantity();

                                        BookingSnack bookingSnack = new BookingSnack();
                                        bookingSnack.setBooking(booking);
                                        bookingSnack.setSnack(snack);
                                        bookingSnack.setQuantity(quantity);
                                        return bookingSnack;
                                })
                                .collect(Collectors.toList());
                booking.getBookingSnacks().addAll(bookingSnacks);

                // Create BookingSeat entries from SeatLockSeat - copy ticket type and price
                BigDecimal totalPrice = BigDecimal.ZERO;
                for (SeatLockSeat seatLockSeat : seatLock.getSeatLockSeats()) {
                        BookingSeat bookingSeat = new BookingSeat();
                        bookingSeat.setBooking(booking);
                        bookingSeat.setShowtimeSeat(seatLockSeat.getShowtimeSeat());
                        bookingSeat.setTicketTypeApplied(seatLockSeat.getTicketType());
                        bookingSeat.setPrice(seatLockSeat.getPrice()); // Copy final price from lock

                        booking.getBookingSeats().add(bookingSeat);
                        totalPrice = totalPrice.add(seatLockSeat.getPrice());
                }

                booking.setTotalPrice(totalPrice);
                booking.setFinalPrice(totalPrice); // Default to total price

                // Apply membership tier discount first (registered users only)
                applyMembershipTierDiscount(booking);

                // Apply promotion if provided (stacks with membership discount)
                // Guest users cannot use promotions
                if (request.getPromotionCode() != null && !request.getPromotionCode().isBlank()) {
                        if (seatLock.getUser().getRole() == UserRole.GUEST) {
                                throw new IllegalArgumentException(
                                                "Guest users cannot apply promotion codes. Please register for an account to use promotions.");
                        }
                        applyPromotionToBooking(booking, request.getPromotionCode(), request.getUserId());
                }

                // QR code generation can be added here

                bookingRepo.save(booking);

                // Deactivate lock and release Redis
                seatLock.setActive(false);
                seatLockRepo.save(seatLock);

                // Release Redis locks
                String lockToken = seatLock.getLockKey();
                redisLockService.releaseMultipleSeatsLock(
                                seatLock.getShowtime().getId(), seatIds, lockToken);

                log.info("Booking pending payment: {} for user {}", booking.getId(), request.getUserId());

                return bookingMapper.toBookingResponse(booking);
        }

        /**
         * Release seats manually (user cancels, navigates away, or presses back button)
         * Used by both DELETE /bookings/lock/release and POST /bookings/lock/back
         * endpoints
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
                        log.info("Released lock {} for user {} and showtime {}", seatLock.getId(), userId, showtimeId);
                } else {
                        log.info("No active lock found for user {} and showtime {} - nothing to release", userId,
                                        showtimeId);
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

                // Only validate showtime, not userId because guest users are allowed to lock
                // seats and confirm bookings
                if (!showtimeRepo.existsById(showtimeId)) {
                        throw new ResourceNotFoundException("Showtime", "id", showtimeId);
                }

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
         * Get user's booking history (API: GET /bookings/my-bookings)
         * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
         * Nodes: none
         */
        public List<BookingResponse> getUserBookings(UUID userId) {
                List<Booking> bookings = bookingRepo.findByUserId(userId);
                return bookings.stream()
                                .map(bookingMapper::toBookingResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Get specific booking for user (API: GET /bookings/{bookingId})
         * Predicate nodes (d): 2 -> V(G) = d + 1 = 3
         * Nodes: findById, !equals(userId)
         */
        public BookingResponse getBookingForUser(UUID bookingId, UUID userId) {
                Booking booking = bookingRepo.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

                if (!booking.getUser().getId().equals(userId)) {
                        throw new CustomException("You do not have access to this booking",
                                        org.springframework.http.HttpStatus.FORBIDDEN);
                }

                return bookingMapper.toBookingResponse(booking);
        }

        /**
         * Update QR code URL for booking (API: PATCH /bookings/{bookingId}/qr)
         * Predicate nodes (d): 2 -> V(G) = d + 1 = 3
         * Nodes: findByIdAndUserId, status != CONFIRMED
         */
        @Transactional
        public BookingResponse updateQrCode(UUID bookingId, UUID userId, String qrCodeUrl) {
                Booking booking = bookingRepo.findByIdAndUserId(bookingId, userId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

                if (booking.getStatus() != BookingStatus.CONFIRMED) {
                        throw new CustomException("QR code can only be attached to confirmed bookings",
                                        org.springframework.http.HttpStatus.BAD_REQUEST);
                }

                booking.setQrCode(qrCodeUrl);
                bookingRepo.save(booking);

                return bookingMapper.toBookingResponse(booking);
        }

        // ========== Private Helper Methods ==========

        /**
         * Apply membership tier discount to booking
         * This is applied before promotion discount
         */
        private void applyMembershipTierDiscount(Booking booking) {
                User user = booking.getUser();

                // Guest users don't have membership tiers
                if (user.getRole() == UserRole.GUEST) {
                        log.debug("Guest user {} - skipping membership tier discount", user.getId());
                        return;
                }

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

        private LockSeatsResponse buildLockResponse(SeatLock seatLock, BigDecimal totalPrice,
                        int lockDurationMinutes) {

                List<LockSeatsResponse.SeatInfo> seatInfos = seatLock.getSeatLockSeats().stream()
                                .map(sls -> LockSeatsResponse.SeatInfo.builder()
                                                .seatId(sls.getShowtimeSeat().getId())
                                                .rowLabel(sls.getShowtimeSeat().getSeat().getRowLabel())
                                                .seatNumber(sls.getShowtimeSeat().getSeat().getSeatNumber())
                                                .seatType(sls.getShowtimeSeat().getSeat().getSeatType().toString())
                                                .price(sls.getPrice()) // Price with ticket type modifier applied
                                                .build())
                                .collect(Collectors.toList());

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
                // Update seat status to AVAILABLE
                List<UUID> seatIds = seatLock.getSeatLockSeats().stream()
                                .map(sls -> sls.getShowtimeSeat().getId())
                                .collect(Collectors.toList());

                showtimeSeatRepo.updateMultipleSeatsStatus(seatIds, SeatStatus.AVAILABLE);

                // Release Redis locks
                redisLockService.releaseMultipleSeatsLock(
                                seatLock.getShowtime().getId(), seatIds, seatLock.getLockKey());

                // Deactivate lock
                seatLock.setActive(false);
                seatLockRepo.save(seatLock);

                log.info("Released seat lock {} (expired: {})", seatLock.getId(), expiredCleanup);
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

        @Transactional
        public void cleanupExpiredPendingPayments() {
                List<Booking> expiredBookings = bookingRepo
                                .findByStatusAndPaymentExpiresAtBefore(BookingStatus.PENDING_PAYMENT,
                                                LocalDateTime.now());

                if (expiredBookings.isEmpty()) {
                        return;
                }

                log.info("Expiring {} pending bookings due to payment timeout", expiredBookings.size());
                expiredBookings.forEach(checkoutLifecycleService::handlePaymentTimeout);
        }

        public Booking getBookingById(UUID bookingId) {
                return bookingRepo.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        }
}
