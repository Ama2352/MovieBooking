package com.api.moviebooking.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.models.dtos.payment.PaymentResponse;
import com.api.moviebooking.models.dtos.payment.RefundRequest;
import com.api.moviebooking.services.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Refund Operations")
@SecurityRequirement(name = "bearerToken")
public class RefundController {

    private final PaymentService paymentService;

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refund payment", description = "Initiates full refund and releases seats")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody RefundRequest request) {
        PaymentResponse response = paymentService.refundPayment(paymentId, request.getReason());
        return ResponseEntity.ok(response);
    }
}
