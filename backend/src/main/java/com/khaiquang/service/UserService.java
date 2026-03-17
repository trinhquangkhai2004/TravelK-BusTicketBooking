package com.khaiquang.service;

import com.khaiquang.dto.request.UserRegisterRequest;
import com.khaiquang.dto.request.UserUpdateRequest;
import com.khaiquang.dto.response.APIResponseDto;
import com.khaiquang.dto.response.UserResponse;

import java.util.List;

public interface UserService {
     UserResponse createUser(UserRegisterRequest request);
     UserResponse updateUser(Long userId, UserUpdateRequest request);
     APIResponseDto getUserById(Long userId);
     String getEmailUser(Long userId);
     List<UserResponse> getAllUsers();
     void deleteUser(Long userId);

}
