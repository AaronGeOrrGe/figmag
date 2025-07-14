package com.figmine.backend.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    @Value("${figmine.jwt.secret}")
    private String secret;

    @Value("${figmine.jwt.expiration:86400000}") // Default 24 hours
    private long jwtExpirationMs;

    @Value("${figmine.jwt.issuer:figmine}")
    private String jwtIssuer;

    // Generate token with optional extra claims
    public String generateToken(Map<String, Object> extraClaims, String username) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(username)
                .setIssuer(jwtIssuer)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Generate token with no extra claims
    public String generateToken(String username) {
        return generateToken(Map.of(), username);
    }

    // Extract username from token (subject claim)
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Generic method to extract any claim
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Get expiration time in seconds since epoch
    public long getExpirationFromToken(String token) {
        return extractExpiration(token).getTime() / 1000;
    }

    // Validate token using subject match and expiration
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // Check if the token has expired
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }

    // Extract expiration date from token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Decode and parse all claims from token with proper exception handling
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token expired: {}", ex.getMessage());
            throw ex;
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage());
            throw ex;
        } catch (MalformedJwtException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            throw ex;
        } catch (SignatureException ex) {
            log.warn("Invalid JWT signature: {}", ex.getMessage());
            throw ex;
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage());
            throw ex;
        }
    }

    // Decode base64 key into a secure HMAC key
    private SecretKey getSigningKey() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            if (keyBytes.length < 32) { // Ensure key is at least 256 bits
                throw new IllegalArgumentException("JWT secret key must be at least 256 bits (32 characters) long when base64 decoded");
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException ex) {
            log.error("Invalid JWT secret key configuration: {}", ex.getMessage());
            throw new IllegalStateException("Invalid JWT secret key configuration", ex);
        }
    }

    // Get the remaining validity of a token in seconds
    public long getRemainingValidity(String token) {
        try {
            Date expiration = extractExpiration(token);
            return (expiration.getTime() - System.currentTimeMillis()) / 1000;
        } catch (Exception ex) {
            return -1;
        }
    }

    // Convenience for controller/auth filter compatibility
    public String getUsernameFromJwt(String token) {
        try {
            return extractUsername(token);
        } catch (Exception ex) {
            log.debug("Failed to extract username from token: {}", ex.getMessage());
            return null;
        }
    }

    // Check if token is valid (catch parsing errors)
    public boolean validateJwtToken(String token) {
        try {
            final Claims claims = extractAllClaims(token);
            final String issuer = claims.getIssuer();
            
            // Validate issuer if configured
            if (jwtIssuer != null && !jwtIssuer.isEmpty() && !jwtIssuer.equals(issuer)) {
                log.warn("Invalid token issuer: {}", issuer);
                return false;
            }
            
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
