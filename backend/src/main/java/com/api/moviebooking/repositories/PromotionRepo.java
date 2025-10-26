package com.api.moviebooking.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api.moviebooking.models.entities.Promotion;

@Repository
public interface PromotionRepo extends JpaRepository<Promotion, UUID> {

    Optional<Promotion> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Promotion> findByIsActive(Boolean isActive);

    List<Promotion> findByStartDateBeforeAndEndDateAfter(LocalDateTime currentDate1, LocalDateTime currentDate2);

    List<Promotion> findByIsActiveAndStartDateBeforeAndEndDateAfter(
            Boolean isActive,
            LocalDateTime currentDate1,
            LocalDateTime currentDate2);
}
