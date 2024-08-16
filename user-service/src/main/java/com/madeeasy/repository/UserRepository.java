package com.madeeasy.repository;

import com.madeeasy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    @Query(value = "SELECT u FROM User u WHERE u.email=:emailId")
    Optional<User> findByEmail(@Param("emailId") String emailId);

    Optional<User> findById(String userId);

    void deleteByEmail(String emailId);
}
