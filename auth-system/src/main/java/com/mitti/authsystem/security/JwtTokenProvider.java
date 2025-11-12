package com.mitti.authsystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:MittiMitraSecretKeyForJWTAuthenticationSystemPleaseUseStrongKeyInProduction}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private int jwtExpirationMs;

    public String generateToken(String username, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        Key key = new SecretKeySpec(jwtSecret.getBytes(), 0, jwtSecret.getBytes().length, "HmacSHA512");

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Key key = new SecretKeySpec(jwtSecret.getBytes(), 0, jwtSecret.getBytes().length, "HmacSHA512");
        Claims claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Key key = new SecretKeySpec(jwtSecret.getBytes(), 0, jwtSecret.getBytes().length, "HmacSHA512");
        Claims claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Long.class);
    }

    public boolean validateToken(String token) {
        try {
            Key key = new SecretKeySpec(jwtSecret.getBytes(), 0, jwtSecret.getBytes().length, "HmacSHA512");
            Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}