package com.api.moviebooking.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.moviebooking.models.dtos.payment.ConfirmPaymentRequest;
import com.api.moviebooking.models.dtos.payment.InitiatePaymentRequest;
import com.api.moviebooking.models.dtos.payment.InitiatePaymentResponse;
import com.api.moviebooking.models.dtos.payment.IpnResponse;
import com.api.moviebooking.models.dtos.payment.PaymentResponse;
import com.api.moviebooking.services.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Operations")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initiate payment - Redirect to payment gateway
     */
    @PostMapping("/order")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Initiate payment", description = "Creates payment order and returns payment URL")
    public ResponseEntity<InitiatePaymentResponse> initiatePayment(@Valid @RequestBody InitiatePaymentRequest request) {
        InitiatePaymentResponse response = paymentService.createOrder(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Frontend calls this after PayPal redirects back to your returnUrl.
     */
    @PostMapping("/order/capture")
    @Operation(summary = "Confirm payment", description = "Captures a PayPal/Momo order after user approval")
    public ResponseEntity<PaymentResponse> confirmPayment(@Valid @RequestBody ConfirmPaymentRequest request) {
        var result = paymentService.confirmPayment(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Momo IPN response handler
     * callback URL: https://cinesverse.com.vn/payment/momo/ipn
     */
    @RequestMapping(value = "/momo/ipn", method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<IpnResponse> ipn(HttpServletRequest request) {
        IpnResponse result = paymentService.processMomoIpn(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Search payments", description = "Search payments with various filters")
    public ResponseEntity<List<PaymentResponse>> searchPayments(
            @RequestParam(required = false) UUID bookingId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        List<PaymentResponse> responses = paymentService.searchPayments(bookingId, userId, status,
                method, startDate, endDate);
        return ResponseEntity.ok(responses);
    }

}
