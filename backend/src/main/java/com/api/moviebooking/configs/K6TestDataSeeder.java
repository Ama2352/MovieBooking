package com.api.moviebooking.configs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.models.entities.Cinema;
import com.api.moviebooking.models.entities.Movie;
import com.api.moviebooking.models.entities.PriceBase;
import com.api.moviebooking.models.entities.Room;
import com.api.moviebooking.models.entities.Seat;
import com.api.moviebooking.models.entities.Showtime;
import com.api.moviebooking.models.entities.ShowtimeSeat;
import com.api.moviebooking.models.entities.ShowtimeTicketType;
import com.api.moviebooking.models.entities.TicketType;
import com.api.moviebooking.models.enums.ModifierType;
import com.api.moviebooking.models.enums.MovieStatus;
import com.api.moviebooking.models.enums.SeatStatus;
import com.api.moviebooking.models.enums.SeatType;
import com.api.moviebooking.models.entities.User;
import com.api.moviebooking.models.enums.UserRole;
import com.api.moviebooking.repositories.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * K6 Load Testing Data Seeder
 * =============================================================================
 * Seeds test data required for k6 performance testing scenarios.
 * 
 * This seeder creates entities using unique business keys (name, code, title)
 * and lets JPA auto-generate the UUIDs. The k6 tests fetch IDs dynamically
 * from the API endpoints.
 * 
 * ACTIVATION:
 * Set environment variable: K6_SEED_ENABLED=true
 * 
 * Usage:
 * - Local (Docker Compose): Add K6_SEED_ENABLED=true to docker-compose.yml
 * - Azure VM: Set K6_SEED_ENABLED=true in environment
 * 
 * Test Data Created:
 * - Cinema: "K6 Test Cinema"
 * - Room: Room 99 (IMAX) with 100 seats (10x10)
 * - Movie: "K6 Performance Test Movie" (status: SHOWING)
 * - 3 Showtimes with 100 available seats each
 * - 3 Ticket Types: k6_adult, k6_student, k6_senior
 * =============================================================================
 */
@Component
@ConditionalOnProperty(name = "k6.seed.enabled", havingValue = "true")
@Order(10) // Run after other seeders
@RequiredArgsConstructor
@Slf4j
public class K6TestDataSeeder implements CommandLineRunner {

    private final CinemaRepo cinemaRepo;
    private final RoomRepo roomRepo;
    private final SeatRepo seatRepo;
    private final MovieRepo movieRepo;
    private final ShowtimeRepo showtimeRepo;
    private final ShowtimeSeatRepo showtimeSeatRepo;
    private final TicketTypeRepo ticketTypeRepo;
    private final ShowtimeTicketTypeRepo showtimeTicketTypeRepo;
    private final PriceBaseRepo priceBaseRepo;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final MembershipTierRepo membershipTierRepo;

    // Unique business keys for K6 test data
    private static final String K6_CINEMA_NAME = "K6 Test Cinema";
    private static final String K6_MOVIE_TITLE = "K6 Performance Test Movie";
    private static final int K6_ROOM_NUMBER = 99; // Use high number to avoid conflicts

    // Ticket type codes (these are unique in the system)
    private static final String TICKET_CODE_ADULT = "k6_adult";
    private static final String TICKET_CODE_STUDENT = "k6_student";
    private static final String TICKET_CODE_SENIOR = "k6_senior";

    @Value("${k6.seed.seats-per-row:10}")
    private int seatsPerRow;

    @Value("${k6.seed.rows:10}")
    private int numberOfRows;

    // Store created entities for logging
    private Cinema createdCinema;
    private Room createdRoom;
    private Movie createdMovie;
    private List<Showtime> createdShowtimes = new ArrayList<>();
    private List<TicketType> createdTicketTypes = new ArrayList<>();

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=".repeat(60));
        log.info("🚀 K6 TEST DATA SEEDER - Starting...");
        log.info("=".repeat(60));

