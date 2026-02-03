package com.chatai.validation;

import com.chatai.dto.request.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for request validation
 * 
 * **Feature: ai-chat-interface, Property 14: Request validation rejects invalid schemas**
 * **Validates: Requirements 6.3**
 */
public class RequestValidationProperties {

    private final Validator validator;

    public RequestValidationProperties() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    // ==================== RegisterRequest Validation ====================

    /**
     * **Feature: ai-chat-interface, Property 14: Request validation rejects invalid schemas**
     * **Validates: Requirements 6.3**
     * 
     * For any RegisterRequest with invalid email format, validation SHALL reject it.
     */
    @Property(tries = 100)
    void registerRequestWithInvalidEmailIsRejected(
            @ForAll("invalidEmails") String invalidEmail,
            @ForAll @StringLength(min = 6, max = 30) String password,
            @ForAll @StringLength(min = 1, max = 50) String name
    ) {
        RegisterRequest request = RegisterRequest.builder()
                .email(invalidEmail)
                .password(password)
                .name(name)
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(violatedFields).contains("email");
    }

    /**
     * **Feature: ai-chat-interface, Property 14: Request validation rejects invalid schemas**
     * **Validates: Requirements 6.3**
     * 
     * For any RegisterRequest with password shorter than 6 characters, validation SHALL reject it.
     */
    @Property(tries = 100)
    void registerRequestWithShortPasswordIsRejected(
            @ForAll("validEmails") String email,
            @ForAll @StringLength(min = 1, max = 5) String shortPassword,
            @ForAll @StringLength(min = 1, max = 50) String name
    ) {
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .password(shortPassword)
                .name(name)
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(violatedFields).contains("password");
    }

    /**
     * **Feature: ai-chat-interface, Property 14: Request validation rejects invalid schemas**
     * **Validates: Requirements 6.3**
     * 
     * For any RegisterRequest with blank name, validation SHALL reject it.
     */
    @Property(tries = 100)
    void registerRequestWithBlankNameIsRejected(
            @ForAll("validEmails") String email,
            @ForAll @StringLength(min = 6, max = 30) String password,
            @ForAll("blankStrings") String blankName
    ) {
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .password(password)
                .name(blankName)
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(violatedFields).contains("name");
    }

