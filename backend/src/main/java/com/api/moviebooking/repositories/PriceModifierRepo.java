package com.api.moviebooking.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api.moviebooking.models.entities.PriceModifier;
import com.api.moviebooking.models.enums.ConditionType;

@Repository
public interface PriceModifierRepo extends JpaRepository<PriceModifier, UUID> {

    @Query("SELECT pm FROM PriceModifier pm WHERE pm.isActive = true")
    List<PriceModifier> findAllActive();

    @Query("SELECT pm FROM PriceModifier pm WHERE pm.isActive = true AND pm.conditionType = :conditionType")
    List<PriceModifier> findByConditionType(@Param("conditionType") ConditionType conditionType);

    @Query("SELECT pm FROM PriceModifier pm WHERE pm.isActive = true AND pm.conditionType = :conditionType AND pm.conditionValue = :conditionValue")
    List<PriceModifier> findByConditionTypeAndValue(
            @Param("conditionType") ConditionType conditionType,
            @Param("conditionValue") String conditionValue);
}
