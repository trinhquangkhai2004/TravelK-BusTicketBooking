package com.khaiquang.controller;

import com.khaiquang.config.JwtProvider;
import com.khaiquang.dto.request.LoginRequest;
import com.khaiquang.entity.User;
import com.khaiquang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class  AuthenticateController {
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        String loginInput = request.getEmail();
        
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginInput, request.getPassword())
        );

        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(loginInput);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        // Tìm user ID (để lưu frontend)
        User user = userRepository.findByEmail(loginInput)
                .or(() -> userRepository.findByUserName(loginInput))
                .orElseThrow();

        return Map.of(
                "accessToken", jwtProvider.generateAccessToken(userDetails),
                "refreshToken", jwtProvider.generateRefreshToken(userDetails),
                "userId", user.getId(),
                "username", user.getUserName(),
                "roles", roles
        );
    }

    @PostMapping("/refresh")
    public Map<String, String> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        if (!jwtProvider.isValid(refreshToken))
            throw new RuntimeException("Invalid refresh token");

        String username = jwtProvider.extractUsername(refreshToken);
        UserDetails user = userDetailsService.loadUserByUsername(username);

        return Map.of("accessToken", jwtProvider.generateAccessToken(user));
    }
}
