package com.api.moviebooking.services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.models.dtos.booking.BookingResponse;
import com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest;
import com.api.moviebooking.models.dtos.checkout.CheckoutPaymentRequest;
import com.api.moviebooking.models.dtos.checkout.CheckoutPaymentResponse;
import com.api.moviebooking.models.dtos.payment.InitiatePaymentRequest;
import com.api.moviebooking.models.dtos.payment.InitiatePaymentResponse;
import com.api.moviebooking.models.entities.Booking;
import com.api.moviebooking.repositories.BookingRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for handling the complete checkout flow atomically
 * Ensures that booking confirmation and payment initiation happen in a single
 * transaction
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

        private final BookingService bookingService;
        private final PaymentService paymentService;
        private final BookingRepo bookingRepo;

        @Value("${payment.timeout.minutes}")
        private int paymentTimeoutMinutes;

        /**
         * Confirm booking and initiate payment atomically (API: POST /checkout)
         * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
         * Nodes: findById
         */
        @Transactional
        public CheckoutPaymentResponse confirmBookingAndInitiatePayment(CheckoutPaymentRequest request) {
                log.info("Starting atomic checkout process for user {} with lock {}",
                                request.getUserId(), request.getLockId());

                // Step 1: Confirm booking (creates PENDING_PAYMENT booking)
                ConfirmBookingRequest confirmRequest = new ConfirmBookingRequest(
                                request.getLockId(),
                                request.getUserId(),
                                request.getPromotionCode());

                BookingResponse booking = bookingService.confirmBooking(confirmRequest);
                log.info("Booking confirmed with ID: {}", booking.getBookingId());

                // Set payment expiry to 17 minutes from now (extends beyond the 10-min lock)
                Booking bookingEntity = bookingRepo.findById(booking.getBookingId())
                                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id",
                                                booking.getBookingId()));
                bookingEntity.setPaymentExpiresAt(LocalDateTime.now().plusMinutes(paymentTimeoutMinutes));
                bookingRepo.save(bookingEntity);
                log.info("Payment expiry set to {} minutes from now for booking {}", paymentTimeoutMinutes,
                                booking.getBookingId());

                // Step 2: Initiate payment with gateway
                // If this fails, the entire transaction will rollback including the booking
                InitiatePaymentRequest initiatePaymentRequest = InitiatePaymentRequest.builder()
                                .bookingId(booking.getBookingId())
                                .paymentMethod(request.getPaymentMethod())
                                .amount(booking.getFinalPrice())
                                .build();

                InitiatePaymentResponse payment = paymentService.createOrder(initiatePaymentRequest);
                log.info("Payment initiated with ID: {}", payment.paymentId());

                // Step 3: Build response
                CheckoutPaymentResponse response = CheckoutPaymentResponse.builder()
                                .bookingId(booking.getBookingId())
                                .paymentId(payment.paymentId())
                                .paymentMethod(request.getPaymentMethod())
                                .redirectUrl(payment.paymentUrl())
                                .message("Booking pending payment. Complete payment using the provided redirect URL.")
                                .build();

                log.info("Checkout process completed successfully for booking {}", booking.getBookingId());
                return response;
        }
}
