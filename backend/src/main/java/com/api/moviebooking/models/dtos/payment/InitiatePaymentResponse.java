package com.api.moviebooking.models.dtos.payment;

import java.util.UUID;

// Both orderId and txnRef are temporarily used for tracking payments (transactionId)
public record InitiatePaymentResponse(UUID paymentId, String orderId, String txnRef, String paymentUrl) {
}
