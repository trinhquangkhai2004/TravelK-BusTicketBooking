package com.khaiquang.dto.mapper;

import com.khaiquang.dto.request.BusRequestDto;
import com.khaiquang.dto.request.BusUpdateRequest;
import com.khaiquang.dto.response.BusResponseDto;
import com.khaiquang.entity.Bus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring")
public interface BusMapper {
    
    @Mapping(target = "stationName", source = "station.name")
    @Mapping(target = "stationId", source = "station.id")
    BusResponseDto toBusResponseDto(Bus bus);

    @Mapping(target = "id", ignore = true)
    Bus toBusEntity(BusRequestDto busRequestDto);

    @Mapping(target = "id", ignore = true)
    void BusUpdateRequestDto(BusUpdateRequest dto, @MappingTarget Bus bus);
}
