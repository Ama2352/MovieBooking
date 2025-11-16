package com.api.moviebooking.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.mapstructs.SeatMapper;
import com.api.moviebooking.models.dtos.seat.AddSeatRequest;
import com.api.moviebooking.models.dtos.seat.BulkSeatResponse;
import com.api.moviebooking.models.dtos.seat.GenerateSeatsRequest;
import com.api.moviebooking.models.dtos.seat.RowLabelsResponse;
import com.api.moviebooking.models.dtos.seat.SeatDataResponse;
import com.api.moviebooking.models.dtos.seat.UpdateSeatRequest;
import com.api.moviebooking.models.entities.Room;
import com.api.moviebooking.models.entities.Seat;
import com.api.moviebooking.models.enums.SeatType;
import com.api.moviebooking.repositories.RoomRepo;
import com.api.moviebooking.repositories.SeatRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepo seatRepo;
    private final RoomRepo roomRepo;
    private final SeatMapper seatMapper;

    private Seat findSeatById(UUID seatId) {
        return seatRepo.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", "id", seatId));
    }

    @Transactional
    public SeatDataResponse addSeat(AddSeatRequest request) {
        // Validate room exists
        Room room = roomRepo.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", request.getRoomId()));

        // Check if seat already exists in this room
        boolean seatExists = room.getSeats().stream()
                .anyMatch(s -> s.getRowLabel().equalsIgnoreCase(request.getRowLabel())
                        && s.getSeatNumber() == request.getSeatNumber());

        if (seatExists) {
            throw new IllegalArgumentException(
                    "Seat " + request.getRowLabel() + request.getSeatNumber() + " already exists in this room");
        }

        Seat newSeat = seatMapper.toEntity(request);
        newSeat.setRoom(room);
        seatRepo.save(newSeat);

        return seatMapper.toDataResponse(newSeat);
    }

    @Transactional
    public SeatDataResponse updateSeat(UUID seatId, UpdateSeatRequest request) {
        Seat seat = findSeatById(seatId);

        if (request.getSeatNumber() != null) {
            seat.setSeatNumber(request.getSeatNumber());
        }

        if (request.getRowLabel() != null) {
            seat.setRowLabel(request.getRowLabel());
        }

        if (request.getSeatType() != null) {
            SeatType newType = SeatType.valueOf(request.getSeatType());
            seat.setSeatType(newType);
        }

        seatRepo.save(seat);
        return seatMapper.toDataResponse(seat);
    }

    @Transactional
    public void deleteSeat(UUID seatId) {
        Seat seat = findSeatById(seatId);

        // Check if seat is being used in any showtimes
        if (!seat.getShowtimeSeats().isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete seat that is being used in showtimes. Remove or reassign showtimes first.");
        }

        seatRepo.delete(seat);
    }

    public SeatDataResponse getSeat(UUID seatId) {
        Seat seat = findSeatById(seatId);
        return seatMapper.toDataResponse(seat);
    }

    public List<SeatDataResponse> getAllSeats() {
        return seatRepo.findAll().stream()
                .map(seatMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    public List<SeatDataResponse> getSeatsByRoom(UUID roomId) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        return room.getSeats().stream()
                .map(seatMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BulkSeatResponse generateSeats(GenerateSeatsRequest request) {
        // Validate room exists
        Room room = roomRepo.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", request.getRoomId()));

        // Check if room already has seats
        if (!room.getSeats().isEmpty()) {
            throw new IllegalStateException(
                    "Room already has seats. Please delete existing seats before generating new layout.");
        }

        // Generate row labels (A, B, C, ..., Z, AA, AB, ...)
        List<String> rowLabels = generateRowLabels(request.getRows());

        // Validate VIP rows exist in generated row labels
        if (request.getVipRows() != null) {
            for (String vipRow : request.getVipRows()) {
                if (!rowLabels.contains(vipRow)) {
                    throw new IllegalArgumentException(
                            String.format("VIP row '%s' does not exist. Available rows: %s",
                                    vipRow, String.join(", ", rowLabels)));
                }
            }
        }

        // Validate couple rows exist in generated row labels
        if (request.getCoupleRows() != null) {
            for (String coupleRow : request.getCoupleRows()) {
                if (!rowLabels.contains(coupleRow)) {
                    throw new IllegalArgumentException(
                            String.format("Couple row '%s' does not exist. Available rows: %s",
                                    coupleRow, String.join(", ", rowLabels)));
                }
            }
        }

        List<Seat> generatedSeats = new ArrayList<>();
        int normalCount = 0;
        int vipCount = 0;
        int coupleCount = 0;

        for (int rowIndex = 0; rowIndex < request.getRows(); rowIndex++) {
            String rowLabel = rowLabels.get(rowIndex);

            for (int seatNumber = 1; seatNumber <= request.getSeatsPerRow(); seatNumber++) {
                Seat seat = new Seat();
                seat.setRoom(room);
                seat.setRowLabel(rowLabel);
                seat.setSeatNumber(seatNumber);

                // Determine seat type based on row
                SeatType seatType = determineSeatType(
                        rowLabel,
                        request.getVipRows(),
                        request.getCoupleRows());

                seat.setSeatType(seatType);

                // Count by type
                switch (seatType) {
                    case VIP:
                        vipCount++;
                        break;
                    case COUPLE:
                        coupleCount++;
                        break;
                    default:
                        normalCount++;
                        break;
                }

                generatedSeats.add(seat);
            }
        }

        // Save all seats
        List<Seat> savedSeats = seatRepo.saveAll(generatedSeats);

        // Convert to response
        List<SeatDataResponse> seatResponses = savedSeats.stream()
                .map(seatMapper::toDataResponse)
                .collect(Collectors.toList());

        BulkSeatResponse response = new BulkSeatResponse();
        response.setTotalSeatsGenerated(savedSeats.size());
        response.setNormalSeats(normalCount);
        response.setVipSeats(vipCount);
        response.setCoupleSeats(coupleCount);
        response.setSeats(seatResponses);

        return response;
    }

    /**
     * Generate row labels preview for frontend
     */
    public RowLabelsResponse getRowLabelsPreview(int numberOfRows) {
        if (numberOfRows < 1) {
            throw new IllegalArgumentException("Number of rows must be at least 1");
        }

        if (numberOfRows > 100) {
            throw new IllegalArgumentException("Number of rows cannot exceed 100");
        }

        List<String> labels = generateRowLabels(numberOfRows);

        RowLabelsResponse response = new RowLabelsResponse();
        response.setTotalRows(numberOfRows);
        response.setRowLabels(labels);

        return response;
    }

    /**
     * Generate row labels: A, B, C, ..., Z, AA, AB, AC, ...
     */
    private List<String> generateRowLabels(int numberOfRows) {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < numberOfRows; i++) {
            labels.add(getColumnLabel(i));
        }
        return labels;
    }

    /**
     * Convert number to Excel-style column label
     * 0 -> A, 1 -> B, ..., 25 -> Z, 26 -> AA, 27 -> AB, ...
     */
    private String getColumnLabel(int index) {
        StringBuilder label = new StringBuilder();
        index++; // Make it 1-based for easier calculation

        while (index > 0) {
            index--; // Adjust for 0-based modulo
            label.insert(0, (char) ('A' + (index % 26)));
            index /= 26;
        }

        return label.toString();
    }

    /**
     * Determine seat type based on row label
     */
    private SeatType determineSeatType(
            String rowLabel,
            List<String> vipRows,
            List<String> coupleRows) {

        if (coupleRows != null && coupleRows.stream()
                .anyMatch(row -> row.equalsIgnoreCase(rowLabel))) {
            return SeatType.COUPLE;
        }

        if (vipRows != null && vipRows.stream()
                .anyMatch(row -> row.equalsIgnoreCase(rowLabel))) {
            return SeatType.VIP;
        }

        return SeatType.NORMAL;
    }
}
