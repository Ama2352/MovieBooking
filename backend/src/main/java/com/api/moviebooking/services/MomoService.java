package com.api.moviebooking.services;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.api.moviebooking.helpers.exceptions.CustomException;
import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.mapstructs.PaymentMapper;
import com.api.moviebooking.helpers.utils.SecurityUtils;
import com.api.moviebooking.models.dtos.payment.InitiatePaymentRequest;
import com.api.moviebooking.models.dtos.payment.InitiatePaymentResponse;
import com.api.moviebooking.models.dtos.payment.IpnResponse;
import com.api.moviebooking.models.dtos.payment.PaymentResponse;
import com.api.moviebooking.models.entities.Booking;
import com.api.moviebooking.models.entities.Payment;
import com.api.moviebooking.models.enums.BookingStatus;
import com.api.moviebooking.models.enums.PaymentMethod;
import com.api.moviebooking.models.enums.PaymentStatus;
import com.api.moviebooking.repositories.PaymentRepo;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MomoService {

    @Value("${momo.partner.code}")
    private String partnerCode;

    @Value("${momo.access.key}")
    private String accessKey;

    @Value("${momo.secret.key}")
    private String secretKey;

    @Value("${momo.api.endpoint}")
    private String apiEndpoint;

    @Value("${momo.return.url}")
    private String returnUrl;

    @Value("${momo.ipn.url}")
    private String ipnUrl;

    private final PaymentRepo paymentRepo;
    private final BookingService bookingService;
    private final PaymentMapper paymentMapper;
    private final CheckoutLifecycleService checkoutLifecycleService;
    private final RestTemplate restTemplate;

    /**
     * Step 1: Create Momo payment request
     */
    @Transactional
    public InitiatePaymentResponse createOrder(InitiatePaymentRequest request) {

        // Validate booking exists and is in correct status
        Booking booking = bookingService.getBookingById(request.getBookingId());

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new CustomException("Booking must be pending payment before Momo initiation",
                    HttpStatus.BAD_REQUEST);
        }

        // Verify amount matches booking total
        if (request.getAmount().compareTo(booking.getFinalPrice()) != 0) {
            throw new CustomException("Payment amount does not match booking total", HttpStatus.BAD_REQUEST);
        }

        // Check if payment already exists for this booking
        Optional<Payment> existingPayment = paymentRepo.findByBookingIdAndMethodAndStatus(booking.getId(),
                PaymentMethod.MOMO, PaymentStatus.PENDING);

        // Create or reuse a PENDING payment
        Payment payment = existingPayment
                .orElseGet(() -> {
                    Payment newPayment = new Payment();
                    newPayment.setMethod(PaymentMethod.MOMO);
                    newPayment.setStatus(PaymentStatus.PENDING);
                    newPayment.setCurrency("VND");
                    newPayment.setAmount(request.getAmount());
                    newPayment.setBooking(booking);
                    return paymentRepo.save(newPayment);
                });

        payment.setAmount(request.getAmount());
        payment.setCurrency("VND");
        paymentRepo.save(payment);

        String orderId = payment.getId().toString();
        String requestId = orderId;
        String orderInfo = "Booking " + booking.getId();
        String amount = request.getAmount().toBigInteger().toString(); // Momo uses whole VND
        String requestType = "captureWallet";
        String extraData = ""; // Optional

        // Build raw signature string
        String rawSignature = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + ipnUrl +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + returnUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;

        // Generate signature using HMAC SHA256
        String signature = SecurityUtils.HmacSHA256sign(secretKey, rawSignature);

        // Build request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("accessKey", accessKey);
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", returnUrl);
        requestBody.put("ipnUrl", ipnUrl);
        requestBody.put("requestType", requestType);
        requestBody.put("extraData", extraData);
        requestBody.put("lang", "en");
        requestBody.put("signature", signature);

        try {
            // Call Momo API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            String apiUrl = apiEndpoint + "/create";
            ResponseEntity<JsonNode> responseEntity = restTemplate.postForEntity(apiUrl, entity, JsonNode.class);
            JsonNode response = responseEntity.getBody();

            if (response == null) {
                throw new CustomException("No response from Momo gateway", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            int resultCode = response.get("resultCode").asInt();
            if (resultCode != 0) {
                String message = response.has("message") ? response.get("message").asText() : "Unknown error";
                throw new CustomException("Momo payment creation failed: " + message, HttpStatus.BAD_REQUEST);
            }

            String payUrl = response.get("payUrl").asText();
            String deeplink = response.has("deeplink") ? response.get("deeplink").asText() : null;
            String qrCodeUrl = response.has("qrCodeUrl") ? response.get("qrCodeUrl").asText() : null;

            // Save transaction reference
            payment.setTransactionId(orderId);
            paymentRepo.save(payment);

            log.info("Momo payment created successfully for booking {}", booking.getId());

            return new InitiatePaymentResponse(payment.getId(), null, orderId, payUrl);

        } catch (Exception e) {
            log.error("Error creating Momo payment", e);
            throw new CustomException("Failed to create Momo payment: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Step 2: Process IPN (server-to-server callback from Momo)
     */
    @Transactional
    public IpnResponse processIpn(Map<String, String> allParams) {
        String receivedSignature = allParams.get("signature");
        if (receivedSignature == null) {
            return IpnResponse.invalidChecksum();
        }

        // Rebuild signature for verification
        String rawSignature = "accessKey=" + allParams.getOrDefault("accessKey", "") +
                "&amount=" + allParams.getOrDefault("amount", "") +
                "&extraData=" + allParams.getOrDefault("extraData", "") +
                "&message=" + allParams.getOrDefault("message", "") +
                "&orderId=" + allParams.getOrDefault("orderId", "") +
                "&orderInfo=" + allParams.getOrDefault("orderInfo", "") +
                "&orderType=" + allParams.getOrDefault("orderType", "") +
                "&partnerCode=" + allParams.getOrDefault("partnerCode", "") +
                "&payType=" + allParams.getOrDefault("payType", "") +
                "&requestId=" + allParams.getOrDefault("requestId", "") +
                "&responseTime=" + allParams.getOrDefault("responseTime", "") +
                "&resultCode=" + allParams.getOrDefault("resultCode", "") +
                "&transId=" + allParams.getOrDefault("transId", "");

        String calculatedSignature = SecurityUtils.HmacSHA256sign(secretKey, rawSignature);
        if (!calculatedSignature.equalsIgnoreCase(receivedSignature)) {
            return IpnResponse.invalidChecksum();
        }

        String orderId = allParams.get("orderId");
        String amountStr = allParams.get("amount");
        String resultCode = allParams.get("resultCode"); // "0" = success
        String transId = allParams.get("transId"); // Momo transaction ID

        if (orderId == null || amountStr == null) {
            return IpnResponse.orderNotFound();
        }

        Optional<Payment> optPay = paymentRepo.findByTransactionId(orderId);
        if (optPay.isEmpty()) {
            return IpnResponse.orderNotFound();
        }

        Payment payment = optPay.get();

        // Verify amount
        BigDecimal expected = payment.getAmount();
        if (!expected.toBigInteger().toString().equals(amountStr)) {
            checkoutLifecycleService.handleFailedPayment(payment, "Momo amount mismatch");
            return IpnResponse.amountInvalid();
        }

        // Idempotency: if already completed, return OK
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return IpnResponse.orderAlreadyConfirmed();
        }

        // Success check
        boolean success = "0".equals(resultCode);
        if (success) {
            checkoutLifecycleService.handleSuccessfulPayment(payment, payment.getAmount(), transId);
            return IpnResponse.ok();
        } else {
            String message = allParams.getOrDefault("message", "Payment failed");
            checkoutLifecycleService.handleFailedPayment(payment, "Momo error: " + message);
            return IpnResponse.paymentFailed();
        }
    }

    /**
     * Verify payment status (called from frontend after redirect)
     */
    public PaymentResponse verifyPayment(String transactionId) {
        Payment payment = paymentRepo.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionId", transactionId));

        return paymentMapper.toPaymentResponse(payment);
    }

    /**
     * Refund payment via Momo
     * 
     * @return Momo refund transaction ID
     */
    public String refundPayment(Payment payment, BigDecimal amount, String reason) {
        try {
            String orderId = payment.getTransactionId();
            String transId = payment.getTransactionId(); // Momo transaction ID from original payment

            if (orderId == null || orderId.isBlank()) {
                throw new CustomException("No Momo transaction ID found for this payment", HttpStatus.BAD_REQUEST);
            }

            log.info("Initiating Momo refund for payment {} (orderId: {}), amount: {}",
                    payment.getId(), orderId, amount);

            String requestId = UUID.randomUUID().toString();
            String refundAmount = amount.toBigInteger().toString();
            String description = reason != null ? reason : "Booking refund";

            // Build raw signature string for refund
            String rawSignature = "accessKey=" + accessKey +
                    "&amount=" + refundAmount +
                    "&description=" + description +
                    "&orderId=" + orderId +
                    "&partnerCode=" + partnerCode +
                    "&requestId=" + requestId +
                    "&transId=" + transId;

            String signature = SecurityUtils.HmacSHA256sign(secretKey, rawSignature);

            // Build refund request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("partnerCode", partnerCode);
            requestBody.put("orderId", orderId);
            requestBody.put("requestId", requestId);
            requestBody.put("amount", refundAmount);
            requestBody.put("transId", transId);
            requestBody.put("lang", "en");
            requestBody.put("description", description);
            requestBody.put("signature", signature);

            // Call Momo Refund API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            String refundUrl = apiEndpoint + "/refund";
            ResponseEntity<JsonNode> responseEntity = restTemplate.postForEntity(refundUrl, entity, JsonNode.class);
            JsonNode response = responseEntity.getBody();

            if (response == null) {
                throw new CustomException("No response from Momo refund API", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            int resultCode = response.get("resultCode").asInt();
            if (resultCode != 0) {
                String message = response.has("message") ? response.get("message").asText() : "Unknown error";
                throw new CustomException("Momo refund failed: " + message, HttpStatus.BAD_REQUEST);
            }

            String refundTransId = response.has("transId") ? response.get("transId").asText() : requestId;
            log.info("Momo refund completed: refundTransId={}", refundTransId);

            return refundTransId;

        } catch (Exception e) {
            log.error("Momo refund failed for payment {}", payment.getId(), e);
            throw new CustomException("Failed to process Momo refund: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
