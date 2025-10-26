package com.api.moviebooking.integrations;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.api.moviebooking.models.dtos.promotion.AddPromotionRequest;
import com.api.moviebooking.models.dtos.promotion.UpdatePromotionRequest;
import com.api.moviebooking.models.entities.Promotion;
import com.api.moviebooking.models.enums.DiscountType;
import com.api.moviebooking.repositories.PromotionRepo;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

/**
 * Integration tests for PromotionController using Testcontainers and RestAssured.
 * Tests Promotion CRUD operations with proper security context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@Transactional
class PromotionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PromotionRepo promotionRepo;

        @Container
        @SuppressWarnings("resource")
        static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                        .withExposedPorts(6379);

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.data.redis.host", redis::getHost);
                registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        }

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build());

        // Clean up test data
        promotionRepo.deleteAll();
    }

    // ==================== Promotion CRUD Tests ====================

    @Test
    @DisplayName("Should create promotion successfully when authenticated as admin")
    @WithMockUser(roles = "ADMIN")
    void testCreatePromotion_Success() {
        AddPromotionRequest request = AddPromotionRequest.builder()
                .code("SUMMER2025")
                .description("Summer sale - 50% off on all bookings")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("50.00"))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(100)
                .perUserLimit(1)
                .isActive(true)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/promotions")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("code", equalTo("SUMMER2025"))
                .body("description", equalTo("Summer sale - 50% off on all bookings"))
                .body("discountType", equalTo("PERCENTAGE"))
                .body("discountValue", equalTo(50.00f))
                .body("usageLimit", equalTo(100))
                .body("perUserLimit", equalTo(1))
                .body("isActive", equalTo(true))
                .body("id", notNullValue());
    }

    @Test
    @DisplayName("Should fail to create promotion when not authenticated")
    void testCreatePromotion_Unauthorized() {
        AddPromotionRequest request = AddPromotionRequest.builder()
                .code("TEST")
                .description("Test promotion")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("10.00"))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(50)
                .perUserLimit(1)
                .isActive(true)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/promotions")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("Should fail to create promotion with invalid data")
    @WithMockUser(roles = "ADMIN")
    void testCreatePromotion_InvalidData() {
        AddPromotionRequest request = AddPromotionRequest.builder()
                .code("")
                .description("")
                .discountType("INVALID_TYPE")
                .discountValue(new BigDecimal("-10.00"))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().minusDays(1))
                .usageLimit(-1)
                .perUserLimit(-1)
                .isActive(true)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/promotions")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should fail to create promotion with duplicate code")
    @WithMockUser(roles = "ADMIN")
    void testCreatePromotion_DuplicateCode() {
        // Create first promotion
        Promotion existingPromotion = new Promotion();
        existingPromotion.setCode("DUPLICATE");
        existingPromotion.setDescription("Existing promotion");
        existingPromotion.setDiscountType(DiscountType.PERCENTAGE);
        existingPromotion.setDiscountValue(new BigDecimal("10.00"));
        existingPromotion.setStartDate(LocalDateTime.now());
        existingPromotion.setEndDate(LocalDateTime.now().plusDays(30));
        existingPromotion.setUsageLimit(100);
        existingPromotion.setPerUserLimit(1);
        existingPromotion.setIsActive(true);
        promotionRepo.save(existingPromotion);

        // Try to create second promotion with same code
        AddPromotionRequest request = AddPromotionRequest.builder()
                .code("DUPLICATE")
                .description("Different description")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("20.00"))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(50)
                .perUserLimit(1)
                .isActive(true)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/promotions")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should fail to create promotion with percentage > 100")
    @WithMockUser(roles = "ADMIN")
    void testCreatePromotion_InvalidPercentage() {
        AddPromotionRequest request = AddPromotionRequest.builder()
                .code("INVALID")
                .description("Invalid percentage")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("150.00"))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(100)
                .perUserLimit(1)
                .isActive(true)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/promotions")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should fail to create promotion when per user limit exceeds usage limit")
    @WithMockUser(roles = "ADMIN")
    void testCreatePromotion_PerUserLimitExceedsUsageLimit() {
        AddPromotionRequest request = AddPromotionRequest.builder()
                .code("INVALID_LIMITS")
                .description("Invalid limits")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("10.00"))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(10)
                .perUserLimit(20) // exceeds usage limit
                .isActive(true)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/promotions")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should get promotion by ID successfully")
    void testGetPromotion_Success() {
        Promotion promotion = new Promotion();
        promotion.setCode("TEST2025");
        promotion.setDescription("Test promotion");
        promotion.setDiscountType(DiscountType.FIXED_AMOUNT);
        promotion.setDiscountValue(new BigDecimal("50000.00"));
        promotion.setStartDate(LocalDateTime.now());
        promotion.setEndDate(LocalDateTime.now().plusDays(30));
        promotion.setUsageLimit(100);
        promotion.setPerUserLimit(1);
        promotion.setIsActive(true);
        promotion = promotionRepo.save(promotion);

        given()
                .when()
                .get("/promotions/" + promotion.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo("TEST2025"))
                .body("description", equalTo("Test promotion"))
                .body("discountType", equalTo("FIXED_AMOUNT"))
                .body("id", equalTo(promotion.getId().toString()));
    }

    @Test
    @DisplayName("Should get promotion by code successfully")
    void testGetPromotionByCode_Success() {
        Promotion promotion = new Promotion();
        promotion.setCode("FINDME");
        promotion.setDescription("Find me promotion");
        promotion.setDiscountType(DiscountType.PERCENTAGE);
        promotion.setDiscountValue(new BigDecimal("25.00"));
        promotion.setStartDate(LocalDateTime.now());
        promotion.setEndDate(LocalDateTime.now().plusDays(30));
        promotion.setUsageLimit(50);
        promotion.setPerUserLimit(2);
        promotion.setIsActive(true);
        promotionRepo.save(promotion);

        given()
                .when()
                .get("/promotions/code/FINDME")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo("FINDME"))
                .body("description", equalTo("Find me promotion"))
                .body("discountValue", equalTo(25.00f));
    }

    @Test
    @DisplayName("Should return 404 when promotion not found")
    void testGetPromotion_NotFound() {
        UUID randomId = UUID.randomUUID();

        given()
                .when()
                .get("/promotions/" + randomId)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should update promotion successfully")
    @WithMockUser(roles = "ADMIN")
    void testUpdatePromotion_Success() {
        Promotion promotion = new Promotion();
        promotion.setCode("ORIGINAL");
        promotion.setDescription("Original description");
        promotion.setDiscountType(DiscountType.PERCENTAGE);
        promotion.setDiscountValue(new BigDecimal("10.00"));
        promotion.setStartDate(LocalDateTime.now());
        promotion.setEndDate(LocalDateTime.now().plusDays(30));
        promotion.setUsageLimit(100);
        promotion.setPerUserLimit(1);
        promotion.setIsActive(true);
        promotion = promotionRepo.save(promotion);

        UpdatePromotionRequest request = UpdatePromotionRequest.builder()
                .description("Updated description")
                .discountValue(new BigDecimal("20.00"))
                .usageLimit(200)
                .perUserLimit(2)
                .discountType(DiscountType.FIXED_AMOUNT.name())
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/promotions/" + promotion.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("code", equalTo("ORIGINAL")) // Code not updated
                .body("description", equalTo("Updated description"))
                .body("discountValue", equalTo(20.00f))
                .body("usageLimit", equalTo(200))
                .body("perUserLimit", equalTo(2));
    }

    @Test
    @DisplayName("Should fail to update promotion when not authenticated as admin")
    @WithMockUser(roles = "USER")
    void testUpdatePromotion_Forbidden() {
        Promotion promotion = new Promotion();
        promotion.setCode("TEST");
        promotion.setDescription("Test promotion");
        promotion.setDiscountType(DiscountType.PERCENTAGE);
        promotion.setDiscountValue(new BigDecimal("10.00"));
        promotion.setStartDate(LocalDateTime.now());
        promotion.setEndDate(LocalDateTime.now().plusDays(30));
        promotion.setUsageLimit(100);
        promotion.setPerUserLimit(1);
        promotion.setIsActive(true);
        promotion = promotionRepo.save(promotion);

        UpdatePromotionRequest request = UpdatePromotionRequest.builder()
                .description("Trying to hack")
                .discountValue(new BigDecimal("99.00"))
                .discountType(DiscountType.FIXED_AMOUNT.name())
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/promotions/" + promotion.getId())
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("Should fail to update promotion with duplicate code")
    @WithMockUser(roles = "ADMIN")
    void testUpdatePromotion_DuplicateCode() {
        // Create first promotion
        Promotion promo1 = new Promotion();
        promo1.setCode("EXISTING");
        promo1.setDescription("Existing promotion");
        promo1.setDiscountType(DiscountType.PERCENTAGE);
        promo1.setDiscountValue(new BigDecimal("10.00"));
        promo1.setStartDate(LocalDateTime.now());
        promo1.setEndDate(LocalDateTime.now().plusDays(30));
        promo1.setUsageLimit(100);
        promo1.setPerUserLimit(1);
        promo1.setIsActive(true);
        promotionRepo.save(promo1);

        // Create second promotion
        Promotion promo2 = new Promotion();
        promo2.setCode("TOUPDATE");
        promo2.setDescription("To update promotion");
        promo2.setDiscountType(DiscountType.PERCENTAGE);
        promo2.setDiscountValue(new BigDecimal("15.00"));
        promo2.setStartDate(LocalDateTime.now());
        promo2.setEndDate(LocalDateTime.now().plusDays(30));
        promo2.setUsageLimit(100);
        promo2.setPerUserLimit(1);
        promo2.setIsActive(true);
        promo2 = promotionRepo.save(promo2);

        // Try to update second promotion with existing code
        UpdatePromotionRequest request = UpdatePromotionRequest.builder()
                .code("EXISTING")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/promotions/" + promo2.getId())
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should fail to update promotion with percentage > 100")
    @WithMockUser(roles = "ADMIN")
    void testUpdatePromotion_InvalidPercentage() {
        Promotion promotion = new Promotion();
        promotion.setCode("TEST");
        promotion.setDescription("Test promotion");
        promotion.setDiscountType(DiscountType.PERCENTAGE);
        promotion.setDiscountValue(new BigDecimal("10.00"));
        promotion.setStartDate(LocalDateTime.now());
        promotion.setEndDate(LocalDateTime.now().plusDays(30));
        promotion.setUsageLimit(100);
        promotion.setPerUserLimit(1);
        promotion.setIsActive(true);
        promotion = promotionRepo.save(promotion);

        UpdatePromotionRequest request = UpdatePromotionRequest.builder()
                .discountValue(new BigDecimal("150.00"))
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/promotions/" + promotion.getId())
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should fail to update promotion when per user limit exceeds usage limit")
    @WithMockUser(roles = "ADMIN")
    void testUpdatePromotion_PerUserLimitExceedsUsageLimit() {
        Promotion promotion = new Promotion();
        promotion.setCode("TEST");
        promotion.setDescription("Test promotion");
        promotion.setDiscountType(DiscountType.PERCENTAGE);
        promotion.setDiscountValue(new BigDecimal("10.00"));
        promotion.setStartDate(LocalDateTime.now());
        promotion.setEndDate(LocalDateTime.now().plusDays(30));
        promotion.setUsageLimit(100);
        promotion.setPerUserLimit(1);
        promotion.setIsActive(true);
        promotion = promotionRepo.save(promotion);

        UpdatePromotionRequest request = UpdatePromotionRequest.builder()
                .usageLimit(10)
                .perUserLimit(20) // exceeds usage limit
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/promotions/" + promotion.getId())
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should fail to update promotion with end date before start date")
    @WithMockUser(roles = "ADMIN")
    void testUpdatePromotion_InvalidDateRange() {
        Promotion promotion = new Promotion();
        promotion.setCode("TEST");
        promotion.setDescription("Test promotion");
        promotion.setDiscountType(DiscountType.PERCENTAGE);
        promotion.setDiscountValue(new BigDecimal("10.00"));
        promotion.setStartDate(LocalDateTime.now());
        promotion.setEndDate(LocalDateTime.now().plusDays(30));
        promotion.setUsageLimit(100);
        promotion.setPerUserLimit(1);
        promotion.setIsActive(true);
        promotion = promotionRepo.save(promotion);

        UpdatePromotionRequest request = UpdatePromotionRequest.builder()
                .startDate(LocalDateTime.now().plusDays(30))
                .endDate(LocalDateTime.now()) // before start date
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/promotions/" + promotion.getId())
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should deactivate promotion successfully")
    @WithMockUser(roles = "ADMIN")
    void testDeactivatePromotion_Success() {
        Promotion promotion = new Promotion();
        promotion.setCode("ACTIVE");
        promotion.setDescription("Active promotion");
        promotion.setDiscountType(DiscountType.PERCENTAGE);
        promotion.setDiscountValue(new BigDecimal("15.00"));
        promotion.setStartDate(LocalDateTime.now());
        promotion.setEndDate(LocalDateTime.now().plusDays(30));
        promotion.setUsageLimit(100);
        promotion.setPerUserLimit(1);
        promotion.setIsActive(true);
        promotion = promotionRepo.save(promotion);

        UUID promotionId = promotion.getId();

        given()
                .when()
                .patch("/promotions/" + promotionId + "/deactivate")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // Verify promotion is deactivated
        Promotion updated = promotionRepo.findById(promotionId).get();
        assert !updated.getIsActive();
    }

    @Test
    @DisplayName("Should delete promotion successfully")
    @WithMockUser(roles = "ADMIN")
    void testDeletePromotion_Success() {
        Promotion promotion = new Promotion();
        promotion.setCode("TO_DELETE");
        promotion.setDescription("Promotion to delete");
        promotion.setDiscountType(DiscountType.PERCENTAGE);
        promotion.setDiscountValue(new BigDecimal("10.00"));
        promotion.setStartDate(LocalDateTime.now());
        promotion.setEndDate(LocalDateTime.now().plusDays(30));
        promotion.setUsageLimit(100);
        promotion.setPerUserLimit(1);
        promotion.setIsActive(true);
        promotion = promotionRepo.save(promotion);

        UUID promotionId = promotion.getId();

        given()
                .when()
                .delete("/promotions/" + promotionId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // Verify promotion is deleted
        assert promotionRepo.findById(promotionId).isEmpty();
    }

    @Test
    @DisplayName("Should fail to delete promotion when not authenticated as admin")
    @WithMockUser(roles = "USER")
    void testDeletePromotion_Forbidden() {
        Promotion promotion = new Promotion();
        promotion.setCode("TEST");
        promotion.setDescription("Test promotion");
        promotion.setDiscountType(DiscountType.PERCENTAGE);
        promotion.setDiscountValue(new BigDecimal("10.00"));
        promotion.setStartDate(LocalDateTime.now());
        promotion.setEndDate(LocalDateTime.now().plusDays(30));
        promotion.setUsageLimit(100);
        promotion.setPerUserLimit(1);
        promotion.setIsActive(true);
        promotion = promotionRepo.save(promotion);

        given()
                .when()
                .delete("/promotions/" + promotion.getId())
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("Should get all promotions successfully")
    void testGetAllPromotions_Success() {
        Promotion promo1 = new Promotion();
        promo1.setCode("PROMO1");
        promo1.setDescription("Promotion 1");
        promo1.setDiscountType(DiscountType.PERCENTAGE);
        promo1.setDiscountValue(new BigDecimal("10.00"));
        promo1.setStartDate(LocalDateTime.now());
        promo1.setEndDate(LocalDateTime.now().plusDays(30));
        promo1.setUsageLimit(100);
        promo1.setPerUserLimit(1);
        promo1.setIsActive(true);
        promotionRepo.save(promo1);

        Promotion promo2 = new Promotion();
        promo2.setCode("PROMO2");
        promo2.setDescription("Promotion 2");
        promo2.setDiscountType(DiscountType.FIXED_AMOUNT);
        promo2.setDiscountValue(new BigDecimal("50000.00"));
        promo2.setStartDate(LocalDateTime.now());
        promo2.setEndDate(LocalDateTime.now().plusDays(30));
        promo2.setUsageLimit(50);
        promo2.setPerUserLimit(2);
        promo2.setIsActive(false);
        promotionRepo.save(promo2);

        given()
                .when()
                .get("/promotions")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(2))
                .body("[0].code", equalTo("PROMO1"))
                .body("[1].code", equalTo("PROMO2"));
    }

    @Test
    @DisplayName("Should get active promotions successfully")
    void testGetActivePromotions_Success() {
        Promotion activePromo = new Promotion();
        activePromo.setCode("ACTIVE");
        activePromo.setDescription("Active promotion");
        activePromo.setDiscountType(DiscountType.PERCENTAGE);
        activePromo.setDiscountValue(new BigDecimal("15.00"));
        activePromo.setStartDate(LocalDateTime.now());
        activePromo.setEndDate(LocalDateTime.now().plusDays(30));
        activePromo.setUsageLimit(100);
        activePromo.setPerUserLimit(1);
        activePromo.setIsActive(true);
        promotionRepo.save(activePromo);

        Promotion inactivePromo = new Promotion();
        inactivePromo.setCode("INACTIVE");
        inactivePromo.setDescription("Inactive promotion");
        inactivePromo.setDiscountType(DiscountType.PERCENTAGE);
        inactivePromo.setDiscountValue(new BigDecimal("20.00"));
        inactivePromo.setStartDate(LocalDateTime.now());
        inactivePromo.setEndDate(LocalDateTime.now().plusDays(30));
        inactivePromo.setUsageLimit(100);
        inactivePromo.setPerUserLimit(1);
        inactivePromo.setIsActive(false);
        promotionRepo.save(inactivePromo);

        given()
                .when()
                .get("/promotions/active")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(1))
                .body("[0].code", equalTo("ACTIVE"))
                .body("[0].isActive", equalTo(true));
    }

    @Test
    @DisplayName("Should get valid promotions successfully")
    void testGetValidPromotions_Success() {
        // Valid: active and within date range
        Promotion validPromo = new Promotion();
        validPromo.setCode("VALID");
        validPromo.setDescription("Valid promotion");
        validPromo.setDiscountType(DiscountType.PERCENTAGE);
        validPromo.setDiscountValue(new BigDecimal("20.00"));
        validPromo.setStartDate(LocalDateTime.now().minusDays(1));
        validPromo.setEndDate(LocalDateTime.now().plusDays(30));
        validPromo.setUsageLimit(100);
        validPromo.setPerUserLimit(1);
        validPromo.setIsActive(true);
        promotionRepo.save(validPromo);

        // Not valid: expired
        Promotion expiredPromo = new Promotion();
        expiredPromo.setCode("EXPIRED");
        expiredPromo.setDescription("Expired promotion");
        expiredPromo.setDiscountType(DiscountType.PERCENTAGE);
        expiredPromo.setDiscountValue(new BigDecimal("10.00"));
        expiredPromo.setStartDate(LocalDateTime.now().minusDays(30));
        expiredPromo.setEndDate(LocalDateTime.now().minusDays(1));
        expiredPromo.setUsageLimit(100);
        expiredPromo.setPerUserLimit(1);
        expiredPromo.setIsActive(true);
        promotionRepo.save(expiredPromo);

        // Not valid: not started yet
        Promotion futurePromo = new Promotion();
        futurePromo.setCode("FUTURE");
        futurePromo.setDescription("Future promotion");
        futurePromo.setDiscountType(DiscountType.PERCENTAGE);
        futurePromo.setDiscountValue(new BigDecimal("30.00"));
        futurePromo.setStartDate(LocalDateTime.now().plusDays(1));
        futurePromo.setEndDate(LocalDateTime.now().plusDays(30));
        futurePromo.setUsageLimit(100);
        futurePromo.setPerUserLimit(1);
        futurePromo.setIsActive(true);
        promotionRepo.save(futurePromo);

        given()
                .when()
                .get("/promotions/valid")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(1))
                .body("[0].code", equalTo("VALID"))
                .body("[0].isActive", equalTo(true));
    }

    @Test
    @DisplayName("Should filter promotions by active status")
    void testFilterPromotionsByActive_Success() {
        Promotion activePromo = new Promotion();
        activePromo.setCode("ACTIVE_FILTER");
        activePromo.setDescription("Active promotion");
        activePromo.setDiscountType(DiscountType.PERCENTAGE);
        activePromo.setDiscountValue(new BigDecimal("15.00"));
        activePromo.setStartDate(LocalDateTime.now());
        activePromo.setEndDate(LocalDateTime.now().plusDays(30));
        activePromo.setUsageLimit(100);
        activePromo.setPerUserLimit(1);
        activePromo.setIsActive(true);
        promotionRepo.save(activePromo);

        Promotion inactivePromo = new Promotion();
        inactivePromo.setCode("INACTIVE_FILTER");
        inactivePromo.setDescription("Inactive promotion");
        inactivePromo.setDiscountType(DiscountType.PERCENTAGE);
        inactivePromo.setDiscountValue(new BigDecimal("20.00"));
        inactivePromo.setStartDate(LocalDateTime.now());
        inactivePromo.setEndDate(LocalDateTime.now().plusDays(30));
        inactivePromo.setUsageLimit(100);
        inactivePromo.setPerUserLimit(1);
        inactivePromo.setIsActive(false);
        promotionRepo.save(inactivePromo);

        given()
                .param("filter", "active")
                .when()
                .get("/promotions")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(1))
                .body("[0].code", equalTo("ACTIVE_FILTER"));
    }
}
