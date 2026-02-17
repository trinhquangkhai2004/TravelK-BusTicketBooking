package com.khaiquang.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterRequest {
    @NotBlank(message = "User name can not be empty")
    private String userName;
    @NotBlank(message = "Password can not be empty")
    private String password;
    @NotBlank(message = "Email can not be empty")
    @Email(message = "Invalid Email")
    private String email;
    @NotBlank(message = "Phone number can not be empty")
    private String phoneNumber;

}
