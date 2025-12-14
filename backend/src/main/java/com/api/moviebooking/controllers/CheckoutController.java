package com.api.moviebooking.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.helpers.utils.SessionHelper;
import com.api.moviebooking.models.dtos.SessionContext;
import com.api.moviebooking.models.dtos.checkout.CheckoutPaymentRequest;
import com.api.moviebooking.models.dtos.payment.InitiatePaymentResponse;
import com.api.moviebooking.services.CheckoutService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/checkout")
@RequiredArgsConstructor
@Tag(name = "Checkout Operations")
public class CheckoutController {

        private final CheckoutService checkoutService;
        private final SessionHelper sessionHelper;

        @PostMapping
        @Operation(summary = "Atomic booking + payment with guest support", description = """
                        Confirms booking and initiates payment in one transaction. Rolls back if payment fails.
                        Authenticated users use JWT; guests use X-Session-Id and provide guestInfo.
                        """, parameters = {
                        @Parameter(name = "X-Session-Id", description = "Guest session ID (required for guests, ignored if JWT present)", example = "550e8400-e29b-41d4-a716-446655440000", required = false, schema = @Schema(type = "string", format = "uuid"))
        })
        public ResponseEntity<InitiatePaymentResponse> confirmAndInitiate(
                        @Valid @RequestBody CheckoutPaymentRequest request,
                        HttpServletRequest httpRequest) {

                // Extract session context (userId from JWT or sessionId from header)
                SessionContext session = sessionHelper.extractSessionContext(httpRequest);

                InitiatePaymentResponse response = checkoutService.confirmBookingAndInitiatePayment(request, session);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
}
