package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.models.dtos.seat.AddSeatRequest;
import com.api.moviebooking.models.dtos.seat.SeatDataResponse;
import com.api.moviebooking.models.entities.Seat;

@Mapper(componentModel = "spring")
public interface SeatMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "showtimeSeats", ignore = true)
    Seat toEntity(AddSeatRequest request);

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomNumber", expression = "java(String.valueOf(seat.getRoom().getRoomNumber()))")
    @Mapping(target = "cinemaName", source = "room.cinema.name")
    SeatDataResponse toDataResponse(Seat seat);
}
