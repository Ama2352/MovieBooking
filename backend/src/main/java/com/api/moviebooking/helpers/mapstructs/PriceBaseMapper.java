package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.models.dtos.priceBase.AddPriceBaseRequest;
import com.api.moviebooking.models.dtos.priceBase.PriceBaseDataResponse;
import com.api.moviebooking.models.entities.PriceBase;

@Mapper(componentModel = "spring")
public interface PriceBaseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PriceBase toEntity(AddPriceBaseRequest request);

    PriceBaseDataResponse toDataResponse(PriceBase priceBase);
}
