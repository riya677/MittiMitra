package com.mittimitra.utils;

import java.util.regex.Pattern;

/**
 * Input validation utilities for forms and user input.
 */
public class ValidationUtils {

    // Indian phone number pattern: 10 digits, optionally prefixed with +91 or 0
    private static final Pattern INDIAN_PHONE_PATTERN = 
        Pattern.compile("^(\\+91|91|0)?[6-9][0-9]{9}$");
    
    // Email pattern
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    // Name pattern: letters, spaces, and common Indian name chars
    private static final Pattern NAME_PATTERN = 
        Pattern.compile("^[\\p{L}\\s.'-]{2,50}$");

    /**
     * Validate Indian phone number.
     * Accepts: +91XXXXXXXXXX, 91XXXXXXXXXX, 0XXXXXXXXXX, XXXXXXXXXX
     * Must start with 6-9 after country code
     */
    public static boolean isValidIndianPhone(String phone) {
        if (phone == null || phone.isEmpty()) return false;
        String cleaned = phone.replaceAll("[\\s()-]", "");
        return INDIAN_PHONE_PATTERN.matcher(cleaned).matches();
    }

    /**
     * Format phone number to standard +91XXXXXXXXXX format.
     */
    public static String formatIndianPhone(String phone) {
        if (phone == null) return null;
        String cleaned = phone.replaceAll("[\\s()-]", "");
        
        if (cleaned.startsWith("+91")) {
            return cleaned;
        } else if (cleaned.startsWith("91") && cleaned.length() == 12) {
            return "+" + cleaned;
        } else if (cleaned.startsWith("0") && cleaned.length() == 11) {
            return "+91" + cleaned.substring(1);
        } else if (cleaned.length() == 10) {
            return "+91" + cleaned;
        }
        return phone; // Return as-is if format unknown
    }

    /**
     * Validate email address format.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validate name (letters, spaces, common punctuation).
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        return NAME_PATTERN.matcher(name.trim()).matches();
    }

    /**
     * Check if OTP is valid (6 digits).
     */
    public static boolean isValidOtp(String otp) {
        if (otp == null) return false;
        return otp.matches("^[0-9]{6}$");
    }

    /**
     * Sanitize text input - remove potential script tags and dangerous chars.
     */
    public static String sanitizeInput(String input) {
        if (input == null) return null;
        return input
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;")
            .trim();
    }

    /**
     * Get display-friendly error message for phone validation.
     */
    public static String getPhoneValidationError(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "Phone number is required";
        }
        String cleaned = phone.replaceAll("[\\s()-]", "");
        if (cleaned.length() < 10) {
            return "Phone number is too short";
        }
        if (cleaned.length() > 13) {
            return "Phone number is too long";
        }
        if (!cleaned.matches(".*[6-9]\\d{9}$")) {
            return "Enter a valid Indian mobile number";
        }
        return "Invalid phone number format";
    }
}
