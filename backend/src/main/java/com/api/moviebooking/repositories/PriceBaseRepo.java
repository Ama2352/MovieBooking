package com.api.moviebooking.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.api.moviebooking.models.entities.PriceBase;

@Repository
public interface PriceBaseRepo extends JpaRepository<PriceBase, UUID> {

    @Query("SELECT pb FROM PriceBase pb WHERE pb.isActive = true ORDER BY pb.createdAt DESC LIMIT 1")
    Optional<PriceBase> findActiveBasePrice();

    boolean existsByNameIgnoreCase(String name);
}
