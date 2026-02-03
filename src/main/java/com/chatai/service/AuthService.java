package com.chatai.service;

import com.chatai.dto.request.LoginRequest;
import com.chatai.dto.request.RegisterRequest;
import com.chatai.dto.response.AuthResponse;
import com.chatai.dto.response.UserResponse;
import com.chatai.entity.User;
import com.chatai.exception.InvalidCredentialsException;
import com.chatai.security.JwtTokenProvider;
import com.chatai.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        UserResponse userResponse = userService.create(request);
        
        String token = jwtTokenProvider.generateToken(
                userResponse.getId(), 
                userResponse.getEmail()
        );
        
        return AuthResponse.of(token, userResponse);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Find user by email - use generic error message to not reveal if email exists
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        // Validate password - use same generic error message
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        UserResponse userResponse = UserResponse.fromEntity(user);

        return AuthResponse.of(token, userResponse);
    }

    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            tokenBlacklistService.blacklist(token);
            log.info("Token blacklisted successfully");
        }
    }

    public boolean validateUser(String email, String password) {
        return userService.findByEmail(email)
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(false);
    }

    public boolean isTokenValid(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        // Remove "Bearer " prefix if present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        return jwtTokenProvider.validateToken(token) && !tokenBlacklistService.isBlacklisted(token);
    }
}
