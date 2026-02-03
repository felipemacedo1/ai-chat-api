package com.chatai.service;

import com.chatai.dto.request.LoginRequest;
import com.chatai.dto.request.RegisterRequest;
import com.chatai.dto.response.AuthResponse;
import com.chatai.exception.InvalidCredentialsException;
import com.chatai.repository.UserRepository;
import com.chatai.security.JwtTokenProvider;
import com.chatai.security.TokenBlacklistService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for AuthService JWT authentication
 * 
 * **Feature: ai-chat-interface, Property 3: Valid credentials return JWT**
 * **Feature: ai-chat-interface, Property 4: Invalid credentials return generic error**
 * **Feature: ai-chat-interface, Property 5: Logout invalidates token**
 */
public class AuthServiceProperties {

    private static ConfigurableApplicationContext context;
    private AuthService authService;
    private UserRepository userRepository;
    private JwtTokenProvider jwtTokenProvider;
    private TokenBlacklistService tokenBlacklistService;

    @BeforeProperty
    void setUp() {
        if (context == null) {
            System.setProperty("spring.profiles.active", "test");
            context = SpringApplication.run(com.chatai.ChatAiApplication.class);
        }
        authService = context.getBean(AuthService.class);
        userRepository = context.getBean(UserRepository.class);
        jwtTokenProvider = context.getBean(JwtTokenProvider.class);
        tokenBlacklistService = context.getBean(TokenBlacklistService.class);
        // Clean database and blacklist before each property
        userRepository.deleteAll();
        tokenBlacklistService.clear();
    }

    /**
     * **Feature: ai-chat-interface, Property 3: Valid credentials return JWT**
     * **Validates: Requirements 1.3**
     * 
     * For any registered user with known credentials, submitting those credentials 
     * SHALL return a valid JWT token that can be used for authenticated requests.
     */
    @Property(tries = 100)
    void validCredentialsReturnValidJwt(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String username,
            @ForAll @AlphaChars @StringLength(min = 6, max = 30) String password,
            @ForAll @AlphaChars @StringLength(min = 2, max = 30) String name
    ) {
        // Generate a valid email from username
        String email = username.toLowerCase() + "@example.com";
        
        // Clean any existing user with same email
        userRepository.findByEmail(email).ifPresent(u -> userRepository.delete(u));
        
        // First register the user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(email)
                .password(password)
                .name(name)
                .build();
        authService.register(registerRequest);

        // Now login with valid credentials
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert - response contains valid JWT
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getAccessToken()).isNotEmpty();
        
        // Verify the token is valid and can be used
        assertThat(jwtTokenProvider.validateToken(response.getAccessToken())).isTrue();
        
        // Verify token contains correct user info
        assertThat(jwtTokenProvider.getEmailFromToken(response.getAccessToken())).isEqualTo(email);
        
