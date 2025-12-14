package com.api.moviebooking.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.CustomException;
import com.api.moviebooking.helpers.exceptions.LockExpiredException;
import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.mapstructs.BookingMapper;
import com.api.moviebooking.models.dtos.SessionContext;
import com.api.moviebooking.models.dtos.booking.BookingResponse;
import com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest;
import com.api.moviebooking.models.dtos.booking.DiscountResult;
import com.api.moviebooking.models.dtos.checkout.CheckoutPaymentRequest;
import com.api.moviebooking.models.dtos.payment.InitiatePaymentRequest;
import com.api.moviebooking.models.dtos.payment.InitiatePaymentResponse;
import com.api.moviebooking.models.entities.Booking;
import com.api.moviebooking.models.entities.BookingSeat;
import com.api.moviebooking.models.entities.BookingSnack;
import com.api.moviebooking.models.entities.SeatLock;
import com.api.moviebooking.models.entities.SeatLockSeat;
import com.api.moviebooking.models.entities.Snack;
import com.api.moviebooking.models.entities.User;
import com.api.moviebooking.models.enums.BookingStatus;
import com.api.moviebooking.models.enums.SeatStatus;
import com.api.moviebooking.models.enums.UserRole;
import com.api.moviebooking.repositories.BookingRepo;
import com.api.moviebooking.repositories.SeatLockRepo;
import com.api.moviebooking.repositories.ShowtimeSeatRepo;
import com.api.moviebooking.repositories.SnackRepo;
import com.api.moviebooking.repositories.UserRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final SeatLockRepo seatLockRepo;
    private final BookingRepo bookingRepo;
    private final UserRepo userRepo;
    private final SnackRepo snackRepo;
    private final ShowtimeSeatRepo showtimeSeatRepo;
    private final BookingMapper bookingMapper;
    private final PaymentService paymentService;
    private final CheckoutLifecycleService checkoutLifecycleService;
    private final PriceCalculationService priceCalculationService;

    @Value("${booking.payment.timeout.minutes:15}")
    private Integer paymentTimeoutMinutes;

    /**
     * Confirm booking with guest support
     * 
     * Flow:
     * 1. Find and validate seat lock
     * 2. Check lock ownership matches session
     * 3. For guests: Create User record with role=GUEST
     * 4. For authenticated: Use existing User
     * 5. Create Booking with PENDING_PAYMENT status
     * 6. Mark seats as BOOKED
     * 7. Link seat lock to user (if was guest)
     * 
     * Predicate nodes (d): 9 -> V(G) = d + 1 = 10
     * Nodes: seatLock.isEmpty, lockOwnership, !isActive, lockExpired,
     * session.isAuthenticated,
     * guestInfo == null, snacks.size != requested, snackCombos != null (x2)
     * Minimum test cases: 10
     */
    @Transactional
    public BookingResponse confirmBooking(ConfirmBookingRequest request, SessionContext session) {
        log.info("Session {} (type: {}) confirming booking for lock {}",
                session.getLockOwnerId(), session.getLockOwnerType(), request.getLockId());

        // Find and validate lock
        SeatLock seatLock = seatLockRepo.findById(request.getLockId())
                .orElseThrow(() -> new ResourceNotFoundException("Seat lock not found"));

        // Validate lock ownership
        if (!seatLock.getLockOwnerId().equals(session.getLockOwnerId())) {
            throw new CustomException(
                    "Lock does not belong to this session",
                    HttpStatus.FORBIDDEN);
        }

        if (!seatLock.isActive()) {
            throw new LockExpiredException("Lock is no longer active");
        }

        if (LocalDateTime.now().isAfter(seatLock.getExpiresAt())) {
            throw new LockExpiredException("Lock has expired. Please lock seats again.");
        }

        // Get or create User
        User user;
        if (session.isAuthenticated()) {
            // Authenticated user - fetch from database
            user = userRepo.findById(session.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", session.getUserId()));
            log.info("Using authenticated user: {}", user.getId());
        } else {
            // Guest session - create new User record
            if (request.getGuestInfo() == null) {
                throw new CustomException(
                        "Guest information required for guest booking",
                        HttpStatus.BAD_REQUEST);
            }

            // Create guest user
            user = createGuestUser(request.getGuestInfo());
            log.info("Created guest user: {} with email: {}", user.getId(), user.getEmail());

            // Link seat lock to newly created user
            seatLock.setUser(user);
            seatLockRepo.save(seatLock);
        }

        // Validate snacks
        List<UUID> snackIds = request.getSnackCombos() == null ? List.of()
                : request.getSnackCombos().stream()
                        .map(ConfirmBookingRequest.SnackComboItem::getSnackId)
                        .collect(Collectors.toList());
        List<Snack> snacks = snackRepo.findAllById(snackIds);
        if (snacks.size() != snackIds.size()) {
            throw new ResourceNotFoundException("One or more snacks not found");
        }

        // Get seat IDs from lock
        List<UUID> seatIds = seatLock.getSeatLockSeats().stream()
                .map(sls -> sls.getShowtimeSeat().getId())
                .collect(Collectors.toList());

        // Update seats to BOOKED
        showtimeSeatRepo.updateMultipleSeatsStatus(seatIds, SeatStatus.BOOKED);

        // Create booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(seatLock.getShowtime());
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentExpiresAt(LocalDateTime.now().plusMinutes(paymentTimeoutMinutes));
        booking.setQrPayload(null);
        booking.setQrCode(null);
        booking.setLoyaltyPointsAwarded(false);
        booking.setRefunded(false);

        // Add snacks
        if (request.getSnackCombos() != null) {
            Map<UUID, Snack> snackMap = snacks.stream()
                    .collect(Collectors.toMap(Snack::getId, snack -> snack));

            List<BookingSnack> bookingSnacks = request.getSnackCombos().stream()
                    .map(item -> {
                        Snack snack = snackMap.get(item.getSnackId());
                        BookingSnack bookingSnack = new BookingSnack();
                        bookingSnack.setBooking(booking);
                        bookingSnack.setSnack(snack);
                        bookingSnack.setQuantity(item.getQuantity());
                        return bookingSnack;
                    })
                    .collect(Collectors.toList());
            booking.getBookingSnacks().addAll(bookingSnacks);
        }

        // Create booking seats from lock
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (SeatLockSeat seatLockSeat : seatLock.getSeatLockSeats()) {
            BookingSeat bookingSeat = new BookingSeat();
            bookingSeat.setBooking(booking);
            bookingSeat.setShowtimeSeat(seatLockSeat.getShowtimeSeat());
            bookingSeat.setTicketTypeApplied(seatLockSeat.getTicketType());
            bookingSeat.setPrice(seatLockSeat.getPrice());

            booking.getBookingSeats().add(bookingSeat);
            totalPrice = totalPrice.add(seatLockSeat.getPrice());
        }

        // Add snack prices
        if (request.getSnackCombos() != null) {
            for (BookingSnack bs : booking.getBookingSnacks()) {
                BigDecimal snackTotal = bs.getSnack().getPrice()
                        .multiply(BigDecimal.valueOf(bs.getQuantity()));
                totalPrice = totalPrice.add(snackTotal);
            }
        }

        booking.setTotalPrice(totalPrice);

        // Calculate discounts using shared logic
        UUID userId = session.isAuthenticated() ? session.getUserId() : null;
        DiscountResult discountResult = priceCalculationService.calculateDiscounts(
                totalPrice, userId, request.getPromotionCode());

        booking.setDiscountValue(discountResult.getTotalDiscount());
        booking.setDiscountReason(discountResult.getDiscountReason());
        booking.setFinalPrice(totalPrice.subtract(discountResult.getTotalDiscount()));

        // Save booking
        bookingRepo.save(booking);

        // Deactivate lock (seats are now booked)
        seatLock.setActive(false);
        seatLockRepo.save(seatLock);

        log.info("Booking {} created for user {} with status PENDING_PAYMENT",
                booking.getId(), user.getId());

        return bookingMapper.toBookingResponse(booking);
    }

    /**
     * Create guest user
     * Predicate nodes (d): 3 -> V(G) = d + 1 = 4
     * Nodes: existing != null, existing.role != GUEST, else (reuse existing)
     * Minimum test cases: 4
     */
    private User createGuestUser(ConfirmBookingRequest.GuestInfo guestInfo) {

        // Check if email already exists
        User existing = userRepo.findByEmail(guestInfo.getEmail()).orElse(null);
        if (existing != null) {
            if (existing.getRole() != UserRole.GUEST) {
                throw new CustomException(
                        "Email already has an account. Please login.",
                        HttpStatus.CONFLICT);
            } else {
                return existing; // Reuse existing guest user
            }
        }

        User guestUser = new User();
        guestUser.setEmail(guestInfo.getEmail());
        guestUser.setUsername(guestInfo.getUsername());
        guestUser.setPhoneNumber(guestInfo.getPhoneNumber());
        guestUser.setRole(UserRole.GUEST);
        return userRepo.save(guestUser);
    }

    /**
     * Combined checkout: confirm booking and initiate payment atomically
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: try-catch (payment initiation failure)
     * Minimum test cases: 2
     */
    @Transactional
    public InitiatePaymentResponse confirmBookingAndInitiatePayment(
            CheckoutPaymentRequest request,
            SessionContext session) {

        log.info("Starting combined checkout for session {} (type: {})",
                session.getLockOwnerId(), session.getLockOwnerType());

        // Step 1: Confirm booking (creates booking with PENDING_PAYMENT status)
        ConfirmBookingRequest confirmRequest = new ConfirmBookingRequest();
        confirmRequest.setLockId(request.getLockId());
        confirmRequest.setPromotionCode(request.getPromotionCode());
        confirmRequest.setSnackCombos(request.getSnackCombos());
        confirmRequest.setGuestInfo(request.getGuestInfo());

        BookingResponse bookingResponse = confirmBooking(confirmRequest, session);

        log.info("Booking {} created successfully, initiating payment", bookingResponse.getBookingId());

        // Step 2: Initiate payment
        InitiatePaymentRequest paymentRequest = InitiatePaymentRequest
                .builder()
                .bookingId(bookingResponse.getBookingId())
                .paymentMethod(request.getPaymentMethod())
                .amount(bookingResponse.getFinalPrice())
                .build();

        // Delegate to PaymentService for payment initiation
        InitiatePaymentResponse paymentResponse;
        try {
            paymentResponse = paymentService.createOrder(paymentRequest);
        } catch (Exception e) {
            log.error("Payment initiation failed for booking {}, transaction will be rolled back",
                    bookingResponse.getBookingId(), e);
            throw new CustomException(
                    "Payment initiation failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("Payment {} initiated successfully for booking {}",
                paymentResponse.paymentId(), bookingResponse.getBookingId());

        // Step 3: Build combined response
        return paymentResponse;
    }

    /**
     * Cleanup expired pending payments (called by scheduler)
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: expiredBookings.isEmpty
     * Minimum test cases: 2
     */
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
}
