package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.helpers.utils.MappingUtils;
import com.api.moviebooking.models.dtos.auth.LoginResponse;
import com.api.moviebooking.models.dtos.user.UserProfileResponse;
import com.api.moviebooking.models.entities.User;

@Mapper(componentModel = "spring", uses = { MappingUtils.class, MembershipTierMapper.class })
public interface UserMapper {

    @Mapping(target = "userId", source = "id")
    UserProfileResponse toUserProfileResponse(User user);

    @Mapping(target = "userId", source = "id")
    LoginResponse toLoginResponse(User user);
}
