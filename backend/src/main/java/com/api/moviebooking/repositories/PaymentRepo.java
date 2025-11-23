package com.api.moviebooking.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.moviebooking.models.entities.Payment;
import com.api.moviebooking.models.enums.PaymentMethod;
import com.api.moviebooking.models.enums.PaymentStatus;

public interface PaymentRepo extends JpaRepository<Payment, UUID> {

        Optional<Payment> findByTransactionId(String transactionId);

        Optional<Payment> findByBookingIdAndMethodAndStatus(UUID bookingId, PaymentMethod method, PaymentStatus status);
}
