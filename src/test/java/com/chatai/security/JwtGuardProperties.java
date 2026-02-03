package com.chatai.security;

import com.chatai.dto.request.LoginRequest;
import com.chatai.dto.request.RegisterRequest;
import com.chatai.dto.response.AuthResponse;
import com.chatai.repository.UserRepository;
import com.chatai.service.AuthService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for JWT validation guarding protected endpoints
 * 
 * **Feature: ai-chat-interface, Property 13: JWT validation guards endpoints**
 * **Validates: Requirements 6.1, 6.2**
 */
public class JwtGuardProperties {

    private static ConfigurableApplicationContext context;
    private static String baseUrl;
    private RestTemplate restTemplate;
    private AuthService authService;
    private UserRepository userRepository;
    private JwtTokenProvider jwtTokenProvider;
    private TokenBlacklistService tokenBlacklistService;

    @BeforeProperty
    void setUp() {
        if (context == null) {
            System.setProperty("spring.profiles.active", "test");
            context = SpringApplication.run(com.chatai.ChatAiApplication.class);
            Environment env = context.getEnvironment();
            int port = Integer.parseInt(env.getProperty("local.server.port"));
            baseUrl = "http://localhost:" + port;
        }
        restTemplate = new RestTemplateBuilder().build();
        authService = context.getBean(AuthService.class);
        userRepository = context.getBean(UserRepository.class);
        jwtTokenProvider = context.getBean(JwtTokenProvider.class);
        tokenBlacklistService = context.getBean(TokenBlacklistService.class);
        // Clean database and blacklist before each property
        userRepository.deleteAll();
        tokenBlacklistService.clear();
    }

    /**
     * **Feature: ai-chat-interface, Property 13: JWT validation guards endpoints**
     * **Validates: Requirements 6.1, 6.2**
     * 
     * For any protected endpoint, requests without JWT token SHALL receive a 401 status response.
     */
    @Property(tries = 100)
    void requestsWithoutTokenReturn401(
            @ForAll("protectedEndpoints") String endpoint
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Act & Assert - request without token should return 401
        assertThatThrownBy(() -> 
            restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, entity, String.class))
            .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    /**
     * **Feature: ai-chat-interface, Property 13: JWT validation guards endpoints**
     * **Validates: Requirements 6.1, 6.2**
     * 
     * For any protected endpoint, requests with invalid JWT token SHALL receive a 401 status response.
     */
    @Property(tries = 100)
    void requestsWithInvalidTokenReturn401(
            @ForAll("protectedEndpoints") String endpoint,
            @ForAll @AlphaChars @StringLength(min = 10, max = 100) String invalidToken
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + invalidToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Act & Assert - request with invalid token should return 401
        assertThatThrownBy(() -> 
            restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, entity, String.class))
            .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    /**
     * **Feature: ai-chat-interface, Property 13: JWT validation guards endpoints**
     * **Validates: Requirements 6.1, 6.2**
     * 
     * For any protected endpoint, requests with malformed Authorization header SHALL receive a 401 status response.
     */
    @Property(tries = 100)
    void requestsWithMalformedAuthHeaderReturn401(
            @ForAll("protectedEndpoints") String endpoint,
            @ForAll("malformedAuthHeaders") String authHeader
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!authHeader.isEmpty()) {
            headers.set("Authorization", authHeader);
        }
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Act & Assert - request with malformed auth header should return 401
        assertThatThrownBy(() -> 
            restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, entity, String.class))
            .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    /**
     * **Feature: ai-chat-interface, Property 13: JWT validation guards endpoints**
     * **Validates: Requirements 6.1, 6.2**
     * 
     * For any protected endpoint, requests with blacklisted (logged out) token SHALL receive a 401 status response.
     */
    @Property(tries = 100)
    void requestsWithBlacklistedTokenReturn401(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String username,
            @ForAll @AlphaChars @StringLength(min = 6, max = 30) String password,
            @ForAll @AlphaChars @StringLength(min = 2, max = 30) String name,
            @ForAll("protectedEndpoints") String endpoint
    ) {
        // Setup - create user and get valid token
        String email = username.toLowerCase() + "@example.com";
        userRepository.findByEmail(email).ifPresent(u -> userRepository.delete(u));
        
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(email)
                .password(password)
                .name(name)
                .build();
        authService.register(registerRequest);

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
        AuthResponse response = authService.login(loginRequest);
        String token = response.getAccessToken();

        // Logout to blacklist the token
        authService.logout(token);

        // Act & Assert - request with blacklisted token should return 401
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        assertThatThrownBy(() -> 
            restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, entity, String.class))
            .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    /**
     * **Feature: ai-chat-interface, Property 13: JWT validation guards endpoints**
     * **Validates: Requirements 6.1, 6.2**
     * 
     * For any protected endpoint, requests with valid JWT token SHALL NOT receive a 401 status response.
     * (They may receive other errors like 404 for non-existent resources, but not 401)
     */
    @Property(tries = 100)
    void requestsWithValidTokenDoNotReturn401(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String username,
            @ForAll @AlphaChars @StringLength(min = 6, max = 30) String password,
            @ForAll @AlphaChars @StringLength(min = 2, max = 30) String name
    ) {
        // Setup - create user and get valid token
        String email = username.toLowerCase() + "@example.com";
        userRepository.findByEmail(email).ifPresent(u -> userRepository.delete(u));
        
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(email)
                .password(password)
                .name(name)
                .build();
        authService.register(registerRequest);

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
        AuthResponse response = authService.login(loginRequest);
        String token = response.getAccessToken();

        // Act & Assert - request with valid token should NOT return 401
        // GET /api/conversations should return 200 (empty list is fine)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> result = restTemplate.exchange(
                baseUrl + "/api/conversations", HttpMethod.GET, entity, String.class);
        
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Provide
    Arbitrary<String> protectedEndpoints() {
        return Arbitraries.of(
                "/api/conversations",
                "/api/conversations/some-id",
                "/api/conversations/test-conv-123/messages"
        );
    }

    @Provide
    Arbitrary<String> malformedAuthHeaders() {
        return Arbitraries.of(
                "",                           // Empty
                "Bearer",                     // Missing token
                "Bearer ",                    // Empty token after Bearer
                "Basic abc123",               // Wrong auth type
                "token123",                   // No Bearer prefix
                "bearer token123",            // Lowercase bearer
                "BEARER token123"             // Uppercase BEARER
        );
    }
}
