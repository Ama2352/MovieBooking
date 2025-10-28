package com.api.moviebooking.integrations;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.api.moviebooking.models.dtos.cinema.AddCinemaRequest;
import com.api.moviebooking.models.dtos.cinema.UpdateCinemaRequest;
import com.api.moviebooking.models.dtos.room.AddRoomRequest;
import com.api.moviebooking.models.dtos.room.UpdateRoomRequest;
import com.api.moviebooking.models.dtos.snack.AddSnackRequest;
import com.api.moviebooking.models.dtos.snack.UpdateSnackRequest;
import com.api.moviebooking.models.entities.Cinema;
import com.api.moviebooking.models.entities.Snack;
import com.api.moviebooking.repositories.CinemaRepo;
import com.api.moviebooking.repositories.RoomRepo;
import com.api.moviebooking.repositories.SeatRepo;
import com.api.moviebooking.repositories.SnackRepo;
import com.api.moviebooking.services.CinemaService;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

/**
 * Integration tests for CinemaController using Testcontainers and RestAssured.
 * Tests Cinema, Room, and Snack CRUD operations with proper security context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class CinemaIntegrationTest {

        @Container
        @ServiceConnection
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
                        DockerImageName.parse("postgres:15-alpine"));

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private CinemaRepo cinemaRepo;

        @Autowired
        private RoomRepo roomRepo;

        @Autowired
        private SeatRepo seatRepo;

        @Autowired
        private SnackRepo snackRepo;

        @Autowired
        private CinemaService cinemaService;

        @BeforeEach
        void setUp() {
                RestAssuredMockMvc.mockMvc(MockMvcBuilders
                                .webAppContextSetup(webApplicationContext)
                                .apply(springSecurity())
                                .build());

                // Clean up test data in correct order (children first, then parents)
                snackRepo.deleteAll();
                seatRepo.deleteAll(); // Seats reference rooms
                roomRepo.deleteAll(); // Rooms reference cinemas
                cinemaRepo.deleteAll();
        }

        @Test
        @DisplayName("Should violates foreign key constraint on children side due to the deletion of parent")
        @Transactional(propagation = Propagation.NOT_SUPPORTED) // Disable transaction for this test
        void deleteCinema_businessLayerPreventsWithSnacks() {
                // Arrange
                Cinema cinema = new Cinema();
                cinema.setName("Test Cinema");
                cinema.setAddress("Test Address");
                cinema.setHotline("12345");

                Snack snack = new Snack();
                snack.setName("Popcorn");
                snack.setType("Food");
                snack.setPrice(new java.math.BigDecimal("50000"));
                snack.setCinema(cinema);
                cinema.getSnacks().add(snack);
                cinemaRepo.save(cinema);

                // Act & Assert
                final UUID cinemaId = cinemaRepo.findById(cinema.getId()).orElseThrow().getId();
                assertThrows(DataIntegrityViolationException.class, () -> {
                        cinemaService.deleteCinema_violatesForeignKeyConstraint(cinemaId); // Expected error here
                });
        }

        // ==================== Cinema CRUD Tests ====================

        @Test
        @DisplayName("Should create cinema successfully when authenticated as admin")
        @WithMockUser(roles = "ADMIN")
        void testAddCinema_Success() {
                AddCinemaRequest request = AddCinemaRequest.builder()
                                .name("CGV Vincom")
                                .address("123 Nguyen Hue St, District 1")
                                .hotline("1900-6017")
                                .build();

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/cinemas")
                                .then()
                                .statusCode(HttpStatus.CREATED.value())
                                .body("name", equalTo("CGV Vincom"))
                                .body("address", equalTo("123 Nguyen Hue St, District 1"))
                                .body("hotline", equalTo("1900-6017"))
                                .body("id", notNullValue());
        }

        @Test
        @DisplayName("Should fail to create cinema when not authenticated")
        void testAddCinema_Unauthorized() {
                AddCinemaRequest request = AddCinemaRequest.builder()
                                .name("CGV Vincom")
                                .address("123 Nguyen Hue St, District 1")
                                .hotline("1900-6017")
                                .build();

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/cinemas")
                                .then()
                                .statusCode(HttpStatus.FORBIDDEN.value()); // Spring Security unauthenticated -> 403
        }

        @Test
        @DisplayName("Should fail to create cinema with invalid data")
        @WithMockUser(roles = "ADMIN")
        void testAddCinema_InvalidData() {
                AddCinemaRequest request = AddCinemaRequest.builder()
                                .name("") // Empty name - validation error
                                .address("123 Nguyen Hue St, District 1")
                                .hotline("1900-6017")
                                .build();

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/cinemas")
                                .then()
                                .statusCode(HttpStatus.BAD_REQUEST.value()); // @Valid annotation ->
                                                                             // MethodArgumentNotValidException ->
                                                                             // 400
        }

        @Test
        @DisplayName("Should get cinema by ID successfully")
        @WithMockUser(roles = "ADMIN")
        void testGetCinema_Success() {
                // Create test cinema
                Cinema cinema = new Cinema();
                cinema.setName("Galaxy Cinema");
                cinema.setAddress("456 Le Loi St, District 1");
                cinema.setHotline("1900-2224");
                cinema = cinemaRepo.save(cinema);

                given()
                                .when()
                                .get("/cinemas/" + cinema.getId())
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .body("name", equalTo("Galaxy Cinema"))
                                .body("address", equalTo("456 Le Loi St, District 1"))
                                .body("hotline", equalTo("1900-2224"));
        }

        @Test
        @DisplayName("Should return 404 when cinema not found")
        @WithMockUser(roles = "ADMIN")
        void testGetCinema_NotFound() {
                UUID nonExistentId = UUID.randomUUID();

                given()
                                .when()
                                .get("/cinemas/" + nonExistentId)
                                .then()
                                .statusCode(HttpStatus.NOT_FOUND.value()); // ResourceNotFoundException from service
                                                                           // -> 404
        }

        @Test
        @DisplayName("Should update cinema successfully")
        @WithMockUser(roles = "ADMIN")
        void testUpdateCinema_Success() {
                // Create test cinema
                Cinema cinema = new Cinema();
                cinema.setName("Old Name");
                cinema.setAddress("Old Address");
                cinema.setHotline("0000-0000");
                cinema = cinemaRepo.save(cinema);

                UpdateCinemaRequest request = UpdateCinemaRequest.builder()
                                .name("Updated Cinema Name")
                                .address("Updated Address")
                                .hotline("1900-9999")
                                .build();

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .put("/cinemas/" + cinema.getId())
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .body("name", equalTo("Updated Cinema Name"))
                                .body("address", equalTo("Updated Address"))
                                .body("hotline", equalTo("1900-9999"));
        }

        @Test
        @DisplayName("Should delete cinema successfully when no rooms or snacks exist")
        @WithMockUser(roles = "ADMIN")
        void testDeleteCinema_Success() {
                // Create test cinema
                Cinema cinema = new Cinema();
                cinema.setName("To Be Deleted");
                cinema.setAddress("Delete Address");
                cinema.setHotline("0000-0000");
                cinema = cinemaRepo.save(cinema);

                given()
                                .when()
                                .delete("/cinemas/" + cinema.getId())
                                .then()
                                .statusCode(HttpStatus.OK.value());

                // Verify deletion
                assert cinemaRepo.findById(cinema.getId()).isEmpty();
        }

        // ==================== Room CRUD Tests ====================

        @Test
        @DisplayName("Should create room successfully")
        @WithMockUser(roles = "ADMIN")
        void testAddRoom_Success() {
                // Create parent cinema first
                Cinema cinema = new Cinema();
                cinema.setName("Test Cinema");
                cinema.setAddress("Test Address");
                cinema.setHotline("1234-5678");
                cinema = cinemaRepo.save(cinema);

                AddRoomRequest request = AddRoomRequest.builder()
                                .cinemaId(cinema.getId())
                                .roomType("IMAX")
                                .roomNumber(1)
                                .build();

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/cinemas/rooms")
                                .then()
                                .statusCode(HttpStatus.CREATED.value())
                                .body("roomType", equalTo("IMAX"))
                                .body("roomNumber", equalTo(1))
                                .body("id", notNullValue());
        }

        @Test
        @DisplayName("Should fail to create room with non-existent cinema")
        @WithMockUser(roles = "ADMIN")
        void testAddRoom_CinemaNotFound() {
                AddRoomRequest request = AddRoomRequest.builder()
                                .cinemaId(UUID.randomUUID())
                                .roomType("IMAX")
                                .roomNumber(1)
                                .build();

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/cinemas/rooms")
                                .then()
                                .statusCode(HttpStatus.NOT_FOUND.value()); // ResourceNotFoundException from service
                                                                           // -> 404
        }

        @Test
        @DisplayName("Should get room by ID successfully")
        @WithMockUser(roles = "ADMIN")
        void testGetRoom_Success() {
                // Create cinema and room
                Cinema cinema = new Cinema();
                cinema.setName("Test Cinema");
                cinema.setAddress("Test Address");
                cinema.setHotline("1234-5678");
                cinema = cinemaRepo.save(cinema);

                AddRoomRequest addRequest = AddRoomRequest.builder()
                                .cinemaId(cinema.getId())
                                .roomType("Standard")
                                .roomNumber(2)
                                .build();

                String roomId = given()
                                .contentType(ContentType.JSON)
                                .body(addRequest)
                                .when()
                                .post("/cinemas/rooms")
                                .then()
                                .statusCode(HttpStatus.CREATED.value())
                                .extract().path("id");

                given()
                                .when()
                                .get("/cinemas/rooms/" + roomId)
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .body("roomType", equalTo("Standard"))
                                .body("roomNumber", equalTo(2));
        }

        @Test
        @DisplayName("Should update room successfully")
        @WithMockUser(roles = "ADMIN")
        void testUpdateRoom_Success() {
                // Create cinema and room
                Cinema cinema = new Cinema();
                cinema.setName("Test Cinema");
                cinema.setAddress("Test Address");
                cinema.setHotline("1234-5678");
                cinema = cinemaRepo.save(cinema);

                AddRoomRequest addRequest = AddRoomRequest.builder()
                                .cinemaId(cinema.getId())
                                .roomType("Standard")
                                .roomNumber(3)
                                .build();

                String roomId = given()
                                .contentType(ContentType.JSON)
                                .body(addRequest)
                                .when()
                                .post("/cinemas/rooms")
                                .then()
                                .statusCode(HttpStatus.CREATED.value())
                                .extract().path("id");

                UpdateRoomRequest updateRequest = UpdateRoomRequest.builder()
                                .roomType("VIP")
                                .roomNumber(5)
                                .build();

                given()
                                .contentType(ContentType.JSON)
                                .body(updateRequest)
                                .when()
                                .put("/cinemas/rooms/" + roomId)
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .body("roomType", equalTo("VIP"))
                                .body("roomNumber", equalTo(5));
        }

        @Test
        @DisplayName("Should delete room successfully")
        @WithMockUser(roles = "ADMIN")
        void testDeleteRoom_Success() {
                // Create cinema and room
                Cinema cinema = new Cinema();
                cinema.setName("Test Cinema");
                cinema.setAddress("Test Address");
                cinema.setHotline("1234-5678");
                cinema = cinemaRepo.save(cinema);

                AddRoomRequest addRequest = AddRoomRequest.builder()
                                .cinemaId(cinema.getId())
                                .roomType("Standard")
                                .roomNumber(4)
                                .build();

                String roomId = given()
                                .contentType(ContentType.JSON)
                                .body(addRequest)
                                .when()
                                .post("/cinemas/rooms")
                                .then()
                                .statusCode(HttpStatus.CREATED.value())
                                .extract().path("id");

                given()
                                .when()
                                .delete("/cinemas/rooms/" + roomId)
                                .then()
                                .statusCode(HttpStatus.OK.value());

                // Verify deletion
                assert roomRepo.findById(UUID.fromString(roomId)).isEmpty();
        }

        // ==================== Snack CRUD Tests ====================

        @Test
        @DisplayName("Should create snack successfully")
        @WithMockUser(roles = "ADMIN")
        void testAddSnack_Success() {
                // Create parent cinema first
                Cinema cinema = new Cinema();
                cinema.setName("Test Cinema");
                cinema.setAddress("Test Address");
                cinema.setHotline("1234-5678");
                cinema = cinemaRepo.save(cinema);

                AddSnackRequest request = AddSnackRequest.builder()
                                .cinemaId(cinema.getId())
                                .name("Popcorn Large")
                                .description("Large size popcorn with butter")
                                .price(new BigDecimal("50000"))
                                .type("FOOD")
                                .build();

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/cinemas/snacks")
                                .then()
                                .statusCode(HttpStatus.CREATED.value())
                                .body("name", equalTo("Popcorn Large"))
                                .body("description", equalTo("Large size popcorn with butter"))
                                .body("price", equalTo(50000))
                                .body("type", equalTo("FOOD"))
                                .body("id", notNullValue());
        }

        @Test
        @DisplayName("Should fail to create snack with non-existent cinema")
        @WithMockUser(roles = "ADMIN")
        void testAddSnack_CinemaNotFound() {
                AddSnackRequest request = AddSnackRequest.builder()
                                .cinemaId(UUID.randomUUID())
                                .name("Popcorn Large")
                                .description("Large size popcorn with butter")
                                .price(new BigDecimal("50000"))
                                .type("FOOD")
                                .build();

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/cinemas/snacks")
                                .then()
                                .statusCode(HttpStatus.NOT_FOUND.value()); // ResourceNotFoundException from service
                                                                           // -> 404
        }

        @Test
        @DisplayName("Should get snack by ID successfully")
        @WithMockUser(roles = "ADMIN")
        void testGetSnack_Success() {
                // Create cinema and snack
                Cinema cinema = new Cinema();
                cinema.setName("Test Cinema");
                cinema.setAddress("Test Address");
                cinema.setHotline("1234-5678");
                cinema = cinemaRepo.save(cinema);

                AddSnackRequest addRequest = AddSnackRequest.builder()
                                .cinemaId(cinema.getId())
                                .name("Coca Cola")
                                .description("Soft drink")
                                .price(new BigDecimal("30000"))
                                .type("DRINK")
                                .build();

                String snackId = given()
                                .contentType(ContentType.JSON)
                                .body(addRequest)
                                .when()
                                .post("/cinemas/snacks")
                                .then()
                                .statusCode(HttpStatus.CREATED.value())
                                .extract().path("id");

                given()
                                .when()
                                .get("/cinemas/snacks/" + snackId)
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .body("name", equalTo("Coca Cola"))
                                .body("description", equalTo("Soft drink"))
                                .body("price", equalTo(30000.0f))
                                .body("type", equalTo("DRINK"));
        }

        @Test
        @DisplayName("Should update snack successfully")
        @WithMockUser(roles = "ADMIN")
        void testUpdateSnack_Success() {
                // Create cinema and snack
                Cinema cinema = new Cinema();
                cinema.setName("Test Cinema");
                cinema.setAddress("Test Address");
                cinema.setHotline("1234-5678");
                cinema = cinemaRepo.save(cinema);

                AddSnackRequest addRequest = AddSnackRequest.builder()
                                .cinemaId(cinema.getId())
                                .name("Old Snack")
                                .description("Old description")
                                .price(new BigDecimal("20000"))
                                .type("FOOD")
                                .build();

                String snackId = given()
                                .contentType(ContentType.JSON)
                                .body(addRequest)
                                .when()
                                .post("/cinemas/snacks")
                                .then()
                                .statusCode(HttpStatus.CREATED.value())
                                .extract().path("id");

                UpdateSnackRequest updateRequest = UpdateSnackRequest.builder()
                                .name("Updated Snack")
                                .description("Updated description")
                                .price(new BigDecimal("25000"))
                                .type("COMBO")
                                .build();

                given()
                                .contentType(ContentType.JSON)
                                .body(updateRequest)
                                .when()
                                .put("/cinemas/snacks/" + snackId)
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .body("name", equalTo("Updated Snack"))
                                .body("description", equalTo("Updated description"))
                                .body("price", equalTo(25000))
                                .body("type", equalTo("COMBO"));
        }

        @Test
        @DisplayName("Should delete snack successfully")
        @WithMockUser(roles = "ADMIN")
        void testDeleteSnack_Success() {
                // Create cinema and snack
                Cinema cinema = new Cinema();
                cinema.setName("Test Cinema");
                cinema.setAddress("Test Address");
                cinema.setHotline("1234-5678");
                cinema = cinemaRepo.save(cinema);

                AddSnackRequest addRequest = AddSnackRequest.builder()
                                .cinemaId(cinema.getId())
                                .name("To Delete Snack")
                                .description("Will be deleted")
                                .price(new BigDecimal("15000"))
                                .type("FOOD")
                                .build();

                String snackId = given()
                                .contentType(ContentType.JSON)
                                .body(addRequest)
                                .when()
                                .post("/cinemas/snacks")
                                .then()
                                .statusCode(HttpStatus.CREATED.value())
                                .extract().path("id");

                given()
                                .when()
                                .delete("/cinemas/snacks/" + snackId)
                                .then()
                                .statusCode(HttpStatus.OK.value());

                // Verify deletion
                assert snackRepo.findById(UUID.fromString(snackId)).isEmpty();
        }

        @Test
        @DisplayName("Should get all cinemas successfully")
        @WithMockUser(roles = "ADMIN")
        void testGetAllCinemas_Success() {
                // Create two cinemas
                Cinema c1 = new Cinema();
                c1.setName("Cinema One");
                c1.setAddress("Addr 1");
                c1.setHotline("111");
                cinemaRepo.save(c1);

                Cinema c2 = new Cinema();
                c2.setName("Cinema Two");
                c2.setAddress("Addr 2");
                c2.setHotline("222");
                cinemaRepo.save(c2);

                given()
                                .when()
                                .get("/cinemas")
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .body("size()", greaterThanOrEqualTo(2))
                                .body("name", hasItems("Cinema One", "Cinema Two"));
        }

        @Test
        @DisplayName("Should get all rooms successfully")
        @WithMockUser(roles = "ADMIN")
        void testGetAllRooms_Success() {
                Cinema cinema = new Cinema();
                cinema.setName("Rooms Cinema");
                cinema.setAddress("R Addr");
                cinema.setHotline("333");
                cinema = cinemaRepo.save(cinema);

                AddRoomRequest r1 = AddRoomRequest.builder()
                                .cinemaId(cinema.getId())
                                .roomType("TypeA")
                                .roomNumber(10)
                                .build();

                AddRoomRequest r2 = AddRoomRequest.builder()
                                .cinemaId(cinema.getId())
                                .roomType("TypeB")
                                .roomNumber(11)
                                .build();

                given().contentType(ContentType.JSON).body(r1).when().post("/cinemas/rooms").then()
                                .statusCode(HttpStatus.CREATED.value());
                given().contentType(ContentType.JSON).body(r2).when().post("/cinemas/rooms").then()
                                .statusCode(HttpStatus.CREATED.value());

                given()
                                .when()
                                .get("/cinemas/rooms")
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .body("size()", greaterThanOrEqualTo(2))
                                .body("roomType", hasItems("TypeA", "TypeB"));
        }

        @Test
        @DisplayName("Should get all snacks successfully")
        @WithMockUser(roles = "ADMIN")
        void testGetAllSnacks_Success() {
                Cinema cinema = new Cinema();
                cinema.setName("Snacks Cinema");
                cinema.setAddress("S Addr");
                cinema.setHotline("444");
                cinema = cinemaRepo.save(cinema);

                AddSnackRequest s1 = AddSnackRequest.builder()
                                .cinemaId(cinema.getId())
                                .name("Snack A")
                                .description("desc")
                                .price(new java.math.BigDecimal("10000"))
                                .type("FOOD")
                                .build();

                AddSnackRequest s2 = AddSnackRequest.builder()
                                .cinemaId(cinema.getId())
                                .name("Snack B")
                                .description("desc2")
                                .price(new java.math.BigDecimal("15000"))
                                .type("DRINK")
                                .build();

                given().contentType(ContentType.JSON).body(s1).when().post("/cinemas/snacks").then()
                                .statusCode(HttpStatus.CREATED.value());
                given().contentType(ContentType.JSON).body(s2).when().post("/cinemas/snacks").then()
                                .statusCode(HttpStatus.CREATED.value());

                given()
                                .when()
                                .get("/cinemas/snacks")
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .body("size()", greaterThanOrEqualTo(2))
                                .body("name", hasItems("Snack A", "Snack B"));
        }

        // ==================== Authorization Tests ====================

        @Test
        @DisplayName("Should deny access to user without ADMIN role")
        @WithMockUser(roles = "USER")
        void testCinemaOperations_Forbidden() {
                AddCinemaRequest request = AddCinemaRequest.builder()
                                .name("CGV Vincom")
                                .address("123 Nguyen Hue St, District 1")
                                .hotline("1900-6017")
                                .build();

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/cinemas")
                                .then()
                                .statusCode(HttpStatus.FORBIDDEN.value()); // @PreAuthorize("hasRole('ADMIN')") ->
                                                                           // AccessDeniedException
                                                                           // -> 403
        }
}