        try {
            // Seed base price first (required for ticket price calculations)
            seedPriceBase();

            // Seed Admin User
            seedAdminUser();

            createdTicketTypes = seedTicketTypes();
            createdCinema = seedCinema();
            createdRoom = seedRoom(createdCinema);
            List<Seat> seats = seedSeats(createdRoom);
            createdMovie = seedMovie();

            // Create multiple showtimes for testing
            Showtime showtime1 = seedShowtime(createdRoom, createdMovie, LocalDateTime.now().plusHours(2));
            Showtime showtime2 = seedShowtime(createdRoom, createdMovie, LocalDateTime.now().plusHours(5));
            Showtime showtime3 = seedShowtime(createdRoom, createdMovie, LocalDateTime.now().plusDays(1).plusHours(2));

            createdShowtimes.add(showtime1);
            createdShowtimes.add(showtime2);
            createdShowtimes.add(showtime3);

            seedShowtimeSeats(showtime1, seats);
            seedShowtimeSeats(showtime2, seats);
            seedShowtimeSeats(showtime3, seats);

            seedShowtimeTicketTypes(showtime1, createdTicketTypes);
            seedShowtimeTicketTypes(showtime2, createdTicketTypes);
            seedShowtimeTicketTypes(showtime3, createdTicketTypes);

            printSummary(seats.size());
        } catch (Exception e) {
            log.error("❌ Failed to seed K6 test data: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void seedPriceBase() {
        log.info("💰 Checking base price configuration...");

        // Check if an active base price already exists
        if (priceBaseRepo.findActiveBasePrice().isPresent()) {
            log.info("   ℹ️ Active base price already exists, skipping...");
            return;
        }

        // Create a base price for K6 testing
        PriceBase priceBase = new PriceBase();
        priceBase.setName("K6 Test Base Price");
        priceBase.setBasePrice(BigDecimal.valueOf(90000)); // 90,000 VND
        priceBase.setIsActive(true);

        priceBaseRepo.save(priceBase);
        log.info("   ✅ Created base price: {} VND", priceBase.getBasePrice());
    }

    private void seedAdminUser() {
        log.info("👤 Seeding Admin User...");
        String adminEmail = "admin@k6test.com";

        if (userRepo.findByEmail(adminEmail).isPresent()) {
            log.info("   ℹ️ Admin user already exists, skipping...");
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setUsername("K6 Admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(UserRole.ADMIN);
        admin.setPhoneNumber("0909998887");
        admin.setLoyaltyPoints(9999);

        // Assign PLATINUM tier if available, else null (will be handled by system
        // defaults usually)
        membershipTierRepo.findAll().stream()
                .filter(t -> t.getName().equalsIgnoreCase("PLATINUM"))
                .findFirst()
                .ifPresent(admin::setMembershipTier);

        userRepo.save(admin);
        log.info("   ✅ Created Admin User: {}", adminEmail);
    }

    private List<TicketType> seedTicketTypes() {
        log.info("📋 Seeding ticket types...");
        List<TicketType> ticketTypes = new ArrayList<>();

        // Adult ticket
        TicketType adult = ticketTypeRepo.findByCode(TICKET_CODE_ADULT).orElseGet(() -> {
            TicketType tt = new TicketType();
            tt.setCode(TICKET_CODE_ADULT);
            tt.setLabel("K6 NGƯỜI LỚN");
            tt.setModifierType(ModifierType.PERCENTAGE);
            tt.setModifierValue(BigDecimal.valueOf(1.0));
            tt.setActive(true);
            tt.setSortOrder(100);
            TicketType saved = ticketTypeRepo.save(tt);
            log.info("   ✅ Created ticket type: {} (ID: {})", saved.getCode(), saved.getId());
            return saved;
        });
        ticketTypes.add(adult);

        // Student ticket
        TicketType student = ticketTypeRepo.findByCode(TICKET_CODE_STUDENT).orElseGet(() -> {
            TicketType tt = new TicketType();
            tt.setCode(TICKET_CODE_STUDENT);
            tt.setLabel("K6 HSSV/U22");
            tt.setModifierType(ModifierType.PERCENTAGE);
            tt.setModifierValue(BigDecimal.valueOf(0.8));
            tt.setActive(true);
            tt.setSortOrder(101);
            TicketType saved = ticketTypeRepo.save(tt);
            log.info("   ✅ Created ticket type: {} (ID: {})", saved.getCode(), saved.getId());
            return saved;
        });
        ticketTypes.add(student);

        // Senior ticket
        TicketType senior = ticketTypeRepo.findByCode(TICKET_CODE_SENIOR).orElseGet(() -> {
            TicketType tt = new TicketType();
            tt.setCode(TICKET_CODE_SENIOR);
            tt.setLabel("K6 NGƯỜI CAO TUỔI");
            tt.setModifierType(ModifierType.PERCENTAGE);
            tt.setModifierValue(BigDecimal.valueOf(0.7));
            tt.setActive(true);
            tt.setSortOrder(102);
            TicketType saved = ticketTypeRepo.save(tt);
            log.info("   ✅ Created ticket type: {} (ID: {})", saved.getCode(), saved.getId());
            return saved;
        });
        ticketTypes.add(senior);

        log.info("   ℹ️ Total ticket types ready: {}", ticketTypes.size());
        return ticketTypes;
    }

    private Cinema seedCinema() {
        log.info("🏢 Seeding cinema...");

        return cinemaRepo.findByName(K6_CINEMA_NAME).orElseGet(() -> {
            Cinema cinema = new Cinema();
            cinema.setName(K6_CINEMA_NAME);
            cinema.setAddress("123 Load Test Street, Performance City");
            cinema.setHotline("1900-K6-TEST");
            Cinema saved = cinemaRepo.save(cinema);
            log.info("   ✅ Created cinema: {} (ID: {})", saved.getName(), saved.getId());
            return saved;
        });
    }

    private Room seedRoom(Cinema cinema) {
        log.info("🎬 Seeding room...");

        return roomRepo.findByCinemaIdAndRoomNumber(cinema.getId(), K6_ROOM_NUMBER).orElseGet(() -> {
            Room room = new Room();
            room.setCinema(cinema);
            room.setRoomNumber(K6_ROOM_NUMBER);
            room.setRoomType("IMAX");
            Room saved = roomRepo.save(room);
            log.info("   ✅ Created room: Room {} ({}) (ID: {})", saved.getRoomNumber(), saved.getRoomType(),
                    saved.getId());
            return saved;
        });
    }

    private List<Seat> seedSeats(Room room) {
        log.info("💺 Seeding seats ({} rows x {} seats)...", numberOfRows, seatsPerRow);

        // Check if seats already exist for this room
        List<Seat> existingSeats = seatRepo.findAll().stream()
                .filter(s -> s.getRoom().getId().equals(room.getId()))
                .toList();

        if (!existingSeats.isEmpty()) {
            log.info("   ℹ️ Seats already exist ({}), skipping creation...", existingSeats.size());
            return existingSeats;
        }

        List<Seat> seats = new ArrayList<>();
        String[] rowLabels = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L" };

        for (int row = 0; row < numberOfRows && row < rowLabels.length; row++) {
            for (int seatNum = 1; seatNum <= seatsPerRow; seatNum++) {
                Seat seat = new Seat();
                seat.setRoom(room);
                seat.setRowLabel(rowLabels[row]);
                seat.setSeatNumber(seatNum);

                // Assign seat types: back rows are VIP, couple seats at ends
                if (row >= numberOfRows - 2) {
                    seat.setSeatType(SeatType.VIP);
                } else if (seatNum <= 2 || seatNum >= seatsPerRow - 1) {
                    seat.setSeatType(SeatType.COUPLE);
                } else {
                    seat.setSeatType(SeatType.NORMAL);
                }

                seats.add(seatRepo.save(seat));
            }
        }

        log.info("   ✅ Created {} seats", seats.size());
        return seats;
    }

    private Movie seedMovie() {
        log.info("🎥 Seeding movie...");

        // Try to find existing movie by title
        List<Movie> existingMovies = movieRepo.findByTitleContainingIgnoreCase(K6_MOVIE_TITLE);
        if (!existingMovies.isEmpty()) {
            Movie existing = existingMovies.get(0);
            // Ensure it's SHOWING status
            if (existing.getStatus() != MovieStatus.SHOWING) {
                existing.setStatus(MovieStatus.SHOWING);
                existing = movieRepo.save(existing);
            }
            log.info("   ℹ️ Movie already exists: {} (ID: {})", existing.getTitle(), existing.getId());
            return existing;
        }

        Movie movie = new Movie();
        movie.setTitle(K6_MOVIE_TITLE);
        movie.setGenre("Action, Thriller");
        movie.setDescription("A thrilling movie created for k6 load testing. " +
                "Watch as the system handles thousands of concurrent requests!");
        movie.setDuration(120);
        movie.setMinimumAge(13);
        movie.setDirector("Load Tester");
        movie.setActors("Virtual User 1, Virtual User 2, Virtual User 3");
        movie.setPosterUrl("https://via.placeholder.com/300x450?text=K6+Test+Movie");
        movie.setTrailerUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        movie.setStatus(MovieStatus.SHOWING);
        movie.setLanguage("English");
        Movie saved = movieRepo.save(movie);
        log.info("   ✅ Created movie: {} (ID: {})", saved.getTitle(), saved.getId());
        return saved;
    }

    private Showtime seedShowtime(Room room, Movie movie, LocalDateTime startTime) {
        log.info("⏰ Seeding showtime at {}...", startTime);

        // Check if a showtime already exists for this room/movie/time (within 1 hour)
        List<Showtime> existingShowtimes = showtimeRepo.findByMovieId(movie.getId()).stream()
                .filter(s -> s.getRoom().getId().equals(room.getId()))
                .filter(s -> Math.abs(java.time.Duration.between(s.getStartTime(), startTime).toMinutes()) < 60)
                .toList();

        if (!existingShowtimes.isEmpty()) {
            Showtime existing = existingShowtimes.get(0);
            log.info("   ℹ️ Showtime already exists: {} (ID: {})", existing.getStartTime(), existing.getId());
            return existing;
        }

        Showtime showtime = new Showtime();
        showtime.setRoom(room);
        showtime.setMovie(movie);
        showtime.setFormat("2D Subtitled");
        showtime.setStartTime(startTime);
        Showtime saved = showtimeRepo.save(showtime);
        log.info("   ✅ Created showtime: {} (ID: {})", saved.getStartTime(), saved.getId());
        return saved;
    }

    private void seedShowtimeSeats(Showtime showtime, List<Seat> seats) {
        log.info("🎫 Seeding showtime seats for showtime: {}...", showtime.getId());

        // Check if showtime seats already exist
        List<ShowtimeSeat> existingSeats = showtimeSeatRepo.findByShowtimeId(showtime.getId());
        if (!existingSeats.isEmpty()) {
            // Reset all seats to AVAILABLE for fresh testing
            log.info("   🔄 Resetting {} existing seats to AVAILABLE...", existingSeats.size());
            for (ShowtimeSeat ss : existingSeats) {
                ss.setStatus(SeatStatus.AVAILABLE);
            }
            showtimeSeatRepo.saveAll(existingSeats);
            return;
        }

        List<ShowtimeSeat> showtimeSeats = new ArrayList<>();
        BigDecimal basePrice = BigDecimal.valueOf(90000); // Base price: 90,000 VND

        for (Seat seat : seats) {
            ShowtimeSeat showtimeSeat = new ShowtimeSeat();
            showtimeSeat.setShowtime(showtime);
            showtimeSeat.setSeat(seat);
            showtimeSeat.setStatus(SeatStatus.AVAILABLE);

            // Set price based on seat type
            BigDecimal price = switch (seat.getSeatType()) {
                case VIP -> basePrice.multiply(BigDecimal.valueOf(1.5));
                case COUPLE -> basePrice.multiply(BigDecimal.valueOf(2.0));
                default -> basePrice;
            };
            showtimeSeat.setPrice(price);

            // Price breakdown JSON
            showtimeSeat.setPriceBreakdown(String.format(
                    "{\"basePrice\": %.0f, \"seatType\": \"%s\", \"multiplier\": %.1f}",
                    basePrice.doubleValue(),
                    seat.getSeatType().name(),
                    price.divide(basePrice, 1, java.math.RoundingMode.HALF_UP).doubleValue()));

            showtimeSeats.add(showtimeSeat);
        }

        showtimeSeatRepo.saveAll(showtimeSeats);
        log.info("   ✅ Created {} showtime seats (all AVAILABLE)", showtimeSeats.size());
    }

    private void seedShowtimeTicketTypes(Showtime showtime, List<TicketType> ticketTypes) {
        log.info("🎟️ Seeding showtime ticket types for showtime: {}...", showtime.getId());

        List<ShowtimeTicketType> existing = showtimeTicketTypeRepo.findAllByShowtime(showtime.getId());
        if (!existing.isEmpty()) {
            log.info("   ℹ️ Showtime ticket types already exist ({}), skipping...", existing.size());
            return;
        }

        for (TicketType ticketType : ticketTypes) {
            ShowtimeTicketType stt = new ShowtimeTicketType();
            stt.setShowtime(showtime);
            stt.setTicketType(ticketType);
            stt.setActive(true);
            showtimeTicketTypeRepo.save(stt);
        }

        log.info("   ✅ Linked {} ticket types to showtime", ticketTypes.size());
    }

    private void printSummary(int totalSeats) {
        log.info("");
        log.info("=".repeat(60));
        log.info("✅ K6 TEST DATA SEEDING COMPLETE!");
        log.info("=".repeat(60));
        log.info("");
        log.info("📋 Test Data Summary (IDs are auto-generated):");
        log.info("   Cinema:    {} (ID: {})", createdCinema.getName(), createdCinema.getId());
        log.info("   Room:      Room {} (ID: {})", createdRoom.getRoomNumber(), createdRoom.getId());
        log.info("   Movie:     {} (ID: {})", createdMovie.getTitle(), createdMovie.getId());
        log.info("   Showtimes:");
        for (int i = 0; i < createdShowtimes.size(); i++) {
            Showtime st = createdShowtimes.get(i);
            log.info("     [{}] {} (ID: {})", i + 1, st.getStartTime(), st.getId());
        }
        log.info("   Total Seats: {} (per showtime)", totalSeats);
        log.info("   Ticket Types:");
        for (TicketType tt : createdTicketTypes) {
            log.info("     - {} (ID: {})", tt.getCode(), tt.getId());
        }
        log.info("");
        log.info("🧪 k6 tests should fetch data dynamically from API:");
        log.info("   GET /movies                    → Find movie by title");
        log.info("   GET /showtimes/movie/{movieId} → Get showtimes");
        log.info("   GET /showtime-seats/showtime/{id}/available → Get seats");
        log.info("");
        log.info("=".repeat(60));
    }
}
