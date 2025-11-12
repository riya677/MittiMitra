package com.mitti.authsystem.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestPath = request.getRequestURI();

            // Skip filtering for public endpoints
            if (requestPath.startsWith("/api/auth/") ||
                    requestPath.contains(".html") ||
                    requestPath.startsWith("/static/")) {
                filterChain.doFilter(request, response);
                return;
            }

            // For other endpoints, continue with normal filter chain
            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            log.error("Filter error: {}", ex.getMessage());
            filterChain.doFilter(request, response);
        }
    }
}