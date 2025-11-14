package com.api.moviebooking.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.api.moviebooking.models.entities.PriceBase;
import com.api.moviebooking.models.entities.PriceModifier;
import com.api.moviebooking.models.enums.ConditionType;
import com.api.moviebooking.models.enums.ModifierType;
import com.api.moviebooking.repositories.PriceBaseRepo;
import com.api.moviebooking.repositories.PriceModifierRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PricingDataInitializer {

    @Bean
    CommandLineRunner initPricingData(PriceBaseRepo priceBaseRepo, PriceModifierRepo priceModifierRepo) {
        return args -> {
            // Check if price modifiers already exist
            long modifierCount = priceModifierRepo.count();
            if (modifierCount > 0) {
                log.info("Price modifiers already exist ({} found). Skipping initialization.", modifierCount);
                return;
            }

            log.info("Initializing sample price modifiers...");

            // Ensure base price exists
            if (priceBaseRepo.count() == 0) {
                PriceBase basePrice = new PriceBase();
                basePrice.setName("Standard Base Price 2025");
                basePrice.setBasePrice(new BigDecimal("45000"));
                basePrice.setIsActive(true);
                priceBaseRepo.save(basePrice);
                log.info("Created base price: 45,000 VND");
            }

            // ============================================
            // 1. DAY_TYPE Modifiers
            // ============================================
            createModifier(priceModifierRepo, "Weekend Surcharge", 
                ConditionType.DAY_TYPE, "WEEKEND", ModifierType.PERCENTAGE, new BigDecimal("20"));

            createModifier(priceModifierRepo, "Weekday Discount", 
                ConditionType.DAY_TYPE, "WEEKDAY", ModifierType.PERCENTAGE, new BigDecimal("-10"));

            // ============================================
            // 2. TIME_RANGE Modifiers
            // ============================================
            createModifier(priceModifierRepo, "Early Bird Morning Discount", 
                ConditionType.TIME_RANGE, "MORNING", ModifierType.PERCENTAGE, new BigDecimal("-15"));

            createModifier(priceModifierRepo, "Afternoon Standard", 
                ConditionType.TIME_RANGE, "AFTERNOON", ModifierType.PERCENTAGE, new BigDecimal("0"));

            createModifier(priceModifierRepo, "Evening Prime Time Surcharge", 
                ConditionType.TIME_RANGE, "EVENING", ModifierType.FIXED_AMOUNT, new BigDecimal("10000"));

            createModifier(priceModifierRepo, "Late Night Surcharge", 
                ConditionType.TIME_RANGE, "NIGHT", ModifierType.FIXED_AMOUNT, new BigDecimal("5000"));

            // ============================================
            // 3. FORMAT Modifiers
            // ============================================
            createModifier(priceModifierRepo, "2D Standard Format", 
                ConditionType.FORMAT, "2D", ModifierType.FIXED_AMOUNT, new BigDecimal("0"));

            createModifier(priceModifierRepo, "3D Format Premium", 
                ConditionType.FORMAT, "3D", ModifierType.FIXED_AMOUNT, new BigDecimal("25000"));

            createModifier(priceModifierRepo, "IMAX Format Premium", 
                ConditionType.FORMAT, "IMAX", ModifierType.PERCENTAGE, new BigDecimal("50"));

            createModifier(priceModifierRepo, "4DX Format Premium", 
                ConditionType.FORMAT, "4DX", ModifierType.FIXED_AMOUNT, new BigDecimal("60000"));

            // ============================================
            // 4. ROOM_TYPE Modifiers
            // ============================================
            createModifier(priceModifierRepo, "Standard Room", 
                ConditionType.ROOM_TYPE, "STANDARD", ModifierType.FIXED_AMOUNT, new BigDecimal("0"));

            createModifier(priceModifierRepo, "VIP Room Premium", 
                ConditionType.ROOM_TYPE, "VIP", ModifierType.FIXED_AMOUNT, new BigDecimal("30000"));

            createModifier(priceModifierRepo, "IMAX Room Premium", 
                ConditionType.ROOM_TYPE, "IMAX", ModifierType.FIXED_AMOUNT, new BigDecimal("40000"));

            createModifier(priceModifierRepo, "Starium Room Premium", 
                ConditionType.ROOM_TYPE, "STARIUM", ModifierType.PERCENTAGE, new BigDecimal("35"));

            // ============================================
            // 5. SEAT_TYPE Modifiers
            // ============================================
            createModifier(priceModifierRepo, "Normal Seat", 
                ConditionType.SEAT_TYPE, "NORMAL", ModifierType.FIXED_AMOUNT, new BigDecimal("0"));

            createModifier(priceModifierRepo, "VIP Seat Premium", 
                ConditionType.SEAT_TYPE, "VIP", ModifierType.FIXED_AMOUNT, new BigDecimal("20000"));

            createModifier(priceModifierRepo, "Couple Seat Premium", 
                ConditionType.SEAT_TYPE, "COUPLE", ModifierType.FIXED_AMOUNT, new BigDecimal("40000"));

            log.info("Successfully initialized {} price modifiers", priceModifierRepo.count());
            log.info("Pricing examples:");
            log.info("  - Cheapest: Weekday + Morning + 2D + Standard + Normal = ~30,600 VND");
            log.info("  - Most expensive: Weekend + Evening + 4DX + VIP Room + Couple = ~180,000+ VND");
        };
    }

    private void createModifier(PriceModifierRepo repo, String name, 
            ConditionType conditionType, String conditionValue,
            ModifierType modifierType, BigDecimal modifierValue) {
        
        PriceModifier modifier = new PriceModifier();
        modifier.setName(name);
        modifier.setConditionType(conditionType);
        modifier.setConditionValue(conditionValue);
        modifier.setModifierType(modifierType);
        modifier.setModifierValue(modifierValue);
        modifier.setIsActive(true);
        repo.save(modifier);
        
        log.debug("Created modifier: {} - {}:{} = {} {}", 
            name, conditionType, conditionValue, 
            modifierType == ModifierType.PERCENTAGE ? modifierValue + "%" : modifierValue + " VND",
            modifierType);
    }
}