        // Verify user info in response
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo(email);
        assertThat(response.getUser().getName()).isEqualTo(name);
    }

    /**
     * **Feature: ai-chat-interface, Property 4: Invalid credentials return generic error**
     * **Validates: Requirements 1.4**
     * 
     * For any login attempt with incorrect password, the system SHALL return an error 
     * message that does not reveal whether the email or password was incorrect.
     */
    @Property(tries = 100)
    void wrongPasswordReturnsGenericError(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String username,
            @ForAll @AlphaChars @StringLength(min = 6, max = 30) String correctPassword,
            @ForAll @AlphaChars @StringLength(min = 6, max = 30) String wrongPassword,
            @ForAll @AlphaChars @StringLength(min = 2, max = 30) String name
    ) {
        // Skip if passwords happen to be the same
        Assume.that(!correctPassword.equals(wrongPassword));
        
        String email = username.toLowerCase() + "@example.com";
        
        // Clean any existing user with same email
        userRepository.findByEmail(email).ifPresent(u -> userRepository.delete(u));
        
        // Register user with correct password
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(email)
                .password(correctPassword)
                .name(name)
                .build();
        authService.register(registerRequest);

        // Try to login with wrong password
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(wrongPassword)
                .build();

        // Act & Assert - should throw generic error
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    /**
     * **Feature: ai-chat-interface, Property 4: Invalid credentials return generic error**
     * **Validates: Requirements 1.4**
     * 
     * For any login attempt with non-existent email, the system SHALL return an error 
     * message that does not reveal whether the email or password was incorrect.
     */
    @Property(tries = 100)
    void nonExistentEmailReturnsGenericError(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String username,
            @ForAll @AlphaChars @StringLength(min = 6, max = 30) String password
    ) {
        String email = username.toLowerCase() + "@nonexistent.com";
        
        // Ensure user doesn't exist
        userRepository.findByEmail(email).ifPresent(u -> userRepository.delete(u));

        // Try to login with non-existent email
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        // Act & Assert - should throw same generic error as wrong password
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    /**
     * **Feature: ai-chat-interface, Property 4: Invalid credentials return generic error**
     * **Validates: Requirements 1.4**
     * 
     * For any login attempt with both wrong email and wrong password, the system SHALL 
     * return the same generic error message, not revealing which field is incorrect.
     */
    @Property(tries = 100)
    void bothWrongCredentialsReturnSameGenericError(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String registeredUsername,
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String wrongUsername,
            @ForAll @AlphaChars @StringLength(min = 6, max = 30) String correctPassword,
            @ForAll @AlphaChars @StringLength(min = 6, max = 30) String wrongPassword,
            @ForAll @AlphaChars @StringLength(min = 2, max = 30) String name
    ) {
        // Ensure usernames are different
        Assume.that(!registeredUsername.equalsIgnoreCase(wrongUsername));
        Assume.that(!correctPassword.equals(wrongPassword));
        
        String registeredEmail = registeredUsername.toLowerCase() + "@example.com";
        String wrongEmail = wrongUsername.toLowerCase() + "@example.com";
        
        // Clean any existing users
        userRepository.findByEmail(registeredEmail).ifPresent(u -> userRepository.delete(u));
        userRepository.findByEmail(wrongEmail).ifPresent(u -> userRepository.delete(u));
        
        // Register user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(registeredEmail)
                .password(correctPassword)
                .name(name)
                .build();
        authService.register(registerRequest);

        // Try to login with wrong email and wrong password
        LoginRequest loginRequest = LoginRequest.builder()
                .email(wrongEmail)
                .password(wrongPassword)
                .build();

        // Act & Assert - should throw same generic error
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    /**
     * **Feature: ai-chat-interface, Property 5: Logout invalidates token**
     * **Validates: Requirements 1.5**
     * 
     * For any authenticated session, after logout the previously valid token 
     * SHALL be rejected on subsequent requests.
     */
    @Property(tries = 100)
    void logoutInvalidatesToken(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String username,
            @ForAll @AlphaChars @StringLength(min = 6, max = 30) String password,
            @ForAll @AlphaChars @StringLength(min = 2, max = 30) String name
    ) {
        // Generate a valid email from username
        String email = username.toLowerCase() + "@example.com";
        
        // Clean any existing user with same email
        userRepository.findByEmail(email).ifPresent(u -> userRepository.delete(u));
        
        // Register the user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(email)
                .password(password)
                .name(name)
                .build();
        authService.register(registerRequest);

        // Login to get a valid token
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
        AuthResponse response = authService.login(loginRequest);
        String token = response.getAccessToken();

        // Verify token is valid before logout
        assertThat(authService.isTokenValid(token)).isTrue();

        // Perform logout
        authService.logout(token);

        // Verify token is now invalid after logout
        assertThat(authService.isTokenValid(token)).isFalse();
        
        // Also verify the token is blacklisted
        assertThat(tokenBlacklistService.isBlacklisted(token)).isTrue();
    }

    /**
     * **Feature: ai-chat-interface, Property 5: Logout invalidates token**
     * **Validates: Requirements 1.5**
     * 
     * For any authenticated session, logout with Bearer prefix should also 
     * invalidate the token correctly.
     */
    @Property(tries = 100)
    void logoutWithBearerPrefixInvalidatesToken(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String username,
            @ForAll @AlphaChars @StringLength(min = 6, max = 30) String password,
            @ForAll @AlphaChars @StringLength(min = 2, max = 30) String name
    ) {
        // Generate a valid email from username
        String email = username.toLowerCase() + "@example.com";
        
        // Clean any existing user with same email
        userRepository.findByEmail(email).ifPresent(u -> userRepository.delete(u));
        
        // Register the user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(email)
                .password(password)
                .name(name)
                .build();
        authService.register(registerRequest);

        // Login to get a valid token
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
        AuthResponse response = authService.login(loginRequest);
        String token = response.getAccessToken();

        // Verify token is valid before logout
        assertThat(authService.isTokenValid(token)).isTrue();

        // Perform logout with Bearer prefix (as it would come from HTTP header)
        authService.logout("Bearer " + token);

        // Verify token is now invalid after logout
        assertThat(authService.isTokenValid(token)).isFalse();
        
        // Also verify the token is blacklisted (without Bearer prefix)
        assertThat(tokenBlacklistService.isBlacklisted(token)).isTrue();
    }
}
