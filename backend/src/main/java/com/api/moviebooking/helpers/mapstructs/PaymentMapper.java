package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.helpers.utils.MappingUtils;
import com.api.moviebooking.models.dtos.payment.PaymentResponse;
import com.api.moviebooking.models.entities.Payment;

@Mapper(componentModel = "spring", uses = { MappingUtils.class, BookingMapper.class })
public interface PaymentMapper {

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "showtimeId", source = "booking.showtime.id")
    @Mapping(target = "movieTitle", source = "booking.showtime.movie.title")
    @Mapping(target = "showtimeStartTime", source = "booking.showtime.startTime")
    @Mapping(target = "cinemaName", source = "booking.showtime.room.cinema.name")
    @Mapping(target = "roomName", expression = "java(formatRoomName(payment))")
    @Mapping(target = "seats", source = "booking.bookingSeats")
    @Mapping(target = "totalPrice", source = "booking.totalPrice")
    @Mapping(target = "discountReason", source = "booking.discountReason")
    @Mapping(target = "discountValue", source = "booking.discountValue")
    @Mapping(target = "finalPrice", source = "booking.finalPrice")
    @Mapping(target = "bookingStatus", source = "booking.status")
    @Mapping(target = "bookedAt", source = "booking.bookedAt")
    @Mapping(target = "qrCode", source = "booking.qrCode")
    @Mapping(target = "qrPayload", source = "booking.qrPayload")
    @Mapping(target = "paymentExpiresAt", source = "booking.paymentExpiresAt")
    @Mapping(target = "posterUrl", source = "booking.showtime.movie.posterUrl")
    PaymentResponse toPaymentResponse(Payment payment);

    default String formatRoomName(Payment payment) {
        if (payment == null || payment.getBooking() == null ||
                payment.getBooking().getShowtime() == null ||
                payment.getBooking().getShowtime().getRoom() == null) {
            return null;
        }
        var room = payment.getBooking().getShowtime().getRoom();
        return "Room " + room.getRoomNumber() + " (" + room.getRoomType() + ")";
    }
}
