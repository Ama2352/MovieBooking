package com.api.moviebooking.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.mapstructs.PriceModifierMapper;
import com.api.moviebooking.models.dtos.priceModifier.AddPriceModifierRequest;
import com.api.moviebooking.models.dtos.priceModifier.PriceModifierDataResponse;
import com.api.moviebooking.models.dtos.priceModifier.UpdatePriceModifierRequest;
import com.api.moviebooking.models.entities.PriceModifier;
import com.api.moviebooking.models.enums.ConditionType;
import com.api.moviebooking.models.enums.ModifierType;
import com.api.moviebooking.repositories.PriceModifierRepo;
import com.api.moviebooking.repositories.ShowtimeSeatRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceModifierService {

    private final PriceModifierRepo priceModifierRepo;
    private final ShowtimeSeatRepo showtimeSeatRepo;
    private final PriceModifierMapper priceModifierMapper;

    private PriceModifier findPriceModifierById(UUID id) {
        return priceModifierRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PriceModifier", "id", id));
    }

    @Transactional
    public PriceModifierDataResponse addPriceModifier(AddPriceModifierRequest request) {
        PriceModifier priceModifier = priceModifierMapper.toEntity(request);

        // Convert string to enums
        try {
            priceModifier.setConditionType(ConditionType.valueOf(request.getConditionType().toUpperCase()));
            priceModifier.setModifierType(ModifierType.valueOf(request.getModifierType().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid condition type or modifier type: " + e.getMessage());
        }

        // Negative values are allowed for discounts (e.g., -10 for 10% discount or -5000 for 5000 VND off)
        // Validation note: Use negative values to decrease prices, positive values to increase prices

        priceModifierRepo.save(priceModifier);
        return priceModifierMapper.toDataResponse(priceModifier);
    }

    @Transactional
    public PriceModifierDataResponse updatePriceModifier(UUID id, UpdatePriceModifierRequest request) {
        PriceModifier priceModifier = findPriceModifierById(id);

        if (request.getName() != null) {
            priceModifier.setName(request.getName());
        }

        if (request.getIsActive() != null) {
            priceModifier.setIsActive(request.getIsActive());
        }

        priceModifierRepo.save(priceModifier);
        return priceModifierMapper.toDataResponse(priceModifier);
    }

    @Transactional
    public void deletePriceModifier(UUID id) {
        PriceModifier priceModifier = findPriceModifierById(id);
        
        // Check if modifier is referenced in any showtime seat price breakdowns
        if (showtimeSeatRepo.isPriceModifierReferencedInBreakdown(priceModifier.getName())) {
            log.info("Soft deleting price modifier {} - referenced in showtime seat breakdowns", id);
            priceModifier.setIsActive(false);
            priceModifierRepo.save(priceModifier);
        } else {
            // Not referenced, safe to hard delete
            log.info("Hard deleting price modifier {} - not referenced", id);
            priceModifierRepo.delete(priceModifier);
        }
    }

    public PriceModifierDataResponse getPriceModifier(UUID id) {
        PriceModifier priceModifier = findPriceModifierById(id);
        return priceModifierMapper.toDataResponse(priceModifier);
    }

    public List<PriceModifierDataResponse> getAllPriceModifiers() {
        return priceModifierRepo.findAll().stream()
                .map(priceModifierMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    public List<PriceModifierDataResponse> getActivePriceModifiers() {
        return priceModifierRepo.findAllActive().stream()
                .map(priceModifierMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    public List<PriceModifierDataResponse> getPriceModifiersByConditionType(ConditionType conditionType) {
        return priceModifierRepo.findByConditionType(conditionType).stream()
                .map(priceModifierMapper::toDataResponse)
                .collect(Collectors.toList());
    }
}
