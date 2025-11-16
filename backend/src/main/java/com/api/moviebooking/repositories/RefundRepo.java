package com.api.moviebooking.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.moviebooking.models.entities.Payment;
import com.api.moviebooking.models.entities.Refund;

public interface RefundRepo extends JpaRepository<Refund, UUID> {

    List<Refund> findByPayment(Payment payment);
}
