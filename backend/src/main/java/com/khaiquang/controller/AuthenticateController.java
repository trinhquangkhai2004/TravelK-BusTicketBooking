package com.khaiquang.controller;

import com.khaiquang.config.JwtProvider;
import com.khaiquang.dto.request.LoginRequest;
import com.khaiquang.entity.User;
import com.khaiquang.exception.ResourceNotFoundException;
import com.khaiquang.repository.UserRepository;
import com.khaiquang.service.EmailSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticateController {
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final EmailSenderService emailSenderService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        String loginInput = request.getEmail(); 
        
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginInput, request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(loginInput);
        
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
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

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email", "Mail", email));

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("reset_pw:" + token, email, 15, TimeUnit.MINUTES);
        String resetLink = "http://localhost:3000/reset-password?token=" + token;

        emailSenderService.sendResetPasswordEmail(email, user.getUserName(), resetLink);

        return ResponseEntity.ok("Vui lòng kiểm tra email để đặt lại mật khẩu.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");

        // Kiểm tra token trong Redis
        String email = (String) redisTemplate.opsForValue().get("reset_pw:" + token);
        
        if (email == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Link đặt lại mật khẩu đã hết hạn hoặc không hợp lệ.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email", "mail", email));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Xóa token sau khi dùng xong
        redisTemplate.delete("reset_pw:" + token);

        return ResponseEntity.ok("Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.");
    }
}
