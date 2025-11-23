package com.api.moviebooking.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
import com.paypal.payments.CapturesRefundRequest;
import com.paypal.payments.Money;
import com.paypal.payments.Refund;
import com.paypal.payments.RefundRequest;

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
    private final ExchangeRateService exchangeRateService;

    @Value("${paypal.return.url}")
    private String returnUrl;

    @Value("${paypal.cancel.url}")
    private String cancelUrl;

    @Value("${currency.default:VND}")
    private String baseCurrency;

    @Value("${payment.paypal.currency:USD}")
    private String paypalCurrency;

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

            ExchangeRateService.CurrencyConversion conversion = exchangeRateService
                    .convert(booking.getFinalPrice(), baseCurrency, paypalCurrency);
            BigDecimal paypalAmount = conversion.targetAmount();

            // Check if payment already exists for this booking
            Optional<Payment> existingPayment = paymentRepo.findByBookingIdAndMethodAndStatus(booking.getId(),
                    PaymentMethod.PAYPAL, PaymentStatus.PENDING);

            // Create or update PENDING payment record
            Payment payment = existingPayment.orElse(new Payment());
            payment.setAmount(conversion.sourceAmount());
            payment.setCurrency(conversion.sourceCurrency());
            payment.setGatewayAmount(paypalAmount);
            payment.setGatewayCurrency(conversion.targetCurrency());
            payment.setExchangeRate(conversion.rate());
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
                                            .currencyCode(conversion.targetCurrency())
                                            .value(String.format("%.2f", paypalAmount)))));

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
                if (capturedAmount != null) {
                    payment.setGatewayAmount(capturedAmount);
                    payment.setGatewayCurrency(paypalCurrency);
                    paymentRepo.save(payment);
                }
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

    /**
     * Refund a captured PayPal payment
     * 
     * @return PayPal refund transaction ID
     */
    public String refundPayment(Payment payment, BigDecimal amount, String reason) {
        try {
            String captureId = payment.getTransactionId();
            if (captureId == null || captureId.isBlank()) {
                throw new CustomException("No PayPal capture ID found for this payment", HttpStatus.BAD_REQUEST);
            }

            log.info("Initiating PayPal refund for payment {} (capture: {}), amount: {}",
                    payment.getId(), captureId, amount);

            // Build refund request
            RefundRequest refundRequest = new RefundRequest();
            BigDecimal gatewayRefundAmount = payment.getGatewayAmount();
            String refundCurrency = payment.getGatewayCurrency() != null ? payment.getGatewayCurrency()
                    : paypalCurrency;

            Money refundAmount = new Money()
                    .currencyCode(refundCurrency)
                    .value(String.format("%.2f", gatewayRefundAmount));
            refundRequest.amount(refundAmount);
            refundRequest.noteToPayer(reason != null ? reason : "Booking refund");

            // Execute refund via PayPal SDK
            CapturesRefundRequest request = new CapturesRefundRequest(captureId);
            request.requestBody(refundRequest);

            HttpResponse<Refund> response = payPalHttpClient.execute(request);
            Refund refund = response.result();

            String refundId = refund.id();
            String status = refund.status();

            log.info("PayPal refund completed: refundId={}, status={}", refundId, status);

            if (!"COMPLETED".equalsIgnoreCase(status)) {
                log.warn("PayPal refund not completed immediately. Status: {}", status);
            }

            return refundId;

        } catch (IOException e) {
            log.error("PayPal refund failed for payment {}", payment.getId(), e);
            throw new CustomException("Failed to process PayPal refund: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
