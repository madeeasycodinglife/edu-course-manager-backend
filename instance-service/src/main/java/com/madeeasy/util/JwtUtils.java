package com.madeeasy.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtils {
    private static final String SECRET_KEY = "1adf0a4782f6e5674a79747fe58ea851b7581658d3715b12f4e0b12e999f307e";
    @Autowired
    private PasswordEncoder passwordEncoder;
    Logger logger = LoggerFactory.getLogger(JwtUtils.class);


    public Claims getAllClaims(String token) {


        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
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
