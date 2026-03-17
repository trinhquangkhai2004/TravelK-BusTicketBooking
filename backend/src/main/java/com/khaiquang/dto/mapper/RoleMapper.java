package com.khaiquang.dto.mapper;

import com.khaiquang.dto.response.RoleResponse;
import com.khaiquang.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleResponse toRoleResponse(Role role);

}
