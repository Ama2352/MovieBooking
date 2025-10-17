package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;

import com.api.moviebooking.helpers.utils.MappingUtils;

@Mapper(componentModel = "spring", uses = MappingUtils.class)
public interface UserMapper {

}
