package com.api.moviebooking.configs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.models.entities.Cinema;
import com.api.moviebooking.models.entities.MembershipTier;
import com.api.moviebooking.models.entities.Movie;
import com.api.moviebooking.models.entities.PriceBase;
import com.api.moviebooking.models.entities.PriceModifier;
import com.api.moviebooking.models.entities.Promotion;
import com.api.moviebooking.models.entities.Room;
import com.api.moviebooking.models.entities.Seat;
import com.api.moviebooking.models.entities.Showtime;
import com.api.moviebooking.models.entities.ShowtimeTicketType;
import com.api.moviebooking.models.entities.Snack;
import com.api.moviebooking.models.entities.TicketType;
import com.api.moviebooking.models.entities.User;
import com.api.moviebooking.models.enums.ConditionType;
import com.api.moviebooking.models.enums.DiscountType;
import com.api.moviebooking.models.enums.ModifierType;
import com.api.moviebooking.models.enums.MovieStatus;
import com.api.moviebooking.models.enums.SeatType;
import com.api.moviebooking.models.enums.UserRole;
import com.api.moviebooking.repositories.CinemaRepo;
import com.api.moviebooking.repositories.MembershipTierRepo;
import com.api.moviebooking.repositories.MovieRepo;
import com.api.moviebooking.repositories.PriceBaseRepo;
import com.api.moviebooking.repositories.PriceModifierRepo;
import com.api.moviebooking.repositories.PromotionRepo;
import com.api.moviebooking.repositories.RoomRepo;
import com.api.moviebooking.repositories.SeatRepo;
import com.api.moviebooking.repositories.ShowtimeRepo;
import com.api.moviebooking.repositories.ShowtimeSeatRepo;
import com.api.moviebooking.repositories.ShowtimeTicketTypeRepo;
import com.api.moviebooking.repositories.SnackRepo;
import com.api.moviebooking.repositories.TicketTypeRepo;
import com.api.moviebooking.repositories.UserRepo;
import com.api.moviebooking.services.ShowtimeSeatService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SeedDataConfig {

    private final UserRepo userRepo;
    private final MembershipTierRepo membershipTierRepo;
    private final PasswordEncoder passwordEncoder;

    private final CinemaRepo cinemaRepo;
    private final RoomRepo roomRepo;
    private final SeatRepo seatRepo;
    private final SnackRepo snackRepo;

    private final MovieRepo movieRepo;

    private final TicketTypeRepo ticketTypeRepo;
    private final PriceBaseRepo priceBaseRepo;
    private final PriceModifierRepo priceModifierRepo;
    private final PromotionRepo promotionRepo;

    private final ShowtimeRepo showtimeRepo;
    private final ShowtimeTicketTypeRepo showtimeTicketTypeRepo;
    private final ShowtimeSeatService showtimeSeatService;

    @PostConstruct
    @Transactional
    public void init() {
        log.info("Starting seed data initialization...");

        seedMembershipTiers();
        seedUsers();
        seedCinemasAndRoomsAndSeats();
        seedSnacks();
        seedMovies();
        seedPricingAndTicketTypes();
        seedPromotions();
        seedShowtimes(); // This will also generate showtime seats

        log.info("Seed data initialization completed.");
    }

    private void seedMembershipTiers() {
        if (membershipTierRepo.count() > 0)
            return;

        List<MembershipTier> tiers = new ArrayList<>();

        MembershipTier silver = new MembershipTier();
        silver.setName("SILVER");
        silver.setMinPoints(0);
        silver.setDiscountType(DiscountType.PERCENTAGE);
        silver.setDiscountValue(new BigDecimal("0.00"));
        silver.setDescription("Standard membership");
        tiers.add(silver);

        MembershipTier gold = new MembershipTier();
        gold.setName("GOLD");
        gold.setMinPoints(1000);
        gold.setDiscountType(DiscountType.PERCENTAGE);
        gold.setDiscountValue(new BigDecimal("5.00"));
        gold.setDescription("Gold membership with 5% discount");
        tiers.add(gold);

        MembershipTier platinum = new MembershipTier();
        platinum.setName("PLATINUM");
        platinum.setMinPoints(5000);
        platinum.setDiscountType(DiscountType.PERCENTAGE);
        platinum.setDiscountValue(new BigDecimal("10.00"));
        platinum.setDescription("Platinum membership with 10% discount");
        tiers.add(platinum);

        membershipTierRepo.saveAll(tiers);
        log.info("Seeded Membership Tiers");
    }

    private void seedUsers() {
        String adminEmail = "admin@gmail.com";
        if (userRepo.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setUsername("Admin User");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(UserRole.ADMIN);
            admin.setPhoneNumber("0901234567");
            admin.setLoyaltyPoints(0);

            // Assign highest tier for admin if available
            membershipTierRepo.findAll().stream()
                    .filter(t -> t.getName().equals("PLATINUM"))
                    .findFirst()
                    .ifPresent(admin::setMembershipTier);

            userRepo.save(admin);
            log.info("Seeded Admin User");
        }

        String userEmail = "hieuhoccode@gmail.com"; // Demo user
        if (userRepo.findByEmail(userEmail).isEmpty()) {
            User user = new User();
            user.setEmail(userEmail);
            user.setUsername("Hieu Hoc Code");
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRole(UserRole.USER);
            user.setPhoneNumber("0987654321");
            user.setLoyaltyPoints(500);

            membershipTierRepo.findAll().stream()
                    .filter(t -> t.getName().equals("SILVER"))
                    .findFirst()
                    .ifPresent(user::setMembershipTier);

            userRepo.save(user);
            log.info("Seeded Demo User");
        }
    }

    private void seedCinemasAndRoomsAndSeats() {
        if (cinemaRepo.count() > 0)
            return;

        Cinema cinema = new Cinema();
        cinema.setName("CGV Vincom Dong Khoi");
        cinema.setAddress("72 Le Thanh Ton, Ben Nghe Ward, District 1, HCMC");
        cinema.setHotline("1900 6017");
        cinema = cinemaRepo.save(cinema);

        // Room 1: Standard
        Room room1 = new Room();
        room1.setCinema(cinema);
        room1.setRoomNumber(1);
        room1.setRoomType("STANDARD");
        room1 = roomRepo.save(room1);
        generateSeatsForRoom(room1, 8, 10); // 8 rows, 10 cols

        // Room 2: IMAX
        Room room2 = new Room();
        room2.setCinema(cinema);
        room2.setRoomNumber(2);
        room2.setRoomType("IMAX");
        room2 = roomRepo.save(room2);
        generateSeatsForRoom(room2, 10, 12); // 10 rows, 12 cols

        log.info("Seeded Cinemas, Rooms, and Seats");
    }

    private void generateSeatsForRoom(Room room, int rows, int cols) {
        List<Seat> seats = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            char rowChar = (char) ('A' + r);
            for (int c = 1; c <= cols; c++) {
                Seat seat = new Seat();
                seat.setRoom(room);
                seat.setRowLabel(String.valueOf(rowChar));
                seat.setSeatNumber(c);

                // VIP seats in the middle rows
                if (r >= rows / 3 && r < rows - rows / 3) {
                    seat.setSeatType(SeatType.VIP);
                } else {
                    seat.setSeatType(SeatType.NORMAL);
                }

                // Couple seats in the last row
                if (r == rows - 1) {
                    seat.setSeatType(SeatType.COUPLE);
                }

                seats.add(seat);
            }
        }
        seatRepo.saveAll(seats);
    }

    private void seedSnacks() {
        if (snackRepo.count() > 0)
            return;

        Cinema cinema = cinemaRepo.findAll().get(0);
        List<Snack> snacks = new ArrayList<>();

        Snack popcorn = new Snack();
        popcorn.setCinema(cinema);
        popcorn.setName("Sweet Popcorn");
        popcorn.setDescription("Large sweet popcorn");
        popcorn.setPrice(new BigDecimal("79000"));
        popcorn.setType("Food");
        popcorn.setImageUrl("https://cinestar.com.vn/pictures/0330-bap-ngot-60oz.png");
        snacks.add(popcorn);

        Snack soda = new Snack();
        soda.setCinema(cinema);
        soda.setName("Pepsi Large");
        soda.setDescription("Large Pepsi 32oz");
        soda.setPrice(new BigDecimal("39000"));
        soda.setType("Drink");
        soda.setImageUrl("https://cinestar.com.vn/pictures/0330-pepsi-32oz.png");
        snacks.add(soda);

        Snack combo = new Snack();
        combo.setCinema(cinema);
        combo.setName("Combo Solo");
        combo.setDescription("1 Popcorn + 1 Drink");
        combo.setPrice(new BigDecimal("99000"));
        combo.setType("Combo");
        combo.setImageUrl("https://cinestar.com.vn/pictures/combo-solo-02.png");
        snacks.add(combo);

        snackRepo.saveAll(snacks);
        log.info("Seeded Snacks");
    }

    private void seedMovies() {
        if (movieRepo.count() > 0)
            return;

        List<Movie> movies = new ArrayList<>();

        Movie m1 = new Movie();
        m1.setTitle("Avatar: The Way of Water");
        m1.setGenre("Sci-Fi, Action");
        m1.setDuration(192);
        m1.setDescription(
                "Jake Sully lives with his newfound family formed on the extrasolar moon Pandora. Once a familiar threat returns to finish what was previously started, Jake must work with Neytiri and the army of the Na'vi race to protect their home.");
        m1.setDirector("James Cameron");
        m1.setActors("Sam Worthington, Zoe Saldana, Sigourney Weaver");
        m1.setLanguage("English");
        m1.setMinimumAge(13);
        m1.setPosterUrl(
                "https://m.media-amazon.com/images/M/MV5BYjhiNjBlODctY2ZiOC00YjVlLWFlNzAtNTVhNzM1YjI1NzMxXkEyXkFqcGdeQXVyMjQxNTE1MDA@._V1_.jpg");
        m1.setTrailerUrl("https://www.youtube.com/watch?v=d9MyW72ELq0");
        m1.setStatus(MovieStatus.SHOWING);
        movies.add(m1);

        Movie m2 = new Movie();
        m2.setTitle("The Batman");
        m2.setGenre("Action, Crime");
        m2.setDuration(176);
        m2.setDescription(
                "When a sadistic serial killer begins murdering key political figures in Gotham, Batman is forced to investigate the city's hidden corruption and question his family's involvement.");
        m2.setDirector("Matt Reeves");
        m2.setActors("Robert Pattinson, Zoë Kravitz, Jeffrey Wright");
        m2.setLanguage("English");
        m2.setMinimumAge(16);
        m2.setPosterUrl(
                "https://m.media-amazon.com/images/M/MV5BMDdmMTBiNTYtMDIzNi00NGVlLWIzMDYtZTk3MTQ3NGQxZGEwXkEyXkFqcGdeQXVyMzMwOTU5MDk@._V1_.jpg");
        m2.setTrailerUrl("https://www.youtube.com/watch?v=mqqft2x_Aa4");
        m2.setStatus(MovieStatus.SHOWING);
        movies.add(m2);

        Movie m3 = new Movie();
        m3.setTitle("Dune: Part Two");
        m3.setGenre("Sci-Fi, Adventure");
        m3.setDuration(166);
        m3.setDescription(
                "Paul Atreides unites with Chani and the Fremen while on a warpath of revenge against the conspirators who destroyed his family.");
        m3.setDirector("Denis Villeneuve");
        m3.setActors("Timothée Chalamet, Zendaya, Rebecca Ferguson");
        m3.setLanguage("English");
        m3.setMinimumAge(13);
        m3.setPosterUrl(
                "https://m.media-amazon.com/images/M/MV5BODdjMjM0ODktOTE2Yi00ODc3LWFjMjYtMGJlZTZkMjQ4OTQyXkEyXkFqcGdeQXVyMTAxNzQ1NzI@._V1_.jpg");
        m3.setTrailerUrl("https://www.youtube.com/watch?v=Way9Dexny3w");
        m3.setStatus(MovieStatus.UPCOMING);
        movies.add(m3);

        movieRepo.saveAll(movies);
        log.info("Seeded Movies");
    }

    private void seedPricingAndTicketTypes() {
        if (ticketTypeRepo.count() > 0)
            return;

        // Ticket Types
        List<TicketType> types = new ArrayList<>();

        TicketType adult = new TicketType();
        adult.setCode("ADULT");
        adult.setLabel("Adult");
        adult.setModifierType(ModifierType.FIXED_AMOUNT);
        adult.setModifierValue(BigDecimal.ZERO); // Base price
        adult.setSortOrder(1);
        types.add(adult);

        TicketType student = new TicketType();
        student.setCode("STUDENT");
        student.setLabel("Student / Senior");
        student.setModifierType(ModifierType.FIXED_AMOUNT);
        student.setModifierValue(new BigDecimal("-20000")); // -20k discount
        student.setSortOrder(2);
        types.add(student);

        ticketTypeRepo.saveAll(types);

        // Price Base
        if (priceBaseRepo.count() == 0) {
            PriceBase base = new PriceBase();
            base.setName("Standard Ticket Price");
            base.setBasePrice(new BigDecimal("90000")); // 90k base
            priceBaseRepo.save(base);
        }

        // Price Modifiers
        if (priceModifierRepo.count() == 0) {
            List<PriceModifier> modifiers = new ArrayList<>();

            PriceModifier weekend = new PriceModifier();
            weekend.setName("Weekend Surcharge");
            weekend.setConditionType(ConditionType.DAY_TYPE);
            weekend.setConditionValue("WEEKEND");
            weekend.setModifierType(ModifierType.FIXED_AMOUNT);
            weekend.setModifierValue(new BigDecimal("10000")); // +10k
            modifiers.add(weekend);

            PriceModifier vipSeat = new PriceModifier();
            vipSeat.setName("VIP Seat Surcharge");
            vipSeat.setConditionType(ConditionType.SEAT_TYPE);
            vipSeat.setConditionValue("VIP");
            vipSeat.setModifierType(ModifierType.FIXED_AMOUNT);
            vipSeat.setModifierValue(new BigDecimal("20000")); // +20k
            modifiers.add(vipSeat);

            PriceModifier coupleSeat = new PriceModifier();
            coupleSeat.setName("Couple Seat Surcharge");
            coupleSeat.setConditionType(ConditionType.SEAT_TYPE);
            coupleSeat.setConditionValue("COUPLE");
            coupleSeat.setModifierType(ModifierType.FIXED_AMOUNT);
            coupleSeat.setModifierValue(new BigDecimal("50000")); // +50k
            modifiers.add(coupleSeat);

            priceModifierRepo.saveAll(modifiers);
        }

        log.info("Seeded Pricing Configs");
    }

    private void seedPromotions() {
        if (promotionRepo.count() > 0)
            return;

        Promotion p1 = new Promotion();
        p1.setCode("SUMMER2025");
        p1.setName("Summer Sale");
        p1.setDescription("Get 20% off for all tickets");
        p1.setDiscountType(DiscountType.PERCENTAGE);
        p1.setDiscountValue(new BigDecimal("20.00"));
        p1.setStartDate(LocalDateTime.now().minusDays(1));
        p1.setEndDate(LocalDateTime.now().plusMonths(3));
        p1.setUsageLimit(100);

        promotionRepo.save(p1);
        log.info("Seeded Promotions");
    }

    private void seedShowtimes() {
        if (showtimeRepo.count() > 0)
            return;

        List<Movie> movies = movieRepo.findAll();
        List<Room> rooms = roomRepo.findAll();
        List<TicketType> ticketTypes = ticketTypeRepo.findAll();

        if (movies.isEmpty() || rooms.isEmpty())
            return;

        // Create showtimes for the next 7 days
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

        for (int i = 0; i < 5; i++) { // Create 5 showtimes
            Showtime showtime = new Showtime();

            Movie movie = movies.get(i % movies.size());
            Room room = rooms.get(i % rooms.size());

            showtime.setMovie(movie);
            showtime.setRoom(room);
            showtime.setFormat(room.getRoomType().equals("IMAX") ? "IMAX 2D" : "2D");
            showtime.setStartTime(start.plusHours(i * 3)); // Stagger showtimes

            showtime = showtimeRepo.save(showtime);

            // Generate seats
            showtimeSeatService.generateShowtimeSeats(showtime.getId());

            // Link ticket types
            for (TicketType tt : ticketTypes) {
                ShowtimeTicketType stTt = new ShowtimeTicketType();
                stTt.setShowtime(showtime);
                stTt.setTicketType(tt);
                stTt.setActive(true);
                showtimeTicketTypeRepo.save(stTt);
            }
        }

        log.info("Seeded Showtimes");
    }
}
