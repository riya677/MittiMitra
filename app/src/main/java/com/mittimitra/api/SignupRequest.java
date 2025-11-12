package com.mittimitra.api;

public class SignupRequest {
    public String username;
    public String email;
    public String password;
    public String confirmPassword;
    public String fullName;
    public String phoneNumber;

    public SignupRequest(String username, String email, String password,
                         String confirmPassword, String fullName, String phoneNumber) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }
}