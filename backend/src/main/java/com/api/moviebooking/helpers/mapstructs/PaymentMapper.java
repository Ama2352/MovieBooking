package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.helpers.utils.MappingUtils;
import com.api.moviebooking.models.dtos.payment.PaymentResponse;
import com.api.moviebooking.models.entities.Payment;

@Mapper(componentModel = "spring", uses = { MappingUtils.class })
public interface PaymentMapper {

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "bookingStatus", source = "booking.status")
    @Mapping(target = "qrPayload", source = "booking.qrPayload")
    PaymentResponse toPaymentResponse(Payment payment);
}
