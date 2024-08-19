package com.madeeasy.util;


import com.madeeasy.exception.TokenValidationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class JwtUtils {

    private static final String SECRET_KEY = "1adf0a4782f6e5674a79747fe58ea851b7581658d3715b12f4e0b12e999f307e";
    private final Logger logger = LoggerFactory.getLogger(JwtUtils.class);


    public String generateAccessToken(String email, List<String> roles) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .claim("roles", roles)
                .issuer("madeeasycodinglife")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)))
                .signWith(getSignKey())
                .compact();
    }

    public String generateRefreshToken(String email, List<String> roles) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .claim("roles", roles)
                .issuer("madeeasycodinglife")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(5)))
                .signWith(getSignKey())
                .compact();
    }

    public Claims getAllClaims(String token) {

        try {
            return Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (MalformedJwtException e) {
            // Invalid JWT token
            // Log or handle the error as needed
            logger.warn("Invalid JWT token: {}", e.getMessage());
            throw new TokenValidationException("Invalid JWT token", HttpStatus.UNAUTHORIZED);
        } catch (ExpiredJwtException e) {
            // JWT token is expired
            // Log or handle the error as needed
            logger.warn("JWT token is expired: {}", e.getMessage());
            throw new TokenValidationException("JWT token is expired", HttpStatus.UNAUTHORIZED);
        } catch (UnsupportedJwtException e) {
            // JWT token is unsupported
            // Log or handle the error as needed
            logger.warn("JWT token is unsupported: {}", e.getMessage());
            throw new TokenValidationException("JWT token is unsupported", HttpStatus.UNAUTHORIZED);
        } catch (IllegalArgumentException e) {
            // JWT claims string is empty
            // Log or handle the error as needed
            logger.warn("JWT claims string is empty: {}", e.getMessage());
            throw new TokenValidationException("JWT claims string is empty", HttpStatus.UNAUTHORIZED);
        } catch (SignatureException e) {
            // JWT signature validation failed
            // Log or handle the error as needed
            logger.warn("JWT signature validation failed: {}", e.getMessage());
            throw new TokenValidationException("JWT signature validation failed", HttpStatus.UNAUTHORIZED);
        }
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Date getExpirationDate(String token) {
        return getAllClaims(token).getExpiration();
    }

    public String getUserName(String token) {
        return getAllClaims(token).getSubject();
    }


    public boolean isTokenExpired(String token) {
        Date expirationDate = getExpirationDate(token);
        return expirationDate.before(new Date(System.currentTimeMillis()));
    }

    public boolean validateToken(String token, String userName) {
        getAllClaims(token);
        return userName.equals(getUserName(token)) && !isTokenExpired(token);
    }


    public List<String> getRolesFromToken(String token) {
        Claims claims = getAllClaims(token);

        Object rolesObject = claims.get("roles");

        List<String> roles = new ArrayList<>();

        if (rolesObject instanceof List<?>) {
            for (Object role : (List<?>) rolesObject) {
                if (role instanceof String) {
                    roles.add((String) role);
                }
                // Add additional checks or handling if needed
            }
        }
        return roles;
    }
}
