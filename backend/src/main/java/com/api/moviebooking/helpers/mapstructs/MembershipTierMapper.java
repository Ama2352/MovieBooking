package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

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
    @Mapping(target = "discountType", source = "discountType", qualifiedByName = "mapDiscountType")
    @Mapping(target = "isActive", source = "isActive", qualifiedByName = "mapIsActive")
    MembershipTier toEntity(AddMembershipTierRequest request);

    @Mapping(target = "membershipTierId", source = "id")
    MembershipTierDataResponse toDataResponse(MembershipTier membershipTier);

    @Named("mapDiscountType")
    default DiscountType mapDiscountType(String discountType) {
        return discountType != null && !discountType.isEmpty() ? DiscountType.valueOf(discountType) : null;
    }

    @Named("mapIsActive")
    default Boolean mapIsActive(Boolean isActive) {
        return isActive != null ? isActive : true;
    }
}