    /**
     * **Feature: ai-chat-interface, Property 14: Request validation rejects invalid schemas**
     * **Validates: Requirements 6.3**
     * 
     * For any valid RegisterRequest, validation SHALL pass.
     */
    @Property(tries = 100)
    void validRegisterRequestPasses(
            @ForAll("validEmails") String email,
            @ForAll("validPasswords") String password,
            @ForAll("validNames") String name
    ) {
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .password(password)
                .name(name)
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    // ==================== LoginRequest Validation ====================

    /**
     * **Feature: ai-chat-interface, Property 14: Request validation rejects invalid schemas**
     * **Validates: Requirements 6.3**
     * 
     * For any LoginRequest with invalid email format, validation SHALL reject it.
     */
    @Property(tries = 100)
    void loginRequestWithInvalidEmailIsRejected(
            @ForAll("invalidEmails") String invalidEmail,
            @ForAll @StringLength(min = 1, max = 30) String password
    ) {
        LoginRequest request = LoginRequest.builder()
                .email(invalidEmail)
                .password(password)
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(violatedFields).contains("email");
    }

    /**
     * **Feature: ai-chat-interface, Property 14: Request validation rejects invalid schemas**
     * **Validates: Requirements 6.3**
     * 
     * For any LoginRequest with blank password, validation SHALL reject it.
     */
    @Property(tries = 100)
    void loginRequestWithBlankPasswordIsRejected(
            @ForAll("validEmails") String email,
            @ForAll("blankStrings") String blankPassword
    ) {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(blankPassword)
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(violatedFields).contains("password");
    }

    // ==================== CreateMessageRequest Validation ====================

    /**
     * **Feature: ai-chat-interface, Property 14: Request validation rejects invalid schemas**
     * **Validates: Requirements 6.3**
     * 
     * For any CreateMessageRequest with blank content, validation SHALL reject it.
     */
    @Property(tries = 100)
    void createMessageRequestWithBlankContentIsRejected(
            @ForAll("blankStrings") String blankContent
    ) {
        CreateMessageRequest request = CreateMessageRequest.builder()
                .content(blankContent)
                .build();

        Set<ConstraintViolation<CreateMessageRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(violatedFields).contains("content");
    }

    /**
     * **Feature: ai-chat-interface, Property 14: Request validation rejects invalid schemas**
     * **Validates: Requirements 6.3**
     * 
     * For any CreateMessageRequest with valid content, validation SHALL pass.
     */
    @Property(tries = 100)
    void validCreateMessageRequestPasses(
            @ForAll("validContent") String content
    ) {
        CreateMessageRequest request = CreateMessageRequest.builder()
                .content(content)
                .build();

        Set<ConstraintViolation<CreateMessageRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    // ==================== UpdateConversationRequest Validation ====================

    /**
     * **Feature: ai-chat-interface, Property 14: Request validation rejects invalid schemas**
     * **Validates: Requirements 6.3**
     * 
     * For any UpdateConversationRequest with blank title, validation SHALL reject it.
     */
    @Property(tries = 100)
    void updateConversationRequestWithBlankTitleIsRejected(
            @ForAll("blankStrings") String blankTitle
    ) {
        UpdateConversationRequest request = UpdateConversationRequest.builder()
                .title(blankTitle)
                .build();

        Set<ConstraintViolation<UpdateConversationRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(violatedFields).contains("title");
    }

    /**
     * **Feature: ai-chat-interface, Property 14: Request validation rejects invalid schemas**
     * **Validates: Requirements 6.3**
     * 
     * For any UpdateConversationRequest with title exceeding 255 characters, validation SHALL reject it.
     */
    @Property(tries = 100)
    void updateConversationRequestWithTooLongTitleIsRejected(
            @ForAll @StringLength(min = 256, max = 500) String longTitle
    ) {
        UpdateConversationRequest request = UpdateConversationRequest.builder()
                .title(longTitle)
                .build();

        Set<ConstraintViolation<UpdateConversationRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(violatedFields).contains("title");
    }

    /**
     * **Feature: ai-chat-interface, Property 14: Request validation rejects invalid schemas**
     * **Validates: Requirements 6.3**
     * 
     * For any UpdateConversationRequest with valid title, validation SHALL pass.
     */
    @Property(tries = 100)
    void validUpdateConversationRequestPasses(
            @ForAll("validTitles") String title
    ) {
        UpdateConversationRequest request = UpdateConversationRequest.builder()
                .title(title)
                .build();

        Set<ConstraintViolation<UpdateConversationRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    // ==================== CreateConversationRequest Validation ====================

    /**
     * **Feature: ai-chat-interface, Property 14: Request validation rejects invalid schemas**
     * **Validates: Requirements 6.3**
     * 
     * For any CreateConversationRequest with title exceeding 255 characters, validation SHALL reject it.
     */
    @Property(tries = 100)
    void createConversationRequestWithTooLongTitleIsRejected(
            @ForAll @StringLength(min = 256, max = 500) String longTitle
    ) {
        CreateConversationRequest request = CreateConversationRequest.builder()
                .title(longTitle)
                .build();

        Set<ConstraintViolation<CreateConversationRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertThat(violatedFields).contains("title");
    }

    /**
     * **Feature: ai-chat-interface, Property 14: Request validation rejects invalid schemas**
     * **Validates: Requirements 6.3**
     * 
     * For any CreateConversationRequest with valid or null title, validation SHALL pass.
     */
    @Property(tries = 100)
    void validCreateConversationRequestPasses(
            @ForAll("optionalValidTitle") String title
    ) {
        CreateConversationRequest request = CreateConversationRequest.builder()
                .title(title)
                .build();

        Set<ConstraintViolation<CreateConversationRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    // ==================== Arbitrary Providers ====================

    @Provide
    Arbitrary<String> invalidEmails() {
        return Arbitraries.oneOf(
                // Missing @
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                        .map(s -> s + "example.com"),
                // Missing domain
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                        .map(s -> s + "@"),
                // Empty string
                Arbitraries.just(""),
                // Only whitespace
                Arbitraries.just("   "),
                // Missing local part
                Arbitraries.just("@example.com"),
                // Invalid characters
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + " space@example.com"),
                // Null
                Arbitraries.just(null)
        );
    }

    @Provide
    Arbitrary<String> validEmails() {
        return Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20)
                .map(s -> s.toLowerCase() + "@example.com");
    }

    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.just("   "),
                Arbitraries.just("\t"),
                Arbitraries.just("\n"),
                Arbitraries.just("  \t\n  "),
                Arbitraries.just(null)
        );
    }

    @Provide
    Arbitrary<String> optionalValidTitle() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(255)
        );
    }

    @Provide
    Arbitrary<String> validPasswords() {
        return Arbitraries.strings().alpha().ofMinLength(6).ofMaxLength(30);
    }

    @Provide
    Arbitrary<String> validNames() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
    }

    @Provide
    Arbitrary<String> validContent() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(1000);
    }

    @Provide
    Arbitrary<String> validTitles() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(255);
    }
}
