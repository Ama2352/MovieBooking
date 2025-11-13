package com.api.moviebooking.models.dtos.payment;

// Both orderId and txnRef are temporarily used for tracking payments (transactionId)
public record InitiatePaymentResponse(String orderId, String txnRef, String paymentUrl) {
}
