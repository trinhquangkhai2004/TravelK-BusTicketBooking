package com.khaiquang.dto.mapper;

import com.khaiquang.dto.request.StationRequestDto;
import com.khaiquang.dto.request.StationUpdateRequest;
import com.khaiquang.dto.response.StationResponseDto;
import com.khaiquang.entity.Station;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {BusMapper.class})
public interface StationMapper {
    @Mapping(target = "id", ignore = true)
    //@Mapping(target = "buses", source = "busList")
    Station toEntity(StationRequestDto requestDto);

    @Mapping(target = "stationId", source = "id")
    @Mapping(target = "stationName", source = "name")
    @Mapping(target = "buses", source = "buses")
    StationResponseDto toResponse(Station station);

    void updateStation(StationUpdateRequest stationUpdateRequest, @MappingTarget Station station);
}
