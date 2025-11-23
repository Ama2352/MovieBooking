package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.models.dtos.ticketType.TicketTypeResponse;
import com.api.moviebooking.models.dtos.ticketType.TicketTypePublicResponse;
import com.api.moviebooking.models.entities.TicketType;

@Mapper(componentModel = "spring")
public interface TicketTypeMapper {

    /**
     * Map to full response with all admin fields
     * Used for admin endpoints: GET /ticket-types/admin
     */
    @Mapping(target = "ticketTypeId", source = "id")
    @Mapping(target = "price", ignore = true)
    TicketTypeResponse toAdminResponse(TicketType ticketType);

    /**
     * Map to public response for guest/user view (only essential fields)
     * Used for public endpoints: GET /ticket-types
     * Price will be calculated and set separately in service layer
     */
    @Mapping(target = "ticketTypeId", source = "id")
    @Mapping(target = "price", ignore = true)
    TicketTypePublicResponse toPublicResponse(TicketType ticketType);

}
