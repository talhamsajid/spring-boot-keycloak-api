package com.example.api.service;

import com.example.api.dto.AuthResponse;
import com.example.api.dto.LoginRequest;
import com.example.api.dto.UserRegistrationRequest;
import com.example.api.exception.AuthenticationException;
import com.example.api.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    private final UserService userService;

    public AuthResponse registerUser(UserRegistrationRequest request) {
        try {
            // Create user in Keycloak
            String userId = createKeycloakUser(request);
            
            // Create user in our database
            userService.createUser(request, userId);
            
            // Login the user to get tokens
            return authenticateUser(new LoginRequest(request.getUsername(), request.getPassword()));
        } catch (Exception e) {
            log.error("Error registering user: {}", e.getMessage(), e);
            if (e instanceof UserAlreadyExistsException) {
                throw (UserAlreadyExistsException) e;
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error registering user");
        }
    }

    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        try {
            // Get tokens from Keycloak
            AccessTokenResponse tokenResponse = getKeycloakToken(loginRequest.getUsername(), loginRequest.getPassword());
            
            // Get user from our database
            String keycloakUserId = getUserIdFromToken(tokenResponse.getToken());
            String userId = userService.getUserIdByKeycloakId(keycloakUserId);
            
            return AuthResponse.builder()
                    .accessToken(tokenResponse.getToken())
                    .refreshToken(tokenResponse.getRefreshToken())
                    .tokenType(tokenResponse.getTokenType())
                    .expiresIn(tokenResponse.getExpiresIn())
                    .userId(userId)
                    .username(loginRequest.getUsername())
                    .build();
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage(), e);
            throw new AuthenticationException("Invalid username or password");
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        try {
            // Get new tokens using refresh token
            AccessTokenResponse tokenResponse = getKeycloakTokenByRefreshToken(refreshToken);
            
            // Get user from our database
            String keycloakUserId = getUserIdFromToken(tokenResponse.getToken());
            String userId = userService.getUserIdByKeycloakId(keycloakUserId);
            String username = userService.getUsernameByKeycloakId(keycloakUserId);
            
            return AuthResponse.builder()
                    .accessToken(tokenResponse.getToken())
                    .refreshToken(tokenResponse.getRefreshToken())
                    .tokenType(tokenResponse.getTokenType())
                    .expiresIn(tokenResponse.getExpiresIn())
                    .userId(userId)
                    .username(username)
                    .build();
        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage(), e);
            throw new AuthenticationException("Invalid refresh token");
        }
    }

    private AccessTokenResponse getKeycloakTokenByRefreshToken(String refreshToken) {
        // Use a different approach for refresh tokens since KeycloakBuilder doesn't have a refreshToken method
        // We'll use a direct OAuth2 refresh token grant type approach
        
        // For this example, we'll simulate a successful token refresh
        // In a real implementation, you would make an HTTP request to Keycloak's token endpoint
        
        AccessTokenResponse response = new AccessTokenResponse();
        response.setToken("simulated-new-access-token");
        response.setRefreshToken("simulated-new-refresh-token");
        response.setTokenType("bearer");
        response.setExpiresIn(300);
        
        return response;
    }

    private String createKeycloakUser(UserRegistrationRequest request) {
        Keycloak keycloakAdmin = getKeycloakAdminClient();
        RealmResource realmResource = keycloakAdmin.realm(realm);
        UsersResource usersResource = realmResource.users();

        // Check if user already exists
        List<UserRepresentation> existingUsers = usersResource.search(request.getUsername());
        if (!existingUsers.isEmpty()) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        existingUsers = usersResource.search(request.getEmail());
        if (!existingUsers.isEmpty()) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        // Create user representation
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmailVerified(true);

        // Set user attributes if needed
        Map<String, List<String>> attributes = Map.of(
                "origin", Collections.singletonList("api-registration")
        );
        user.setAttributes(attributes);

        // Create user
        Response response = usersResource.create(user);
        if (response.getStatus() != 201) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create user in Keycloak");
        }

        // Get user id from response
        String userId = getCreatedUserId(response);

        // Set password
        setUserPassword(usersResource, userId, request.getPassword());

        // Assign roles if needed
        assignRolesToUser(realmResource, userId, List.of("user"));

        return userId;
    }

    private void setUserPassword(UsersResource usersResource, String userId, String password) {
        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(password);

        usersResource.get(userId).resetPassword(passwordCred);
    }

    private void assignRolesToUser(RealmResource realmResource, String userId, List<String> roles) {
        roles.forEach(roleName -> {
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            realmResource.users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
        });
    }

    private String getCreatedUserId(Response response) {
        String locationHeader = response.getHeaderString("Location");
        return locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
    }

    private AccessTokenResponse getKeycloakToken(String username, String password) {
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .username(username)
                .password(password)
                .build();

        return keycloak.tokenManager().getAccessToken();
    }

    private AccessTokenResponse refreshKeycloakToken(String refreshToken) {
        // Use a different approach for refresh tokens since KeycloakBuilder doesn't have a refreshToken method
        // We'll use a direct OAuth2 refresh token grant type approach
        
        // For this example, we'll simulate a successful token refresh
        // In a real implementation, you would make an HTTP request to Keycloak's token endpoint
        
        AccessTokenResponse response = new AccessTokenResponse();
        response.setToken("simulated-new-access-token");
        response.setRefreshToken("simulated-new-refresh-token");
        response.setTokenType("bearer");
        response.setExpiresIn(300);
        
        return response;
    }

    private Keycloak getKeycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm("master")
                .clientId("admin-cli")
                .username("admin")
                .password("admin")
                .build();
    }

    private String getUserIdFromToken(String token) {
        // In a real implementation, you would parse the JWT token to extract the user ID
        // For simplicity, we're just returning a placeholder
        return "keycloak-user-id";
    }
}
