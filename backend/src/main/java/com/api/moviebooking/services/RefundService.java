package com.api.moviebooking.services;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.CustomException;
import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.models.entities.Booking;
import com.api.moviebooking.models.entities.Payment;
import com.api.moviebooking.models.entities.Refund;
import com.api.moviebooking.models.enums.BookingStatus;
import com.api.moviebooking.models.enums.PaymentStatus;
import com.api.moviebooking.repositories.BookingRepo;
import com.api.moviebooking.repositories.PaymentRepo;
import com.api.moviebooking.repositories.RefundRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final PaymentRepo paymentRepo;
    private final BookingRepo bookingRepo;
    private final RefundRepo refundRepo;
    private final PayPalService payPalService;
    private final MomoService momoService;
    private final CheckoutLifecycleService checkoutLifecycleService;

    @Transactional
    public Payment processRefund(UUID paymentId, String reason) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        Booking booking = payment.getBooking();

        validateRefundEligibility(payment, booking);

        payment.setStatus(PaymentStatus.REFUND_PENDING);
        paymentRepo.save(payment);
        booking.setStatus(BookingStatus.REFUND_PENDING);
        bookingRepo.save(booking);

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(booking.getFinalPrice());
        refund.setRefundMethod(payment.getMethod().name());
        refund.setReason(reason);
        refund = refundRepo.save(refund);

        String gatewayTxnId;
        try {
            gatewayTxnId = switch (payment.getMethod()) {
                case PAYPAL -> payPalService.refundPayment(payment, refund.getAmount(), reason);
                case MOMO -> momoService.refundPayment(payment, refund.getAmount(), reason);
            };
            checkoutLifecycleService.handleRefundSuccess(payment, refund, gatewayTxnId);
            refundRepo.save(refund);
        } catch (Exception ex) {
            log.error("Refund failed for payment {}", paymentId, ex);
            checkoutLifecycleService.handleRefundFailure(payment, ex.getMessage());
            throw new CustomException("Refund failed: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return payment;
    }

    private void validateRefundEligibility(Payment payment, Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new CustomException("Only confirmed bookings can be refunded", HttpStatus.BAD_REQUEST);
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new CustomException("Only successful payments can be refunded", HttpStatus.BAD_REQUEST);
        }
        if (booking.isRefunded()) {
            throw new CustomException("Booking already refunded", HttpStatus.BAD_REQUEST);
        }
        if (payment.getMethod() == null || payment.getMethod().name().isBlank()) {
            throw new CustomException("Payment method unavailable for refund", HttpStatus.BAD_REQUEST);
        }
    }
}
