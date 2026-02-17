package com.khaiquang.dto.mapper;

import com.khaiquang.dto.request.UserRegisterRequest;
import com.khaiquang.dto.request.UserUpdateRequest;
import com.khaiquang.dto.response.UserResponse;
import com.khaiquang.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {RoleMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)

public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "bookingTrips", ignore = true)
    User toUser(UserRegisterRequest request);

    @Mapping(target = "userId", source = "id")
    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bookingTrips",  ignore = true)
    @Mapping(target = "userName", ignore = true)
    void updateUserDto(UserUpdateRequest userUpdateRequest, @MappingTarget User user);
}
