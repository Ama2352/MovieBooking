package com.api.moviebooking.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api.moviebooking.models.entities.MembershipTier;

@Repository
public interface MembershipTierRepo extends JpaRepository<MembershipTier, UUID> {

    Optional<MembershipTier> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    List<MembershipTier> findByIsActive(Boolean isActive);

    // Find appropriate tier based on loyalty points
    @Query("SELECT mt FROM MembershipTier mt WHERE mt.isActive = true AND mt.minPoints <= :points ORDER BY mt.minPoints DESC")
    List<MembershipTier> findEligibleTiers(@Param("points") Integer points);

    // Get default tier (lowest minPoints)
    @Query("SELECT mt FROM MembershipTier mt WHERE mt.isActive = true ORDER BY mt.minPoints ASC LIMIT 1")
    Optional<MembershipTier> findDefaultTier();
}
