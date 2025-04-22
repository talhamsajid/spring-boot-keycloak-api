package com.example.api.repository;

import com.example.api.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByKeycloakId(String keycloakId);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
}
