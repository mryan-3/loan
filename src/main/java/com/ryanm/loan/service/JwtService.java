package com.ryanm.loan.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    @Value("${jwt.refresh-token.expiration}")
    private long refreshExpiration;
    
    // Generate token for a user
    public String generateToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return createToken(claims, username);
    }
    
    // Create the actual token
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
        .signWith(getSigningKey())
                .compact();
    }
    
    // Get the secret key for signing
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
    
    // Extract username from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    // Extract user ID from token
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }
    
    // Extract expiration date from token
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    // Extract any claim from token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    // Extract all claims from token
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    // Check if token is expired
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    // Validate token
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    // Generate refresh token
    public String generateRefreshToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tokenType", "refresh"); // Mark as refresh token
        return createRefreshToken(claims, username);
    }

    private String createRefreshToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))  // 👈 7 days
                .signWith(getSigningKey())
                .compact();
    }

    public Boolean isRefreshToken(String token) {
        try {
            String tokenType = extractClaim(token, claims -> claims.get("tokenType", String.class));
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    // 👈 NEW: Refresh access token using refresh token
    public String refreshAccessToken(String refreshToken) {
        if (!isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }
        
        String username = extractUsername(refreshToken);
        Long userId = extractUserId(refreshToken);
        
        if (isTokenExpired(refreshToken)) {
            throw new RuntimeException("Refresh token expired");
        }
        
        // Generate new access token
        return generateToken(username, userId);
    }


}