package com.api.moviebooking.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.mapstructs.ShowtimeSeatMapper;
import com.api.moviebooking.models.dtos.showtimeSeat.ShowtimeSeatDataResponse;
import com.api.moviebooking.models.dtos.showtimeSeat.UpdateShowtimeSeatRequest;
import com.api.moviebooking.models.entities.Room;
import com.api.moviebooking.models.entities.Seat;
import com.api.moviebooking.models.entities.Showtime;
import com.api.moviebooking.models.entities.ShowtimeSeat;
import com.api.moviebooking.models.enums.SeatStatus;
import com.api.moviebooking.repositories.ShowtimeRepo;
import com.api.moviebooking.repositories.ShowtimeSeatRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeSeatService {

    private final ShowtimeSeatRepo showtimeSeatRepo;
    private final ShowtimeRepo showtimeRepo;
    private final ShowtimeSeatMapper showtimeSeatMapper;
    private final PriceCalculationService priceCalculationService;

    private ShowtimeSeat findShowtimeSeatById(UUID id) {
        return showtimeSeatRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShowtimeSeat", "id", id));
    }

    /**
     * Auto-generate showtime seats when a new showtime is created
     * This should be called by ShowtimeService after creating a showtime
     */
    @Transactional
    public List<ShowtimeSeatDataResponse> generateShowtimeSeats(UUID showtimeId) {
        Showtime showtime = showtimeRepo.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id", showtimeId));

        Room room = showtime.getRoom();
        List<Seat> roomSeats = room.getSeats();

        if (roomSeats.isEmpty()) {
            throw new IllegalStateException("Room has no seats. Please add seats to the room first.");
        }

        List<ShowtimeSeat> showtimeSeats = new ArrayList<>();

        for (Seat seat : roomSeats) {
            // Calculate price and get breakdown
            Object[] priceData = priceCalculationService.calculatePriceWithBreakdown(showtime, seat);
            BigDecimal calculatedPrice = (BigDecimal) priceData[0];
            String priceBreakdown = (String) priceData[1];

            ShowtimeSeat showtimeSeat = new ShowtimeSeat();
            showtimeSeat.setShowtime(showtime);
            showtimeSeat.setSeat(seat);
            showtimeSeat.setStatus(SeatStatus.AVAILABLE);
            showtimeSeat.setPrice(calculatedPrice);
            showtimeSeat.setPriceBreakdown(priceBreakdown);

            showtimeSeats.add(showtimeSeat);
        }

        List<ShowtimeSeat> savedSeats = showtimeSeatRepo.saveAll(showtimeSeats);

        log.info("Generated {} showtime seats for showtime {}", savedSeats.size(), showtimeId);

        return savedSeats.stream()
                .map(showtimeSeatMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update showtime seat (API: PUT /showtime-seats/{id})
     * Predicate nodes (d): 3 -> V(G) = d + 1 = 4
     * Nodes: findShowtimeSeatById, status!=null (with try-catch), price!=null
     */
    @Transactional
    public ShowtimeSeatDataResponse updateShowtimeSeat(UUID id, UpdateShowtimeSeatRequest request) {
        ShowtimeSeat showtimeSeat = findShowtimeSeatById(id);

        if (request.getStatus() != null) {
            try {
                SeatStatus newStatus = SeatStatus.valueOf(request.getStatus().toUpperCase());
                showtimeSeat.setStatus(newStatus);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid seat status: " + request.getStatus());
            }
        }

        if (request.getPrice() != null) {
            showtimeSeat.setPrice(request.getPrice());
        }

        showtimeSeatRepo.save(showtimeSeat);
        return showtimeSeatMapper.toDataResponse(showtimeSeat);
    }

    /**
     * Reset showtime seat status to AVAILABLE (API: PUT /showtime-seats/{id}/reset)
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findShowtimeSeatById
     */
    @Transactional
    public ShowtimeSeatDataResponse resetShowtimeSeatStatus(UUID id) {
        ShowtimeSeat showtimeSeat = findShowtimeSeatById(id);

        showtimeSeat.setStatus(SeatStatus.AVAILABLE);
        showtimeSeatRepo.save(showtimeSeat);

        log.info("Reset showtime seat {} to AVAILABLE", id);

        return showtimeSeatMapper.toDataResponse(showtimeSeat);
    }

    /**
     * Get showtime seat by ID (API: GET /showtime-seats/{id})
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findShowtimeSeatById
     */
    public ShowtimeSeatDataResponse getShowtimeSeat(UUID id) {
        ShowtimeSeat showtimeSeat = findShowtimeSeatById(id);
        return showtimeSeatMapper.toDataResponse(showtimeSeat);
    }

    /**
     * Get all seats for a showtime (API: GET /showtime-seats/showtime/{showtimeId})
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     * Nodes: none
     */
    public List<ShowtimeSeatDataResponse> getShowtimeSeatsByShowtime(UUID showtimeId) {
        List<ShowtimeSeat> seats = showtimeSeatRepo.findByShowtimeId(showtimeId);
        return seats.stream()
                .map(showtimeSeatMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get available seats for a showtime (API: GET
     * /showtime-seats/showtime/{showtimeId}/available)
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     * Nodes: none
     */
    public List<ShowtimeSeatDataResponse> getAvailableShowtimeSeats(UUID showtimeId) {
        List<ShowtimeSeat> seats = showtimeSeatRepo.findByShowtimeIdAndStatus(showtimeId, SeatStatus.AVAILABLE);
        return seats.stream()
                .map(showtimeSeatMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Recalculate prices for all seats in a showtime (API: POST
     * /showtime-seats/showtime/{showtimeId}/recalculate-prices)
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findById (for showtime)
     */
    @Transactional
    public List<ShowtimeSeatDataResponse> recalculatePrices(UUID showtimeId) {
        Showtime showtime = showtimeRepo.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id", showtimeId));

        List<ShowtimeSeat> showtimeSeats = showtimeSeatRepo.findByShowtimeId(showtimeId);

        for (ShowtimeSeat showtimeSeat : showtimeSeats) {
            Object[] priceData = priceCalculationService.calculatePriceWithBreakdown(showtime, showtimeSeat.getSeat());
            BigDecimal newPrice = (BigDecimal) priceData[0];
            String priceBreakdown = (String) priceData[1];

            showtimeSeat.setPrice(newPrice);
            showtimeSeat.setPriceBreakdown(priceBreakdown);
        }

        List<ShowtimeSeat> updatedSeats = showtimeSeatRepo.saveAll(showtimeSeats);

        log.info("Recalculated prices for {} seats in showtime {}", updatedSeats.size(), showtimeId);

        return updatedSeats.stream()
                .map(showtimeSeatMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete all showtime seats for a showtime
     * Used internally when deleting a showtime
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     * Nodes: none
     */
    @Transactional
    public void deleteShowtimeSeats(UUID showtimeId) {
        List<ShowtimeSeat> showtimeSeats = showtimeSeatRepo.findByShowtimeId(showtimeId);
        showtimeSeatRepo.deleteAll(showtimeSeats);
        log.info("Deleted {} showtime seats for showtime {}", showtimeSeats.size(), showtimeId);
    }
}
