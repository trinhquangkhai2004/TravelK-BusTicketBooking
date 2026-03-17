package com.khaiquang.utils;

import com.khaiquang.entity.Role;
import com.khaiquang.entity.User;
import com.khaiquang.repository.RoleRepository;
import com.khaiquang.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRoleName("ROLE_ADMIN");
                    return roleRepository.save(role);
                });

        if (!userRepository.existsByUserName("admin")) {
            User user = new User();
            user.setUserName("admin");
            user.setPassword(passwordEncoder.encode("admin"));
            user.setRoles(Set.of(adminRole));
            userRepository.save(user);

            System.out.println("✅ Admin created");
        } else {
            System.out.println("ℹ️ Admin already exists");
        }
    }
}
