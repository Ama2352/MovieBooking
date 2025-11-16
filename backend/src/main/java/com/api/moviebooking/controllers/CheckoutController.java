package com.api.moviebooking.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.models.dtos.checkout.CheckoutPaymentRequest;
import com.api.moviebooking.models.dtos.checkout.CheckoutPaymentResponse;
import com.api.moviebooking.services.CheckoutService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/checkout")
@RequiredArgsConstructor
@Tag(name = "Checkout Operations")
@SecurityRequirement(name = "bearerToken")
public class CheckoutController {

        private final CheckoutService checkoutService;

        @PostMapping
        @Operation(summary = "Confirm booking and initiate payment", description = "Atomically validates seat locks, creates a pending booking, and initiates payment. If payment initiation fails, the entire transaction is rolled back.")
        public ResponseEntity<CheckoutPaymentResponse> confirmAndInitiate(
                        @Valid @RequestBody CheckoutPaymentRequest request) {

                CheckoutPaymentResponse response = checkoutService.confirmBookingAndInitiatePayment(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
}
