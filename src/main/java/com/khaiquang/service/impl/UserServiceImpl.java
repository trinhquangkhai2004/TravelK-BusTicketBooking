package com.khaiquang.service.impl;

import com.khaiquang.dto.mapper.UserMapper;
import com.khaiquang.dto.request.UserRegisterRequest;
import com.khaiquang.dto.request.UserUpdateRequest;
import com.khaiquang.dto.response.APIResponseDto;
import com.khaiquang.dto.response.UserResponse;
import com.khaiquang.entity.Role;
import com.khaiquang.entity.User;
import com.khaiquang.exception.ResourceDuplicateException;
import com.khaiquang.exception.ResourceNotFoundException;
import com.khaiquang.repository.RoleRepository;
import com.khaiquang.repository.UserRepository;
import com.khaiquang.service.StatisticService;
import com.khaiquang.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper  userMapper;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StatisticService statisticService;


    @Override
    public UserResponse createUser(UserRegisterRequest request) {
        if(userRepository.existsByUserName(request.getUserName())){
            throw new ResourceDuplicateException("user", "username",  request.getUserName());
        }
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByRoleName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "USER"));
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        
        // Update Redis Statistic
        statisticService.incrementUserCount();
        
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("user", "id", userId));
        Set<Role> roles = user.getRoles().stream().map(role -> roleRepository.findByRoleName(role.getRoleName())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Role", "name", role.getRoleName())
                )).collect(Collectors.toSet());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRoles(roles);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    public APIResponseDto getUserById(Long userId) {
        return null;
    }

    @Override
    public String getEmailUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("User", "id", userId));
        return user.getEmail();
    }

    @Override
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(userMapper::toUserResponse).toList();
    }

    @Override
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("User", "id", userId));
        userRepository.delete(user);
    }
}
