package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.helpers.utils.MappingUtils;
import com.api.moviebooking.models.dtos.booking.BookingResponse;
import com.api.moviebooking.models.entities.Booking;
import com.api.moviebooking.models.entities.BookingSeat;

@Mapper(componentModel = "spring", uses = MappingUtils.class)
public interface BookingMapper {

    @Mapping(target = "bookingId", source = "id")
    @Mapping(target = "showtimeId", source = "showtime.id")
    @Mapping(target = "movieTitle", source = "showtime.movie.title")
    @Mapping(target = "showtimeStartTime", source = "showtime.startTime")
    @Mapping(target = "cinemaName", source = "showtime.room.cinema.name")
    @Mapping(target = "roomName", expression = "java(formatRoomName(booking))")
    @Mapping(target = "seats", source = "bookingSeats")
    @Mapping(target = "totalPrice", source = "totalPrice")
    @Mapping(target = "discountReason", source = "discountReason")
    @Mapping(target = "discountValue", source = "discountValue")
    @Mapping(target = "finalPrice", source = "finalPrice")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "bookedAt", source = "bookedAt")
    @Mapping(target = "qrCode", source = "qrCode")
    @Mapping(target = "qrPayload", source = "qrPayload")
    @Mapping(target = "paymentExpiresAt", source = "paymentExpiresAt")
    BookingResponse toBookingResponse(Booking booking);

    @Mapping(target = "rowLabel", source = "showtimeSeat.seat.rowLabel")
    @Mapping(target = "seatNumber", source = "showtimeSeat.seat.seatNumber")
    @Mapping(target = "seatType", source = "showtimeSeat.seat.seatType")
    @Mapping(target = "price", source = "priceFinal")
    BookingResponse.SeatDetail toSeatDetail(BookingSeat bookingSeat);

    default String formatRoomName(Booking booking) {
        if (booking == null || booking.getShowtime() == null || booking.getShowtime().getRoom() == null) {
            return null;
        }
        var room = booking.getShowtime().getRoom();
        return "Room " + room.getRoomNumber() + " (" + room.getRoomType() + ")";
    }
}
