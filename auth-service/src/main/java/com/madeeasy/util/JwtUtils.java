package com.madeeasy.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    Logger logger = LoggerFactory.getLogger(JwtUtils.class);


    public String generateAccessToken(String email, List<String> roles) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .claim("roles", roles)
                .issuer("madeeasycodinglife")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)))
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
                .expiration(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)))
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
            System.out.println(e.getMessage());
        } catch (ExpiredJwtException e) {
            // JWT token is expired
            // Log or handle the error as needed
            System.out.println(e.getMessage());
        } catch (UnsupportedJwtException e) {
            // JWT token is unsupported
            // Log or handle the error as needed
            System.out.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            // JWT claims string is empty
            // Log or handle the error as needed
            System.out.println(e.getMessage());
        } catch (SignatureException e) {
            // JWT signature validation failed
            // Log or handle the error as needed
            System.out.println(e.getMessage());
        }
        return null;
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Date getExpirationDate(String token) {
        Claims claims = getAllClaims(token);
        return (claims != null) ? claims.getExpiration() : null;
    }

    public String getUserName(String token) {
        Claims claims = getAllClaims(token);
        return (claims != null) ? claims.getSubject() : null;
    }


    public boolean isTokenExpired(String token) {
        Date expirationDate = getExpirationDate(token);
        return expirationDate.before(new Date(System.currentTimeMillis()));
    }

    public boolean validateToken(String token, String userName) {
        try {
            Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            boolean isValid = userName.equals(getUserName(token)) && !isTokenExpired(token);
            return isValid;
        } catch (MalformedJwtException e) {
            // Invalid JWT token
            // Log or handle the error as needed
            System.out.println(e.getMessage());
        } catch (ExpiredJwtException e) {
            // JWT token is expired
            // Log or handle the error as needed
            System.out.println(e.getMessage());
        } catch (UnsupportedJwtException e) {
            // JWT token is unsupported
            // Log or handle the error as needed
            System.out.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            // JWT claims string is empty
            // Log or handle the error as needed
            System.out.println(e.getMessage());
        } catch (SignatureException e) {
            // JWT signature validation failed
            // Log or handle the error as needed
            System.out.println(e.getMessage());
        }
        return false;
    }


    public List<String> getRolesFromToken(String token) {
        Claims claims = getAllClaims(token);

        if (claims != null) {
            Object rolesObject = claims.get("roles");

            if (rolesObject instanceof List<?>) {
                List<String> roles = new ArrayList<>();

                for (Object role : (List<?>) rolesObject) {
                    if (role instanceof String) {
                        roles.add((String) role);
                    }
                    // Add additional checks or handling if needed
                }

                return roles;
            }
        }

        return null;
    }
}