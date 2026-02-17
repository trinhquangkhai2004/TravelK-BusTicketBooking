package com.khaiquang.dto.mapper;

import com.khaiquang.dto.request.TripDto;
import com.khaiquang.dto.response.TripResponseDto;
import com.khaiquang.entity.Trip;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {BusMapper.class, StationMapper.class})
public interface TripMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bus", ignore = true)
    @Mapping(target = "departureStation", ignore = true)
    @Mapping(target = "arrivalStation", ignore = true)
    @Mapping(target = "destination", source = "arrivalStationName")
    @Mapping(target = "origin", source = "departureStationName")
    Trip toEntity(TripDto tripDto);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "busName", source = "bus.number")
    TripResponseDto toResponseDto(Trip trip);
}
