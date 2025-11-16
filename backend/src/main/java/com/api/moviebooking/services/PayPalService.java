package com.api.moviebooking.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.CustomException;
import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.mapstructs.PaymentMapper;
import com.api.moviebooking.models.dtos.payment.InitiatePaymentRequest;
import com.api.moviebooking.models.dtos.payment.InitiatePaymentResponse;
import com.api.moviebooking.models.dtos.payment.PaymentResponse;
import com.api.moviebooking.models.entities.Booking;
import com.api.moviebooking.models.entities.Payment;
import com.api.moviebooking.models.enums.BookingStatus;
import com.api.moviebooking.models.enums.PaymentMethod;
import com.api.moviebooking.models.enums.PaymentStatus;
import com.api.moviebooking.repositories.PaymentRepo;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.AmountWithBreakdown;
import com.paypal.orders.ApplicationContext;
import com.paypal.orders.LinkDescription;
import com.paypal.orders.Order;
import com.paypal.orders.OrderRequest;
import com.paypal.orders.OrdersCaptureRequest;
import com.paypal.orders.OrdersCreateRequest;
import com.paypal.orders.PurchaseUnitRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayPalService {

    private final PayPalHttpClient payPalHttpClient;
    private final PaymentRepo paymentRepo;
    private final BookingService bookingService;
    private final PaymentMapper paymentMapper;
    private final CheckoutLifecycleService checkoutLifecycleService;

    @Value("${paypal.return.url}")
    private String returnUrl;

    @Value("${paypal.cancel.url}")
    private String cancelUrl;

    @Transactional
    public InitiatePaymentResponse createOrder(InitiatePaymentRequest request) {
        try {
            // Validate booking exists and is in correct status
            Booking booking = bookingService.getBookingById(request.getBookingId());

            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
                throw new CustomException("Booking must be pending payment before PayPal initiation",
                        HttpStatus.BAD_REQUEST);
            }

            // Verify amount matches booking total
            if (request.getAmount().compareTo(booking.getFinalPrice()) != 0) {
                throw new CustomException("Payment amount does not match booking total", HttpStatus.BAD_REQUEST);
            }

            // Check if payment already exists for this booking
            Optional<Payment> existingPayment = paymentRepo.findByBookingIdAndMethodAndStatus(booking.getId(),
                    PaymentMethod.PAYPAL, PaymentStatus.PENDING);

            // Create or update PENDING payment record
            Payment payment = existingPayment.orElse(new Payment());
            payment.setAmount(request.getAmount());
            payment.setCurrency("USD");
            payment.setStatus(PaymentStatus.PENDING);
            payment.setMethod(PaymentMethod.PAYPAL);
            payment.setBooking(booking);
            payment = paymentRepo.save(payment);

            // Build PayPal order request
            OrderRequest orderRequest = new OrderRequest();
            orderRequest.checkoutPaymentIntent("CAPTURE");
            orderRequest.applicationContext(new ApplicationContext()
                    .brandName("MovieBookingWebsite")
                    .landingPage("NO_PREFERENCE")
                    .cancelUrl(cancelUrl)
                    .returnUrl(returnUrl));
            orderRequest.purchaseUnits(List.of(
                    new PurchaseUnitRequest()
                            .referenceId(booking.getId().toString())
                            .amountWithBreakdown(
                                    new AmountWithBreakdown()
                                            .currencyCode("USD")
                                            .value(String.format("%.2f", request.getAmount())))));

            // Execute PayPal order creation
            OrdersCreateRequest createRequest = new OrdersCreateRequest().requestBody(orderRequest);
            HttpResponse<Order> response = payPalHttpClient.execute(createRequest);
            Order order = response.result();

            // Store the PayPal order ID for later reference
            payment.setTransactionId(order.id());
            paymentRepo.save(payment);

            String approvalUrl = order.links().stream()
                    .filter(link -> "approve".equals(link.rel()))
                    .findFirst()
                    .map(LinkDescription::href)
                    .orElseThrow(() -> new RuntimeException("Approval URL not found"));

            return new InitiatePaymentResponse(payment.getId(), order.id(), null, approvalUrl);

        } catch (IOException e) {
            throw new CustomException("Failed to create PayPal order: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public PaymentResponse captureOrder(String orderId) {

        try {
            // Find payment by the PayPal order ID stored earlier
            Payment payment = paymentRepo.findByTransactionId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionId", orderId));

            if (payment.getStatus() != PaymentStatus.PENDING) {
                throw new CustomException("Payment has already been processed", HttpStatus.CONFLICT);
            }

            // Execute PayPal capture request
            OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
            HttpResponse<Order> response = payPalHttpClient.execute(request);
            Order result = response.result();

            String status = result.status();
            String transactionId = result.purchaseUnits()
                    .get(0).payments().captures().get(0).id();

            // Update payment record based on outcome
            var capture = result.purchaseUnits().get(0).payments().captures().get(0);
            BigDecimal capturedAmount = capture.amount() != null && capture.amount().value() != null
                    ? new BigDecimal(capture.amount().value())
                    : null;

            Payment updatedPayment;
            if ("COMPLETED".equalsIgnoreCase(status)) {
                updatedPayment = checkoutLifecycleService.handleSuccessfulPayment(payment, capturedAmount,
                        transactionId);
            } else {
                updatedPayment = checkoutLifecycleService.handleFailedPayment(payment,
                        "PayPal capture status: " + status);
            }

            return paymentMapper.toPaymentResponse(updatedPayment);
        } catch (IOException e) {
            throw new CustomException("Failed to capture PayPal order: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    public String refundPayment(Payment payment, BigDecimal amount, String reason) {
        // TODO: integrate with PayPal capture refund API. For now, log and return
        // synthetic txn id.
        log.info("Initiating PayPal refund for payment {} amount {}", payment.getId(), amount);
        return "PAYPAL-REF-" + UUID.randomUUID();
    }
}
