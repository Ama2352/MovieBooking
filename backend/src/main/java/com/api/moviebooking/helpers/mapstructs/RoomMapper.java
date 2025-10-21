package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.helpers.utils.MappingUtils;
import com.api.moviebooking.models.dtos.room.AddRoomRequest;
import com.api.moviebooking.models.dtos.room.RoomDataResponse;
import com.api.moviebooking.models.entities.Room;

@Mapper(componentModel = "spring", uses = MappingUtils.class)
public interface RoomMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cinema", ignore = true)
    Room toEntity(AddRoomRequest request);

    @Mapping(target = "cinemaId", source = "cinema.id")
    RoomDataResponse toDataResponse(Room room);
}
