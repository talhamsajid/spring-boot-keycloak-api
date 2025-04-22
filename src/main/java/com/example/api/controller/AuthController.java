package com.example.api.controller;

import com.example.api.dto.AuthResponse;
import com.example.api.dto.LoginRequest;
import com.example.api.dto.UserProfileResponse;
import com.example.api.dto.UserRegistrationRequest;
import com.example.api.service.KeycloakService;
import com.example.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakService keycloakService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        AuthResponse response = keycloakService.registerUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = keycloakService.authenticateUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody String refreshToken) {
        AuthResponse response = keycloakService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('user')")
    public ResponseEntity<UserProfileResponse> getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        UserProfileResponse userProfile = userService.getUserProfileByUsername(username);
        return ResponseEntity.ok(userProfile);
    }

    @GetMapping("/profile/{userId}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<UserProfileResponse> getUserProfileByAdmin(@PathVariable String userId) {
        UserProfileResponse userProfile = userService.getUserProfile(userId);
        return ResponseEntity.ok(userProfile);
    }
}
