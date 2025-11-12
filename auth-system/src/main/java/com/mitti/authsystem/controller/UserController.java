package com.mitti.authsystem.controller;

import com.mitti.authsystem.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile/{userId}")
    @PreAuthorize("@userService.checkUsernameExists(#userId)")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
        log.info("Fetching profile for user: {}", userId);
        Map<String, Object> response = userService.getUserProfile(userId);
        if ((Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<?> updateUserProfile(
            @PathVariable Long userId,
            @RequestBody Map<String, String> updates) {
        log.info("Updating profile for user: {}", userId);
        Map<String, Object> response = userService.updateUserProfile(userId, updates);
        if ((Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/change-password/{userId}")
    public ResponseEntity<?> changePassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        log.info("Password change request for user: {}", userId);
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Old password and new password are required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Map<String, Object> response = userService.changePassword(userId, oldPassword, newPassword);
        if ((Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @DeleteMapping("/account/{userId}")
    public ResponseEntity<?> deleteAccount(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        log.info("Account deletion request for user: {}", userId);
        String password = request.get("password");

        if (password == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Password is required for account deletion");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Map<String, Object> response = userService.deleteAccount(userId, password);
        if ((Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @GetMapping("/check-username/{username}")
    public ResponseEntity<?> checkUsername(@PathVariable String username) {
        Map<String, Object> response = new HashMap<>();
        boolean exists = userService.checkUsernameExists(username);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-email/{email}")
    public ResponseEntity<?> checkEmail(@PathVariable String email) {
        Map<String, Object> response = new HashMap<>();
        boolean exists = userService.checkEmailExists(email);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
}