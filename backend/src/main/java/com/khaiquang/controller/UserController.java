package com.khaiquang.controller;

import com.khaiquang.dto.request.UserRegisterRequest;
import com.khaiquang.dto.request.UserUpdateRequest;
import com.khaiquang.dto.response.APIResponseDto;
import com.khaiquang.dto.response.UserResponse;
import com.khaiquang.service.UserService;
import com.khaiquang.utils.CustomHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRegisterRequest request){
        return new  ResponseEntity<>(userService.createUser(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @RequestBody UserUpdateRequest request){
        return  new  ResponseEntity<>(userService.updateUser(id, request), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<APIResponseDto> getUserById(@RequestHeader(CustomHeaders.X_AUTH_USER_ID) Long userId){
        return  new ResponseEntity<>(userService.getUserById(userId), HttpStatus.OK);
    }

    @GetMapping("/email/{id}")
    public ResponseEntity<String> getEmailUser(@PathVariable Long id){
        return  new ResponseEntity<>(userService.getEmailUser(id), HttpStatus.OK);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers(){
        return new ResponseEntity<>(userService.getAllUsers(),HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id){
        userService.deleteUser(id);
        return new ResponseEntity<>("Delete Successfully",HttpStatus.OK);
    }
}
