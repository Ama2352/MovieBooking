package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.models.dtos.priceModifier.AddPriceModifierRequest;
import com.api.moviebooking.models.dtos.priceModifier.PriceModifierDataResponse;
import com.api.moviebooking.models.entities.PriceModifier;

@Mapper(componentModel = "spring")
public interface PriceModifierMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conditionType", ignore = true)
    @Mapping(target = "modifierType", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PriceModifier toEntity(AddPriceModifierRequest request);

    @Mapping(target = "priceModifierId", source = "id")
    PriceModifierDataResponse toDataResponse(PriceModifier priceModifier);
}
