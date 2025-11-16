package com.api.moviebooking.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutLifecycleService {

    private final BookingRepo bookingRepo;
    private final PaymentRepo paymentRepo;
    private final ShowtimeSeatRepo showtimeSeatRepo;
    private final UserService userService;

    @Transactional
    public Payment handleSuccessfulPayment(Payment payment, BigDecimal gatewayAmount, String gatewayTxnId) {
        Booking booking = payment.getBooking();

        if (payment.getStatus() == PaymentStatus.SUCCESS && booking.getStatus() == BookingStatus.CONFIRMED) {
            log.debug("Payment {} already processed successfully", payment.getId());
            return payment;
        }

        if (gatewayAmount != null && booking.getFinalPrice().compareTo(gatewayAmount) != 0) {
            log.error("Gateway amount mismatch for booking {}. Expected {}, got {}", booking.getId(),
                    booking.getFinalPrice(), gatewayAmount);
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

        booking.setStatus(BookingStatus.EXPIRED);
        booking.setQrPayload(null);
        booking.setQrCode(null);
        bookingRepo.save(booking);
        releaseSeats(booking);
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
}
