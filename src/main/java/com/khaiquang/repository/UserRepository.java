package com.khaiquang.repository;

import com.khaiquang.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    boolean existsByUserName(String username);
    Optional<User> findByUserName(String username);
    
    // Thêm phương thức tìm theo email
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}
