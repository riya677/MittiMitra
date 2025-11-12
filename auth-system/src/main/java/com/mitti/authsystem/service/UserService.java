package com.mitti.authsystem.service;

import com.mitti.authsystem.dto.UserDTO;
import com.mitti.authsystem.entity.User;
import com.mitti.authsystem.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Map<String, Object> getUserProfile(Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<User> user = userRepository.findById(userId);
            if (user.isPresent()) {
                response.put("success", true);
                response.put("data", UserDTO.fromUser(user.get()));
                return response;
            } else {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching user profile");
            log.error("Error fetching user profile: {}", e.getMessage());
            return response;
        }
    }

    @Transactional
    public Map<String, Object> updateUserProfile(Long userId, Map<String, String> updates) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<User> optionalUser = userRepository.findById(userId);
            if (!optionalUser.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }

            User user = optionalUser.get();

            // Update fields if provided
            if (updates.containsKey("fullName")) {
                user.setFullName(updates.get("fullName"));
            }
            if (updates.containsKey("phoneNumber")) {
                user.setPhoneNumber(updates.get("phoneNumber"));
            }
            if (updates.containsKey("email")) {
                String newEmail = updates.get("email");
                if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                    response.put("success", false);
                    response.put("message", "Email already exists");
                    return response;
                }
                user.setEmail(newEmail);
            }

            user.setUpdatedAt(LocalDateTime.now());
            User updatedUser = userRepository.save(user);

            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("data", UserDTO.fromUser(updatedUser));
            log.info("User profile updated: {}", user.getUsername());

            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating profile");
            log.error("Error updating profile: {}", e.getMessage());
            return response;
        }
    }

    @Transactional
    public Map<String, Object> changePassword(Long userId, String oldPassword, String newPassword) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<User> optionalUser = userRepository.findById(userId);
            if (!optionalUser.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }

            User user = optionalUser.get();

            // Verify old password
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                response.put("success", false);
                response.put("message", "Old password is incorrect");
                return response;
            }

            // Validate new password
            if (newPassword.length() < 6) {
                response.put("success", false);
                response.put("message", "New password must be at least 6 characters");
                return response;
            }

            // Update password
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            response.put("success", true);
            response.put("message", "Password changed successfully");
            log.info("Password changed for user: {}", user.getUsername());

            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error changing password");
            log.error("Error changing password: {}", e.getMessage());
            return response;
        }
    }

    @Transactional
    public Map<String, Object> deleteAccount(Long userId, String password) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<User> optionalUser = userRepository.findById(userId);
            if (!optionalUser.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }

            User user = optionalUser.get();

            // Verify password before deletion
            if (!passwordEncoder.matches(password, user.getPassword())) {
                response.put("success", false);
                response.put("message", "Password is incorrect");
                return response;
            }

            userRepository.deleteById(userId);
            response.put("success", true);
            response.put("message", "Account deleted successfully");
            log.info("Account deleted for user: {}", user.getUsername());

            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting account");
            log.error("Error deleting account: {}", e.getMessage());
            return response;
        }
    }

    @Transactional(readOnly = true)
    public boolean checkUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean checkEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}