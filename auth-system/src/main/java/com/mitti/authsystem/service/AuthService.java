package com.mitti.authsystem.service;

import com.mitti.authsystem.dto.AuthResponse;
import com.mitti.authsystem.dto.LoginRequest;
import com.mitti.authsystem.dto.SignupRequest;
import com.mitti.authsystem.entity.User;
import com.mitti.authsystem.repository.UserRepository;
import com.mitti.authsystem.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user
     */
    public AuthResponse signup(SignupRequest request) {
        AuthResponse response = new AuthResponse();

        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            response.setSuccess(false);
            response.setMessage("Passwords do not match");
            return response;
        }

        // Check if user already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            response.setSuccess(false);
            response.setMessage("Username already exists");
            return response;
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            response.setSuccess(false);
            response.setMessage("Email already exists");
            return response;
        }

        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            response.setSuccess(false);
            response.setMessage("Phone number already exists");
            return response;
        }

        try {
            // Create new user
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setFullName(request.getFullName());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setIsActive(true);
            user.setCreatedAt(LocalDateTime.now());

            User savedUser = userRepository.save(user);
            log.info("User registered successfully: {}", savedUser.getUsername());

            response.setSuccess(true);
            response.setMessage("User registered successfully");
            response.setUserId(savedUser.getId());
            response.setUsername(savedUser.getUsername());

        } catch (Exception e) {
            log.error("Error during signup: {}", e.getMessage());
            response.setSuccess(false);
            response.setMessage("Registration failed: " + e.getMessage());
        }

        return response;
    }

    /**
     * Login user - supports both username and phone number
     */
    public AuthResponse login(LoginRequest request) {
        AuthResponse response = new AuthResponse();

        try {
            String input = request.getUsername(); // Can be username or phone

            log.info("Attempting login with input: {}", input);

            // Try to find by username first
            var userOptional = userRepository.findByUsername(input);

            // If not found by username, try to find by phone number
            if (userOptional.isEmpty()) {
                log.info("User not found by username, trying phone number: {}", input);
                userOptional = userRepository.findByPhoneNumber(input);
            }

            if (userOptional.isEmpty()) {
                log.warn("Login failed: User not found for input: {}", input);
                response.setSuccess(false);
                response.setMessage("Invalid username/phone or password");
                return response;
            }

            User user = userOptional.get();

            // Check if user is active
            if (!user.getIsActive()) {
                response.setSuccess(false);
                response.setMessage("User account is inactive");
                return response;
            }

            // Validate password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.warn("Login failed: Invalid password for user: {}", input);
                response.setSuccess(false);
                response.setMessage("Invalid username/phone or password");
                return response;
            }

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate token
            String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId());

            response.setSuccess(true);
            response.setMessage("Login successful");
            response.setToken(token);
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());

            log.info("User logged in successfully: {}", user.getUsername());

        } catch (Exception e) {
            log.error("Error during login: {}", e.getMessage());
            response.setSuccess(false);
            response.setMessage("Login failed: " + e.getMessage());
        }

        return response;
    }

    /**
     * Validate token
     */
    public AuthResponse validateToken(String token) {
        AuthResponse response = new AuthResponse();

        if (!jwtTokenProvider.validateToken(token)) {
            response.setSuccess(false);
            response.setMessage("Invalid token");
            return response;
        }

        try {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            response.setSuccess(true);
            response.setMessage("Token is valid");
            response.setUsername(username);
            response.setUserId(userId);

        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            response.setSuccess(false);
            response.setMessage("Token validation failed");
        }

        return response;
    }
}