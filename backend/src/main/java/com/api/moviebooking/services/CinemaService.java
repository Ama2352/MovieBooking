package com.api.moviebooking.services;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.mapstructs.CinemaMapper;
import com.api.moviebooking.models.dtos.cinema.AddCinemaRequest;
import com.api.moviebooking.models.dtos.cinema.CinemaDataResponse;
import com.api.moviebooking.models.dtos.cinema.UpdateCinemaRequest;
import com.api.moviebooking.models.entities.Cinema;
import com.api.moviebooking.repositories.CinemaRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CinemaService {

    private final CinemaRepo cinemaRepo;
    private final CinemaMapper cinemaMapper;

    private Cinema findCinemaById(UUID cinemaId) {
        return cinemaRepo.findById(cinemaId)
                .orElseThrow(() -> new RuntimeException("Cinema not found"));
    }

    @Transactional
    public CinemaDataResponse addCinema(AddCinemaRequest request) {
        Cinema newCinema = cinemaMapper.toEntity(request);
        cinemaRepo.save(newCinema);
        return cinemaMapper.toDataResponse(newCinema);
    }

    @Transactional
    public CinemaDataResponse updateCinema(UUID cinemaId, UpdateCinemaRequest request) {
        Cinema cinema = findCinemaById(cinemaId);
        if (request.getName() != null) {
            cinema.setName(request.getName());
        }
        if (request.getAddress() != null) {
            cinema.setAddress(request.getAddress());
        }
        if (request.getHotline() != null) {
            cinema.setHotline(request.getHotline());
        }
        cinemaRepo.save(cinema);
        return cinemaMapper.toDataResponse(cinema);
    }

    @Transactional
    public void deleteCinema(UUID cinemaId) {
        Cinema cinema = findCinemaById(cinemaId);
        cinemaRepo.delete(cinema);
    }

    public CinemaDataResponse getCinema(UUID cinemaId) {
        Cinema cinema = findCinemaById(cinemaId);
        return cinemaMapper.toDataResponse(cinema);
    }
}
