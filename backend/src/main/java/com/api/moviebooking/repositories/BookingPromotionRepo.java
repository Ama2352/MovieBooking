package com.api.moviebooking.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api.moviebooking.models.entities.BookingPromotion;
import com.api.moviebooking.models.entities.BookingPromotion.BookingPromotionId;

@Repository
public interface BookingPromotionRepo extends JpaRepository<BookingPromotion, BookingPromotionId> {
}
