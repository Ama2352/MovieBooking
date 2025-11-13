package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.models.dtos.membershipTier.AddMembershipTierRequest;
import com.api.moviebooking.models.dtos.membershipTier.MembershipTierDataResponse;
import com.api.moviebooking.models.entities.MembershipTier;
import com.api.moviebooking.models.enums.DiscountType;

@Mapper(componentModel = "spring")
public interface MembershipTierMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "discountType", expression = "java(mapDiscountType(request.getDiscountType()))")
    @Mapping(target = "isActive", expression = "java(request.getIsActive() != null ? request.getIsActive() : true)")
    MembershipTier toEntity(AddMembershipTierRequest request);

    MembershipTierDataResponse toDataResponse(MembershipTier membershipTier);

    default DiscountType mapDiscountType(String discountType) {
        return discountType != null && !discountType.isEmpty() ? DiscountType.valueOf(discountType) : null;
    }
}
