package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.models.dtos.promotion.AddPromotionRequest;
import com.api.moviebooking.models.dtos.promotion.PromotionDataResponse;
import com.api.moviebooking.models.entities.Promotion;
import com.api.moviebooking.models.enums.DiscountType;

@Mapper(componentModel = "spring")
public interface PromotionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "bookings", ignore = true)
    @Mapping(target = "discountType", expression = "java(mapDiscountType(request.getDiscountType()))")
    @Mapping(target = "isActive", expression = "java(request.getIsActive() != null ? request.getIsActive() : true)")
    Promotion toEntity(AddPromotionRequest request);

    @Mapping(target = "promotionId", source = "id")
    PromotionDataResponse toDataResponse(Promotion promotion);

    default DiscountType mapDiscountType(String discountType) {
        return discountType != null ? DiscountType.valueOf(discountType) : null;
    }
}
