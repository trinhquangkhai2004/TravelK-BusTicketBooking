package com.khaiquang.security;

import com.khaiquang.entity.User;
import com.khaiquang.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        // Thử tìm bằng email trước
        Optional<User> userOpt = userRepository.findByEmail(input);
        
        // Nếu không thấy, thử tìm bằng username (để hỗ trợ refresh token hoặc legacy)
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUserName(input);
        }

        User user = userOpt.orElseThrow(() ->
                new UsernameNotFoundException("User not found by email or username: " + input));
                
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        user.getRoles().forEach(role -> grantedAuthorities.add(new SimpleGrantedAuthority(role.getRoleName())));
        
        return new CustomUserDetail(
                user.getId(),
                user.getUserName(),
                user.getPassword(),
                grantedAuthorities
        );
    }
}
