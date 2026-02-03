package com.chatai.service;

import com.chatai.dto.request.RegisterRequest;
import com.chatai.dto.response.UserResponse;
import com.chatai.entity.User;
import com.chatai.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for UserService
 * 
 * **Feature: ai-chat-interface, Property 1: Valid registration creates user**
 * **Feature: ai-chat-interface, Property 2: Invalid registration returns field-specific errors**
 */
public class UserServiceProperties {

    private static ConfigurableApplicationContext context;
    private static Validator validator;
    private UserService userService;
    private UserRepository userRepository;

    @BeforeProperty
    void setUp() {
        if (context == null) {
            System.setProperty("spring.profiles.active", "test");
            context = SpringApplication.run(com.chatai.ChatAiApplication.class);
        }
        if (validator == null) {
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }
        userService = context.getBean(UserService.class);
        userRepository = context.getBean(UserRepository.class);
        // Clean database before each property
        userRepository.deleteAll();
    }

    /**
     * **Feature: ai-chat-interface, Property 1: Valid registration creates user**
     * **Validates: Requirements 1.1**
     * 
     * For any valid registration data (valid email format, password meeting requirements, 
     * non-empty name), submitting registration SHALL result in a new user being created 
     * with matching email and name.
     */
    @Property(tries = 100)
    void validRegistrationCreatesUser(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String username,
            @ForAll @AlphaChars @StringLength(min = 6, max = 30) String password,
            @ForAll @AlphaChars @StringLength(min = 2, max = 30) String name
    ) {
        // Generate a valid email from username
        String email = username.toLowerCase() + "@example.com";
        
        // Clean any existing user with same email
        userRepository.findByEmail(email).ifPresent(u -> userRepository.delete(u));
        
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .password(password)
                .name(name)
                .build();

        // Act
        UserResponse response = userService.create(request);

        // Assert - user was created with matching data
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getEmail()).isEqualTo(email);
        assertThat(response.getName()).isEqualTo(name);

        // Verify user exists in database
        Optional<User> savedUser = userRepository.findByEmail(email);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getEmail()).isEqualTo(email);
        assertThat(savedUser.get().getName()).isEqualTo(name);
        
        // Password should be hashed (not stored as plain text)
        assertThat(savedUser.get().getPassword()).isNotEqualTo(password);
    }

    /**
     * **Feature: ai-chat-interface, Property 2: Invalid registration returns field-specific errors**
     * **Validates: Requirements 1.2**
     * 
     * For any registration data with invalid fields, the system SHALL return error messages 
     * that specifically identify each invalid field.
     */
    @Property(tries = 100)
    void invalidEmailReturnsEmailFieldError(
            @ForAll @AlphaChars @StringLength(min = 6, max = 30) String password,
            @ForAll @AlphaChars @StringLength(min = 2, max = 30) String name
    ) {
        // Invalid email (not a valid email format)
        String invalidEmail = "not-an-email";
        
        RegisterRequest request = RegisterRequest.builder()
                .email(invalidEmail)
                .password(password)
                .name(name)
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        
        // Should have validation error
        assertThat(violations).isNotEmpty();
        
        // Should specifically identify the email field
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(violatedFields).contains("email");
    }

    /**
     * **Feature: ai-chat-interface, Property 2: Invalid registration returns field-specific errors**
     * **Validates: Requirements 1.2**
     * 
     * For any registration data with short password, the system SHALL return error messages 
     * that specifically identify the password field.
     */
    @Property(tries = 100)
    void shortPasswordReturnsPasswordFieldError(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String username,
            @ForAll @AlphaChars @StringLength(min = 1, max = 5) String shortPassword,
            @ForAll @AlphaChars @StringLength(min = 2, max = 30) String name
    ) {
        String email = username.toLowerCase() + "@example.com";
        
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .password(shortPassword)
                .name(name)
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        
        // Should have validation error
        assertThat(violations).isNotEmpty();
        
        // Should specifically identify the password field
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(violatedFields).contains("password");
    }

    /**
     * **Feature: ai-chat-interface, Property 2: Invalid registration returns field-specific errors**
     * **Validates: Requirements 1.2**
     * 
     * For any registration data with blank name, the system SHALL return error messages 
     * that specifically identify the name field.
     */
    @Property(tries = 100)
    void blankNameReturnsNameFieldError(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String username,
            @ForAll @AlphaChars @StringLength(min = 6, max = 30) String password
    ) {
        String email = username.toLowerCase() + "@example.com";
        
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .password(password)
                .name("")  // Blank name
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        
        // Should have validation error
        assertThat(violations).isNotEmpty();
        
        // Should specifically identify the name field
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(violatedFields).contains("name");
    }

    /**
     * **Feature: ai-chat-interface, Property 2: Invalid registration returns field-specific errors**
     * **Validates: Requirements 1.2**
     * 
     * For any registration data with multiple invalid fields, the system SHALL return 
     * error messages that specifically identify each invalid field.
     */
    @Property(tries = 100)
    void multipleInvalidFieldsReturnMultipleFieldErrors(
            @ForAll @AlphaChars @StringLength(min = 1, max = 5) String shortPassword
    ) {
        RegisterRequest request = RegisterRequest.builder()
                .email("invalid-email")  // Invalid email
                .password(shortPassword)  // Short password
                .name("")  // Blank name
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        
        // Should have multiple validation errors
        assertThat(violations.size()).isGreaterThanOrEqualTo(3);
        
        // Should specifically identify all invalid fields
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(violatedFields).contains("email", "password", "name");
    }
}
