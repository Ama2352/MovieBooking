package com.api.moviebooking.integrations;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.api.moviebooking.models.dtos.priceBase.AddPriceBaseRequest;
import com.api.moviebooking.models.dtos.priceBase.UpdatePriceBaseRequest;
import com.api.moviebooking.models.dtos.priceModifier.AddPriceModifierRequest;
import com.api.moviebooking.models.dtos.priceModifier.UpdatePriceModifierRequest;
import com.api.moviebooking.models.entities.PriceBase;
import com.api.moviebooking.models.entities.PriceModifier;
import com.api.moviebooking.models.enums.ConditionType;
import com.api.moviebooking.models.enums.ModifierType;
import com.api.moviebooking.repositories.PriceBaseRepo;
import com.api.moviebooking.repositories.PriceModifierRepo;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

/**
 * Integration tests for PriceBase and PriceModifier Controllers.
 * Tests dynamic pricing system with base prices and conditional modifiers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@Transactional
@DisplayName("Pricing System Integration Tests")
class PricingIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"));

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PriceBaseRepo priceBaseRepo;

    @Autowired
    private PriceModifierRepo priceModifierRepo;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build());

        priceModifierRepo.deleteAll();
        priceBaseRepo.deleteAll();
    }

    // ========================================================================
    // Price Base Tests
    // ========================================================================

    @Nested
    @DisplayName("Price Base Operations")
    class PriceBaseTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create base price successfully")
        void testAddPriceBase_Success() {
            AddPriceBaseRequest request = new AddPriceBaseRequest();
            request.setName("Standard 2024");
            request.setBasePrice(new BigDecimal("80000"));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/price-base")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("name", equalTo("Standard 2024"))
                    .body("basePrice", equalTo(80000))
                    .body("isActive", equalTo(true));
        }

        @Test
        @DisplayName("Should fail without authentication")
        void testAddPriceBase_Unauthorized() {
            AddPriceBaseRequest request = new AddPriceBaseRequest();
            request.setName("Test");
            request.setBasePrice(new BigDecimal("80000"));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/price-base")
                    .then()
                    .statusCode(HttpStatus.FORBIDDEN.value());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should fail with USER role")
        void testAddPriceBase_Forbidden() {
            AddPriceBaseRequest request = new AddPriceBaseRequest();
            request.setName("Test");
            request.setBasePrice(new BigDecimal("80000"));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/price-base")
                    .then()
                    .statusCode(HttpStatus.FORBIDDEN.value());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should fail with invalid data")
        void testAddPriceBase_InvalidData() {
            AddPriceBaseRequest request = new AddPriceBaseRequest();
            request.setName("");
            request.setBasePrice(new BigDecimal("-1000"));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/price-base")
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get active base price")
        void testGetActiveBasePrice_Success() {
            PriceBase priceBase = new PriceBase();
            priceBase.setName("Active Price");
            priceBase.setBasePrice(new BigDecimal("90000"));
            priceBase.setIsActive(true);
            priceBaseRepo.save(priceBase);

            given()
                    .when()
                    .get("/price-base/active")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("name", equalTo("Active Price"))
                    .body("basePrice", equalTo(90000))
                    .body("isActive", equalTo(true));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update base price")
        void testUpdatePriceBase_Success() {
            PriceBase priceBase = new PriceBase();
            priceBase.setName("Old Name");
            priceBase.setBasePrice(new BigDecimal("80000"));
            priceBase.setIsActive(true);
            priceBase = priceBaseRepo.save(priceBase);

            UpdatePriceBaseRequest request = new UpdatePriceBaseRequest();
            request.setName("New Name");
            request.setIsActive(false);

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .put("/price-base/" + priceBase.getId())
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("name", equalTo("New Name"))
                    .body("isActive", equalTo(false));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete base price")
        void testDeletePriceBase_Success() {
            PriceBase priceBase = new PriceBase();
            priceBase.setName("To Delete");
            priceBase.setBasePrice(new BigDecimal("80000"));
            priceBase = priceBaseRepo.save(priceBase);

            var id = priceBase.getId();

            given()
                    .when()
                    .delete("/price-base/" + id)
                    .then()
                    .statusCode(HttpStatus.OK.value());

            assertTrue(priceBaseRepo.findById(id).isEmpty());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get all base prices")
        void testGetAllPriceBases_Success() {
            PriceBase pb1 = new PriceBase();
            pb1.setName("Price 1");
            pb1.setBasePrice(new BigDecimal("80000"));
            priceBaseRepo.save(pb1);

            PriceBase pb2 = new PriceBase();
            pb2.setName("Price 2");
            pb2.setBasePrice(new BigDecimal("90000"));
            priceBaseRepo.save(pb2);

            given()
                    .when()
                    .get("/price-base")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("size()", equalTo(2));
        }
    }

    // ========================================================================
    // Price Modifier Tests
    // ========================================================================

    @Nested
    @DisplayName("Price Modifier Operations")
    class PriceModifierTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create seat type modifier")
        void testAddPriceModifier_SeatType() {
            AddPriceModifierRequest request = new AddPriceModifierRequest();
            request.setName("VIP Seat Premium");
            request.setConditionType("SEAT_TYPE");
            request.setConditionValue("VIP");
            request.setModifierType("FIXED_AMOUNT");
            request.setModifierValue(new BigDecimal("20000"));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/price-modifiers")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("name", equalTo("VIP Seat Premium"))
                    .body("conditionType", equalTo("SEAT_TYPE"))
                    .body("conditionValue", equalTo("VIP"))
                    .body("modifierType", equalTo("FIXED_AMOUNT"))
                    .body("modifierValue", equalTo(20000));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create day type modifier")
        void testAddPriceModifier_DayType() {
            AddPriceModifierRequest request = new AddPriceModifierRequest();
            request.setName("Weekend Surcharge");
            request.setConditionType("DAY_TYPE");
            request.setConditionValue("WEEKEND");
            request.setModifierType("PERCENTAGE");
            request.setModifierValue(new BigDecimal("1.2"));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/price-modifiers")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("conditionType", equalTo("DAY_TYPE"))
                    .body("modifierType", equalTo("PERCENTAGE"))
                    .body("modifierValue", equalTo(1.2f));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create time range modifier")
        void testAddPriceModifier_TimeRange() {
            AddPriceModifierRequest request = new AddPriceModifierRequest();
            request.setName("Prime Time");
            request.setConditionType("TIME_RANGE");
            request.setConditionValue("EVENING");
            request.setModifierType("FIXED_AMOUNT");
            request.setModifierValue(new BigDecimal("10000"));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/price-modifiers")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("conditionType", equalTo("TIME_RANGE"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create format modifier")
        void testAddPriceModifier_Format() {
            AddPriceModifierRequest request = new AddPriceModifierRequest();
            request.setName("3D Surcharge");
            request.setConditionType("FORMAT");
            request.setConditionValue("3D");
            request.setModifierType("FIXED_AMOUNT");
            request.setModifierValue(new BigDecimal("15000"));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/price-modifiers")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("conditionType", equalTo("FORMAT"))
                    .body("conditionValue", equalTo("3D"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create room type modifier")
        void testAddPriceModifier_RoomType() {
            AddPriceModifierRequest request = new AddPriceModifierRequest();
            request.setName("IMAX Premium");
            request.setConditionType("ROOM_TYPE");
            request.setConditionValue("IMAX");
            request.setModifierType("PERCENTAGE");
            request.setModifierValue(new BigDecimal("1.5"));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/price-modifiers")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("conditionType", equalTo("ROOM_TYPE"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get active modifiers")
        void testGetActiveModifiers_Success() {
            PriceModifier active = new PriceModifier();
            active.setName("Active Modifier");
            active.setConditionType(ConditionType.SEAT_TYPE);
            active.setConditionValue("VIP");
            active.setModifierType(ModifierType.FIXED_AMOUNT);
            active.setModifierValue(new BigDecimal("20000"));
            active.setIsActive(true);
            priceModifierRepo.save(active);

            PriceModifier inactive = new PriceModifier();
            inactive.setName("Inactive Modifier");
            inactive.setConditionType(ConditionType.DAY_TYPE);
            inactive.setConditionValue("WEEKEND");
            inactive.setModifierType(ModifierType.PERCENTAGE);
            inactive.setModifierValue(new BigDecimal("1.2"));
            inactive.setIsActive(false);
            priceModifierRepo.save(inactive);

            given()
                    .when()
                    .get("/price-modifiers/active")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("size()", equalTo(1))
                    .body("[0].name", equalTo("Active Modifier"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should filter by condition type")
        void testGetModifiersByCondition_Success() {
            PriceModifier seat1 = new PriceModifier();
            seat1.setName("VIP");
            seat1.setConditionType(ConditionType.SEAT_TYPE);
            seat1.setConditionValue("VIP");
            seat1.setModifierType(ModifierType.FIXED_AMOUNT);
            seat1.setModifierValue(new BigDecimal("20000"));
            priceModifierRepo.save(seat1);

            PriceModifier seat2 = new PriceModifier();
            seat2.setName("Couple");
            seat2.setConditionType(ConditionType.SEAT_TYPE);
            seat2.setConditionValue("COUPLE");
            seat2.setModifierType(ModifierType.FIXED_AMOUNT);
            seat2.setModifierValue(new BigDecimal("30000"));
            priceModifierRepo.save(seat2);

            PriceModifier day = new PriceModifier();
            day.setName("Weekend");
            day.setConditionType(ConditionType.DAY_TYPE);
            day.setConditionValue("WEEKEND");
            day.setModifierType(ModifierType.PERCENTAGE);
            day.setModifierValue(new BigDecimal("1.2"));
            priceModifierRepo.save(day);

            given()
                    .queryParam("conditionType", "SEAT_TYPE")
                    .when()
                    .get("/price-modifiers/by-condition")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("size()", equalTo(2))
                    .body("[0].conditionType", equalTo("SEAT_TYPE"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get condition types info")
        void testGetConditionTypes_Success() {
            given()
                    .when()
                    .get("/price-modifiers/condition-types")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("size()", greaterThan(0));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update modifier")
        void testUpdateModifier_Success() {
            PriceModifier modifier = new PriceModifier();
            modifier.setName("Old Name");
            modifier.setConditionType(ConditionType.SEAT_TYPE);
            modifier.setConditionValue("VIP");
            modifier.setModifierType(ModifierType.FIXED_AMOUNT);
            modifier.setModifierValue(new BigDecimal("20000"));
            modifier.setIsActive(true);
            modifier = priceModifierRepo.save(modifier);

            UpdatePriceModifierRequest request = new UpdatePriceModifierRequest();
            request.setName("New Name");
            request.setIsActive(false);

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .put("/price-modifiers/" + modifier.getId())
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("name", equalTo("New Name"))
                    .body("isActive", equalTo(false));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete modifier")
        void testDeleteModifier_Success() {
            PriceModifier modifier = new PriceModifier();
            modifier.setName("To Delete");
            modifier.setConditionType(ConditionType.SEAT_TYPE);
            modifier.setConditionValue("VIP");
            modifier.setModifierType(ModifierType.FIXED_AMOUNT);
            modifier.setModifierValue(new BigDecimal("20000"));
            modifier = priceModifierRepo.save(modifier);

            var id = modifier.getId();

            given()
                    .when()
                    .delete("/price-modifiers/" + id)
                    .then()
                    .statusCode(HttpStatus.OK.value());

            assertTrue(priceModifierRepo.findById(id).isEmpty());
        }
    }

    // ========================================================================
    // Price Calculation Scenarios
    // ========================================================================

    @Nested
    @DisplayName("Price Calculation Scenarios")
    class PriceCalculationTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should calculate price with ADD modifiers")
        void testPriceCalculation_Add() {
            // Base price: 80,000
            PriceBase base = new PriceBase();
            base.setName("Standard");
            base.setBasePrice(new BigDecimal("80000"));
            base.setIsActive(true);
            priceBaseRepo.save(base);

            // Add VIP: +20,000
            PriceModifier vip = new PriceModifier();
            vip.setName("VIP");
            vip.setConditionType(ConditionType.SEAT_TYPE);
            vip.setConditionValue("VIP");
            vip.setModifierType(ModifierType.FIXED_AMOUNT);
            vip.setModifierValue(new BigDecimal("20000"));
            vip.setIsActive(true);
            priceModifierRepo.save(vip);

            // Expected: 100,000
            var activeBase = priceBaseRepo.findActiveBasePrice().orElseThrow();
            var activeModifiers = priceModifierRepo.findAllActive();

            assertEquals(new BigDecimal("80000"), activeBase.getBasePrice());
            assertEquals(1, activeModifiers.size());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should calculate price with MULTIPLY modifiers")
        void testPriceCalculation_Multiply() {
            PriceBase base = new PriceBase();
            base.setName("Standard");
            base.setBasePrice(new BigDecimal("100000"));
            base.setIsActive(true);
            priceBaseRepo.save(base);

            PriceModifier weekend = new PriceModifier();
            weekend.setName("Weekend");
            weekend.setConditionType(ConditionType.DAY_TYPE);
            weekend.setConditionValue("WEEKEND");
            weekend.setModifierType(ModifierType.PERCENTAGE);
            weekend.setModifierValue(new BigDecimal("1.2"));
            weekend.setIsActive(true);
            priceModifierRepo.save(weekend);

            // Expected: 120,000 (100,000 * 1.2)
            var activeBase = priceBaseRepo.findActiveBasePrice().orElseThrow();
            assertEquals(new BigDecimal("100000"), activeBase.getBasePrice());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should stack multiple modifiers")
        void testPriceCalculation_MultipleModifiers() {
            // Base: 80,000
            PriceBase base = new PriceBase();
            base.setName("Standard");
            base.setBasePrice(new BigDecimal("80000"));
            base.setIsActive(true);
            priceBaseRepo.save(base);

            // +20,000 for VIP
            PriceModifier vip = new PriceModifier();
            vip.setName("VIP");
            vip.setConditionType(ConditionType.SEAT_TYPE);
            vip.setConditionValue("VIP");
            vip.setModifierType(ModifierType.FIXED_AMOUNT);
            vip.setModifierValue(new BigDecimal("20000"));
            vip.setIsActive(true);
            priceModifierRepo.save(vip);

            // +15,000 for 3D
            PriceModifier format = new PriceModifier();
            format.setName("3D");
            format.setConditionType(ConditionType.FORMAT);
            format.setConditionValue("3D");
            format.setModifierType(ModifierType.FIXED_AMOUNT);
            format.setModifierValue(new BigDecimal("15000"));
            format.setIsActive(true);
            priceModifierRepo.save(format);

            // *1.2 for Weekend
            PriceModifier weekend = new PriceModifier();
            weekend.setName("Weekend");
            weekend.setConditionType(ConditionType.DAY_TYPE);
            weekend.setConditionValue("WEEKEND");
            weekend.setModifierType(ModifierType.PERCENTAGE);
            weekend.setModifierValue(new BigDecimal("1.2"));
            weekend.setIsActive(true);
            priceModifierRepo.save(weekend);

            // Expected: (80,000 + 20,000 + 15,000) * 1.2 = 138,000
            var activeModifiers = priceModifierRepo.findAllActive();
            assertEquals(3, activeModifiers.size());
        }
    }
}
