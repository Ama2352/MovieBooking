package com.api.moviebooking.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.models.entities.Booking;
import com.api.moviebooking.models.entities.Payment;
import com.api.moviebooking.models.entities.Refund;
import com.api.moviebooking.models.entities.ShowtimeSeat;
import com.api.moviebooking.models.enums.BookingStatus;
import com.api.moviebooking.models.enums.PaymentStatus;
import com.api.moviebooking.models.enums.SeatStatus;
import com.api.moviebooking.repositories.BookingRepo;
import com.api.moviebooking.repositories.PaymentRepo;
import com.api.moviebooking.repositories.ShowtimeSeatRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CheckoutLifecycleService {

    private final BookingRepo bookingRepo;
    private final PaymentRepo paymentRepo;
    private final ShowtimeSeatRepo showtimeSeatRepo;
    private final UserService userService;
    private final RefundService refundService;

    @Value("${currency.default:VND}")
    private String baseCurrency;

    public CheckoutLifecycleService(
            BookingRepo bookingRepo,
            PaymentRepo paymentRepo,
            ShowtimeSeatRepo showtimeSeatRepo,
            UserService userService,
            @Lazy RefundService refundService) {
        this.bookingRepo = bookingRepo;
        this.paymentRepo = paymentRepo;
        this.showtimeSeatRepo = showtimeSeatRepo;
        this.userService = userService;
        this.refundService = refundService;
    }

    @Transactional
    public Payment handleSuccessfulPayment(Payment payment, BigDecimal gatewayAmount, String gatewayTxnId) {
        Booking booking = payment.getBooking();

        if (payment.getStatus() == PaymentStatus.SUCCESS && booking.getStatus() == BookingStatus.CONFIRMED) {
            log.debug("Payment {} already processed successfully", payment.getId());
            return payment;
        }

        // Check if booking expired while user was completing payment
        if (booking.getStatus() == BookingStatus.EXPIRED) {
            log.warn("Payment {} arrived after booking {} expired. Attempting seat re-acquisition.",
                    payment.getId(), booking.getId());
            return handleLatePayment(payment, gatewayAmount, gatewayTxnId);
        }

        BigDecimal expectedGatewayAmount = resolveGatewayAmount(payment);
        if (gatewayAmount != null && expectedGatewayAmount != null
                && expectedGatewayAmount.compareTo(gatewayAmount) != 0) {
            String currency = resolveGatewayCurrency(payment);
            log.error("Gateway amount mismatch for payment {}. Expected {} {}, got {} {}", payment.getId(),
                    expectedGatewayAmount, currency, gatewayAmount, currency);
            return handleFailedPayment(payment, "Gateway amount mismatch");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setCompletedAt(LocalDateTime.now());
        if (gatewayTxnId != null) {
            payment.setTransactionId(gatewayTxnId);
        }
        paymentRepo.save(payment);

        boolean bookingChanged = false;

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setQrPayload(generateQrPayload(booking));
            bookingChanged = true;
        }

        if (!booking.isLoyaltyPointsAwarded()) {
            userService.addLoyaltyPoints(booking.getUser().getId(), booking.getFinalPrice());
            booking.setLoyaltyPointsAwarded(true);
            bookingChanged = true;
        }

        if (bookingChanged) {
            bookingRepo.save(booking);
        }

        return payment;
    }

    @Transactional
    public Payment handleFailedPayment(Payment payment, String reason) {
        Booking booking = payment.getBooking();

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Attempted to mark payment {} as failed after success", payment.getId());
            return payment;
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setErrorMessage(reason);
        paymentRepo.save(payment);

        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setQrPayload(null);
            booking.setQrCode(null);
            bookingRepo.save(booking);
            releaseSeats(booking);
        }

        return payment;
    }

    @Transactional
    public void handlePaymentTimeout(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return;
        }

        log.info("Expiring booking {} due to payment timeout (paymentExpiresAt: {})",
                booking.getId(), booking.getPaymentExpiresAt());
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setQrPayload(null);
        booking.setQrCode(null);
        bookingRepo.save(booking);
        releaseSeats(booking);
    }

    /**
     * Handle late payment (payment arrives after booking expired)
     * Attempts to re-acquire seats if still available, otherwise fails payment
     */
    @Transactional
    public Payment handleLatePayment(Payment payment, BigDecimal gatewayAmount, String gatewayTxnId) {
        Booking booking = payment.getBooking();

        // Validate amount first
        BigDecimal expectedGatewayAmount = resolveGatewayAmount(payment);
        if (gatewayAmount != null && expectedGatewayAmount != null
                && expectedGatewayAmount.compareTo(gatewayAmount) != 0) {
            String currency = resolveGatewayCurrency(payment);
            log.error("Gateway amount mismatch for late payment. Booking {}, Expected {} {}, got {} {}",
                    booking.getId(), expectedGatewayAmount, currency, gatewayAmount, currency);
            return handleFailedPayment(payment, "Gateway amount mismatch - payment received after expiry");
        }

        // Check if seats are still available
        List<UUID> seatIds = booking.getBookedSeats().stream()
                .map(ShowtimeSeat::getId)
                .collect(Collectors.toList());

        List<ShowtimeSeat> seats = showtimeSeatRepo.findAllById(seatIds);
        boolean allAvailable = seats.stream()
                .allMatch(seat -> seat.getStatus() == SeatStatus.AVAILABLE);

        if (allAvailable) {
            // Re-acquire seats and confirm booking
            log.info("Re-acquiring seats for late payment. Booking {}", booking.getId());
            showtimeSeatRepo.updateMultipleSeatsStatus(seatIds, SeatStatus.BOOKED);

            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setQrPayload(generateQrPayload(booking));
            booking.setPaymentExpiresAt(null); // Clear expiry

            // Award loyalty points
            if (!booking.isLoyaltyPointsAwarded()) {
                userService.addLoyaltyPoints(booking.getUser().getId(), booking.getFinalPrice());
                booking.setLoyaltyPointsAwarded(true);
            }

            bookingRepo.save(booking);

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setCompletedAt(LocalDateTime.now());
            if (gatewayTxnId != null) {
                payment.setTransactionId(gatewayTxnId);
            }
            paymentRepo.save(payment);

            log.info("Successfully processed late payment for booking {}", booking.getId());
            return payment;
        } else {
            // Seats already taken - reject and trigger automatic refund
            log.warn("Cannot re-acquire seats for late payment. Booking {}, seats already taken", booking.getId());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(
                    "Payment received after booking expired and seats were re-booked by another user. Refund will be processed automatically.");
            paymentRepo.save(payment);

            // Trigger automatic refund
            try {
                log.info("Triggering automatic refund for late payment {}", payment.getId());
                refundService.processAutomaticRefund(payment, "Seats no longer available - booking expired");
            } catch (Exception e) {
                log.error("Automatic refund failed for payment {}. Manual intervention required.", payment.getId(), e);
                payment.setErrorMessage(
                        payment.getErrorMessage() + " Automatic refund failed - please contact support.");
                paymentRepo.save(payment);
            }

            return payment;
        }
    }

    private void releaseSeats(Booking booking) {
        List<UUID> seatIds = booking.getBookedSeats().stream()
                .map(ShowtimeSeat::getId)
                .collect(Collectors.toList());

        if (seatIds.isEmpty()) {
            return;
        }

        showtimeSeatRepo.updateMultipleSeatsStatus(seatIds, SeatStatus.AVAILABLE);
    }

    private String generateQrPayload(Booking booking) {
        String rawPayload = booking.getId() + ":" + booking.getUser().getId() + ":" + System.nanoTime();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawPayload.getBytes());
    }

    @Transactional
    public void handleRefundSuccess(Payment payment, Refund refund, String gatewayTxnId) {
        Booking booking = payment.getBooking();

        releaseSeats(booking);

        if (booking.isLoyaltyPointsAwarded()) {
            userService.revokeLoyaltyPoints(booking.getUser().getId(), booking.getFinalPrice());
            booking.setLoyaltyPointsAwarded(false);
        }

        booking.setStatus(BookingStatus.REFUNDED);
        booking.setRefunded(true);
        booking.setRefundedAt(LocalDateTime.now());
        booking.setRefundReason(refund.getReason());
        booking.setQrPayload(null);
        booking.setQrCode(null);
        bookingRepo.save(booking);

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepo.save(payment);

        refund.setRefundGatewayTxnId(gatewayTxnId);
        refund.setRefundedAt(LocalDateTime.now());
    }

    @Transactional
    public void handleRefundFailure(Payment payment, String reason) {
        Booking booking = payment.getBooking();

        payment.setStatus(PaymentStatus.REFUND_FAILED);
        payment.setErrorMessage(reason);
        paymentRepo.save(payment);

        if (booking.getStatus() == BookingStatus.REFUND_PENDING) {
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepo.save(booking);
        }
    }

    private BigDecimal resolveGatewayAmount(Payment payment) {
        if (payment.getGatewayAmount() != null) {
            return payment.getGatewayAmount();
        }
        if (payment.getCurrency() != null && payment.getCurrency().equalsIgnoreCase(baseCurrency)) {
            return payment.getAmount();
        }
        return null;
    }

    private String resolveGatewayCurrency(Payment payment) {
        if (payment.getGatewayCurrency() != null) {
            return payment.getGatewayCurrency();
        }
        if (payment.getCurrency() != null) {
            return payment.getCurrency();
        }
        return baseCurrency;
    }
}
