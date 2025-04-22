package com.example.api.service;

import com.example.api.dto.UserProfileResponse;
import com.example.api.dto.UserRegistrationRequest;
import com.example.api.exception.ResourceNotFoundException;
import com.example.api.model.User;
import com.example.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public void createUser(UserRegistrationRequest request, String keycloakId) {
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .keycloakId(keycloakId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
    }

    public UserProfileResponse getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return mapToUserProfileResponse(user);
    }

    public UserProfileResponse getUserProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        return mapToUserProfileResponse(user);
    }

    public String getUserIdByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with keycloakId: " + keycloakId));
    }

    public String getUsernameByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .map(User::getUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with keycloakId: " + keycloakId));
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
