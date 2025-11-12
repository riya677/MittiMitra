package com.mitti.authsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.mitti.authsystem"})
public class AuthSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthSystemApplication.class, args);
    }
}