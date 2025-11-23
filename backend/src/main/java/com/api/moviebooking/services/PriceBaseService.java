package com.api.moviebooking.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.mapstructs.PriceBaseMapper;
import com.api.moviebooking.models.dtos.priceBase.AddPriceBaseRequest;
import com.api.moviebooking.models.dtos.priceBase.PriceBaseDataResponse;
import com.api.moviebooking.models.dtos.priceBase.UpdatePriceBaseRequest;
import com.api.moviebooking.models.entities.PriceBase;
import com.api.moviebooking.repositories.PriceBaseRepo;
import com.api.moviebooking.repositories.ShowtimeSeatRepo;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceBaseService {

    private final PriceBaseRepo priceBaseRepo;
    private final ShowtimeSeatRepo showtimeSeatRepo;
    private final PriceBaseMapper priceBaseMapper;
    private final EntityManager entityManager;

    private PriceBase findPriceBaseById(UUID id) {
        return priceBaseRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PriceBase", "id", id));
    }

    /**
     * Add a new base price (API: POST /price-base)
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: existsByNameIgnoreCase
     */
    @Transactional
    public PriceBaseDataResponse addPriceBase(AddPriceBaseRequest request) {
        if (priceBaseRepo.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Price base with this name already exists: " + request.getName());
        }

        PriceBase priceBase = priceBaseMapper.toEntity(request);
        priceBaseRepo.save(priceBase);
        entityManager.flush();
        entityManager.refresh(priceBase);
        return priceBaseMapper.toDataResponse(priceBase);
    }

    /**
     * Update base price (API: PUT /price-base/{id})
     * Predicate nodes (d): 3 -> V(G) = d + 1 = 4
     * Nodes: name!=null, !equalsIgnoreCase && existsByNameIgnoreCase,
     * isActive!=null
     */
    @Transactional
    public PriceBaseDataResponse updatePriceBase(UUID id, UpdatePriceBaseRequest request) {
        PriceBase priceBase = findPriceBaseById(id);

        if (request.getName() != null) {
            if (!request.getName().equalsIgnoreCase(priceBase.getName())
                    && priceBaseRepo.existsByNameIgnoreCase(request.getName())) {
                throw new IllegalArgumentException("Price base with this name already exists: " + request.getName());
            }
            priceBase.setName(request.getName());
        }

        if (request.getIsActive() != null) {
            priceBase.setIsActive(request.getIsActive());
        }

        priceBaseRepo.save(priceBase);
        entityManager.flush();
        entityManager.refresh(priceBase);
        return priceBaseMapper.toDataResponse(priceBase);
    }

    /**
     * Delete base price (API: DELETE /price-base/{id})
     * Predicate nodes (d): 2 -> V(G) = d + 1 = 3
     * Nodes: findPriceBaseById, isPriceBaseReferencedInBreakdown
     */
    @Transactional
    public void deletePriceBase(UUID id) {
        PriceBase priceBase = findPriceBaseById(id);

        // Check if THIS specific price base is being used in any showtime seat price
        // breakdown
        String basePriceStr = priceBase.getBasePrice().toString();
        if (showtimeSeatRepo.isPriceBaseReferencedInBreakdown(basePriceStr)) {
            log.info("Soft deleting price base {} ({}VND) - referenced in showtime seat breakdowns",
                    id, basePriceStr);
            priceBase.setIsActive(false);
            priceBaseRepo.save(priceBase);
        } else {
            // Not referenced, safe to hard delete
            log.info("Hard deleting price base {} ({}VND) - not referenced", id, basePriceStr);
            priceBaseRepo.delete(priceBase);
        }
    }

    /**
     * Get base price by ID (API: GET /price-base/{id})
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findPriceBaseById
     */
    public PriceBaseDataResponse getPriceBase(UUID id) {
        PriceBase priceBase = findPriceBaseById(id);
        return priceBaseMapper.toDataResponse(priceBase);
    }

    /**
     * Get all base prices (API: GET /price-base)
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     * Nodes: none
     */
    public List<PriceBaseDataResponse> getAllPriceBases() {
        return priceBaseRepo.findAll().stream()
                .map(priceBaseMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get current active base price (API: GET /price-base/active)
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findActiveBasePrice
     */
    public PriceBaseDataResponse getActiveBasePrice() {
        PriceBase priceBase = priceBaseRepo.findActiveBasePrice()
                .orElseThrow(() -> new IllegalStateException(
                        "No active base price configured. Please create at least one active price base."));
        return priceBaseMapper.toDataResponse(priceBase);
    }
}
