package com.api.moviebooking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.api.moviebooking.helpers.mapstructs.PromotionMapper;
import com.api.moviebooking.models.dtos.promotion.AddPromotionRequest;
import com.api.moviebooking.models.dtos.promotion.PromotionDataResponse;
import com.api.moviebooking.models.dtos.promotion.UpdatePromotionRequest;
import com.api.moviebooking.models.entities.Promotion;
import com.api.moviebooking.models.enums.DiscountType;
import com.api.moviebooking.repositories.PromotionRepo;
import com.api.moviebooking.tags.RegressionTest;
import com.api.moviebooking.tags.SanityTest;
import com.api.moviebooking.tags.SmokeTest;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionRepo promotionRepo;

    @Mock
    private PromotionMapper promotionMapper;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PromotionService promotionService;

    @Test
    @SmokeTest
    @SanityTest
    @RegressionTest
    void addPromotionPercentage_mapsSavesAndReturnsResponse() {
        AddPromotionRequest req = AddPromotionRequest.builder()
                .code("SUMMER2025")
                .description("Summer sale 50% off")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("50.00"))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(100)
                .perUserLimit(1)
                .isActive(true)
                .build();

        Promotion entity = new Promotion();
        entity.setCode("SUMMER2025");
        entity.setDescription("Summer sale 50% off");
        entity.setDiscountType(DiscountType.PERCENTAGE);
        entity.setDiscountValue(new BigDecimal("50.00"));

        PromotionDataResponse expected = PromotionDataResponse.builder()
                .code("SUMMER2025")
                .description("Summer sale 50% off")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("50.00"))
                .build();

        when(promotionRepo.existsByCodeIgnoreCase(req.getCode())).thenReturn(false);
        when(promotionMapper.toEntity(req)).thenReturn(entity);
        when(promotionMapper.toDataResponse(entity)).thenReturn(expected);

        PromotionDataResponse result = promotionService.addPromotion(req);

        verify(promotionRepo).save(entity);
        assertSame(expected, result);
    }

        @Test
    @RegressionTest
    void addPromotionFixed_mapsSavesAndReturnsResponse() {
        AddPromotionRequest req = AddPromotionRequest.builder()
                .code("SUMMER2025")
                .description("Summer sale 50$ off")
                .discountType("FIXED_AMOUNT")
                .discountValue(new BigDecimal("50.00"))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(100)
                .perUserLimit(1)
                .isActive(true)
                .build();

        Promotion entity = new Promotion();
        entity.setCode("SUMMER2025");
        entity.setDescription("Summer sale 50$ off");
        entity.setDiscountType(DiscountType.PERCENTAGE);
        entity.setDiscountValue(new BigDecimal("50.00"));

        PromotionDataResponse expected = PromotionDataResponse.builder()
                .code("SUMMER2025")
                .description("Summer sale 50$ off")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("50.00"))
                .build();

        when(promotionRepo.existsByCodeIgnoreCase(req.getCode())).thenReturn(false);
        when(promotionMapper.toEntity(req)).thenReturn(entity);
        when(promotionMapper.toDataResponse(entity)).thenReturn(expected);

        PromotionDataResponse result = promotionService.addPromotion(req);

        verify(promotionRepo).save(entity);
        assertSame(expected, result);
    }

    @Test
    @RegressionTest
    void addPromotion_throwsWhenCodeAlreadyExists() {
        AddPromotionRequest req = AddPromotionRequest.builder()
                .code("DUPLICATE")
                .description("Test")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("10.00"))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(100)
                .perUserLimit(1)
                .isActive(true)
                .build();

        when(promotionRepo.existsByCodeIgnoreCase(req.getCode())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> promotionService.addPromotion(req));
    }

    @Test
    @RegressionTest
    void addPromotion_throwsWhenEndDateBeforeStartDate() {
        AddPromotionRequest req = AddPromotionRequest.builder()
                .code("INVALID")
                .description("Test")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("10.00"))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().minusDays(1)) // End before start
                .usageLimit(100)
                .perUserLimit(1)
                .isActive(true)
                .build();

        when(promotionRepo.existsByCodeIgnoreCase(req.getCode())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> promotionService.addPromotion(req));
    }

    @Test
    @RegressionTest
    void addPromotion_throwsWhenPercentageExceeds100() {
        AddPromotionRequest req = AddPromotionRequest.builder()
                .code("INVALID")
                .description("Test")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("150.00")) // > 100%
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(100)
                .perUserLimit(1)
                .isActive(true)
                .build();

        when(promotionRepo.existsByCodeIgnoreCase(req.getCode())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> promotionService.addPromotion(req));
    }

    @Test
    @RegressionTest
    void addPromotion_throwsWhenPerUserLimitExceedsUsageLimit() {
        AddPromotionRequest req = AddPromotionRequest.builder()
                .code("INVALID")
                .description("Test")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("10.00"))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(10)
                .perUserLimit(20) // > usage limit
                .isActive(true)
                .build();

        when(promotionRepo.existsByCodeIgnoreCase(req.getCode())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> promotionService.addPromotion(req));
    }

    @Test
    @SanityTest
    @RegressionTest
    void updatePromotion_updatesNonNullFieldsAndSaves() {
        UUID id = UUID.randomUUID();
        Promotion existing = new Promotion();
        existing.setId(id);
        existing.setCode("OLD_CODE");
        existing.setDescription("Old description");
        existing.setDiscountType(DiscountType.PERCENTAGE);
        existing.setDiscountValue(new BigDecimal("10.00"));
        existing.setStartDate(LocalDateTime.now());
        existing.setEndDate(LocalDateTime.now().plusDays(30));
        existing.setUsageLimit(100);
        existing.setPerUserLimit(1);
        existing.setIsActive(true);

        UpdatePromotionRequest req = UpdatePromotionRequest.builder()
                .code("NEW_CODE")
                .description("New description")
                .discountValue(new BigDecimal("20.00"))
                .build();

        when(promotionRepo.findById(id)).thenReturn(Optional.of(existing));
        when(promotionRepo.existsByCodeIgnoreCase("NEW_CODE")).thenReturn(false);

        PromotionDataResponse mapped = PromotionDataResponse.builder()
                .code("NEW_CODE")
                .description("New description")
                .discountValue(new BigDecimal("20.00"))
                .build();
        when(promotionMapper.toDataResponse(existing)).thenReturn(mapped);

        PromotionDataResponse result = promotionService.updatePromotion(id, req);

        verify(promotionRepo).save(existing);
        assertEquals("NEW_CODE", existing.getCode());
        assertEquals("New description", existing.getDescription());
        assertEquals(new BigDecimal("20.00"), existing.getDiscountValue());

        assertSame(mapped, result);
    }

    @Test
    @RegressionTest
    void updatePromotion_throwsWhenNewCodeConflicts() {
        UUID id = UUID.randomUUID();
        Promotion existing = new Promotion();
        existing.setId(id);
        existing.setCode("OLD_CODE");

        UpdatePromotionRequest req = UpdatePromotionRequest.builder()
                .code("CONFLICTING_CODE")
                .build();

        when(promotionRepo.findById(id)).thenReturn(Optional.of(existing));
        when(promotionRepo.existsByCodeIgnoreCase("CONFLICTING_CODE")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> promotionService.updatePromotion(id, req));
    }

    @Test
    @RegressionTest
    void updatePromotion_throwsWhenPercentageExceeds100() {
        UUID id = UUID.randomUUID();
        Promotion existing = new Promotion();
        existing.setId(id);
        existing.setCode("TEST");
        existing.setDiscountType(DiscountType.PERCENTAGE);
        existing.setDiscountValue(new BigDecimal("50.00"));
        existing.setStartDate(LocalDateTime.now());
        existing.setEndDate(LocalDateTime.now().plusDays(30));
        existing.setUsageLimit(100);
        existing.setPerUserLimit(1);

        UpdatePromotionRequest req = UpdatePromotionRequest.builder()
                .discountValue(new BigDecimal("150.00")) // > 100%
                .build();

        when(promotionRepo.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> promotionService.updatePromotion(id, req));
    }

    @Test
    @RegressionTest
    void updatePromotion_throwsWhenPerUserLimitExceedsUsageLimit() {
        UUID id = UUID.randomUUID();
        Promotion existing = new Promotion();
        existing.setId(id);
        existing.setCode("TEST");
        existing.setDiscountType(DiscountType.PERCENTAGE);
        existing.setDiscountValue(new BigDecimal("50.00"));
        existing.setStartDate(LocalDateTime.now());
        existing.setEndDate(LocalDateTime.now().plusDays(30));
        existing.setUsageLimit(100);
        existing.setPerUserLimit(1);

        UpdatePromotionRequest req = UpdatePromotionRequest.builder()
                .usageLimit(10)
                .perUserLimit(20) // > usage limit
                .build();

        when(promotionRepo.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> promotionService.updatePromotion(id, req));
    }

    @Test
    @SanityTest
    @RegressionTest
    void deactivatePromotion_setsIsActiveToFalse() {
        UUID id = UUID.randomUUID();
        Promotion promotion = new Promotion();
        promotion.setId(id);
        promotion.setCode("ACTIVE");
        promotion.setIsActive(true);

        when(promotionRepo.findById(id)).thenReturn(Optional.of(promotion));

        promotionService.deactivatePromotion(id);

        assertFalse(promotion.getIsActive());
        verify(promotionRepo).save(promotion);
    }

    @Test
    @SanityTest
    @RegressionTest
    void deletePromotion_findsAndDeletes() {
        UUID id = UUID.randomUUID();
        Promotion existing = new Promotion();
        existing.setId(id);
        existing.setCode("TO_DELETE");

        when(promotionRepo.findById(id)).thenReturn(Optional.of(existing));

        promotionService.deletePromotion(id);

        verify(promotionRepo).delete(existing);
    }

    @Test
    @SmokeTest
    @SanityTest
    @RegressionTest
    void getPromotion_returnsMappedResponse() {
        UUID id = UUID.randomUUID();
        Promotion existing = new Promotion();
        existing.setId(id);
        existing.setCode("TEST");
        existing.setDescription("Test description");

        when(promotionRepo.findById(id)).thenReturn(Optional.of(existing));

        PromotionDataResponse mapped = PromotionDataResponse.builder()
                .code("TEST")
                .description("Test description")
                .build();
        when(promotionMapper.toDataResponse(existing)).thenReturn(mapped);

        PromotionDataResponse result = promotionService.getPromotion(id);
        assertSame(mapped, result);
    }

    @Test
    @SmokeTest
    @SanityTest
    @RegressionTest
    void getPromotionByCode_returnsMappedResponse() {
        Promotion existing = new Promotion();
        existing.setCode("TESTCODE");
        existing.setDescription("Test description");

        when(promotionRepo.findByCode("TESTCODE")).thenReturn(Optional.of(existing));

        PromotionDataResponse mapped = PromotionDataResponse.builder()
                .code("TESTCODE")
                .description("Test description")
                .build();
        when(promotionMapper.toDataResponse(existing)).thenReturn(mapped);

        PromotionDataResponse result = promotionService.getPromotionByCode("TESTCODE");
        assertSame(mapped, result);
    }

    @Test
    @SanityTest
    @RegressionTest
    void getAllPromotions_returnsMappedList() {
        Promotion promo1 = new Promotion();
        promo1.setCode("PROMO1");
        Promotion promo2 = new Promotion();
        promo2.setCode("PROMO2");

        when(promotionRepo.findAll()).thenReturn(List.of(promo1, promo2));

        PromotionDataResponse resp1 = PromotionDataResponse.builder()
                .code("PROMO1")
                .build();
        PromotionDataResponse resp2 = PromotionDataResponse.builder()
                .code("PROMO2")
                .build();

        when(promotionMapper.toDataResponse(promo1)).thenReturn(resp1);
        when(promotionMapper.toDataResponse(promo2)).thenReturn(resp2);

        List<PromotionDataResponse> result = promotionService.getAllPromotions();

        assertEquals(2, result.size());
        assertEquals("PROMO1", result.get(0).getCode());
        assertEquals("PROMO2", result.get(1).getCode());
    }

    @Test
    @SanityTest
    @RegressionTest
    void getActivePromotions_returnsMappedList() {
        Promotion promo = new Promotion();
        promo.setCode("ACTIVE");
        promo.setIsActive(true);

        when(promotionRepo.findByIsActive(true)).thenReturn(List.of(promo));

        PromotionDataResponse resp = PromotionDataResponse.builder()
                .code("ACTIVE")
                .build();
        when(promotionMapper.toDataResponse(promo)).thenReturn(resp);

        List<PromotionDataResponse> result = promotionService.getActivePromotions();

        assertEquals(1, result.size());
        assertEquals("ACTIVE", result.get(0).getCode());
    }

    @Test
    @SanityTest
    @RegressionTest
    void getValidPromotions_returnsMappedList() {
        Promotion promo = new Promotion();
        promo.setCode("VALID");
        promo.setIsActive(true);
        promo.setStartDate(LocalDateTime.now().minusDays(1));
        promo.setEndDate(LocalDateTime.now().plusDays(1));

        when(promotionRepo.findByIsActiveAndStartDateBeforeAndEndDateAfter(any(), any(), any()))
                .thenReturn(List.of(promo));

        PromotionDataResponse resp = PromotionDataResponse.builder()
                .code("VALID")
                .build();
        when(promotionMapper.toDataResponse(promo)).thenReturn(resp);

        List<PromotionDataResponse> result = promotionService.getValidPromotions();

        assertEquals(1, result.size());
        assertEquals("VALID", result.get(0).getCode());
    }

    @Test
    void operations_throwWhenPromotionNotFound() {
        UUID id = UUID.randomUUID();
        when(promotionRepo.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> promotionService.getPromotion(id));
        assertThrows(RuntimeException.class,
                () -> promotionService.updatePromotion(id, UpdatePromotionRequest.builder().build()));
        assertThrows(RuntimeException.class, () -> promotionService.deletePromotion(id));
        assertThrows(RuntimeException.class, () -> promotionService.deactivatePromotion(id));
    }
}
