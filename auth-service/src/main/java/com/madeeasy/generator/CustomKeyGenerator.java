package com.madeeasy.generator;

import com.madeeasy.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
@RequiredArgsConstructor
public class CustomKeyGenerator implements KeyGenerator {

    private final JwtUtils jwtUtils;

    @Override
    public Object generate(Object target, Method method, Object... params) {
        // Custom logic to generate a cache key
        if (method.getName().equals("validateAccessToken")) {
            String accessToken = (String) params[0];
            String email = jwtUtils.getUserName(accessToken); // Assuming jwtUtils is accessible here
            return email + ":accessToken";
        }
        if (method.getName().equals("refreshToken")) {
            String refreshToken = (String) params[0];
            String email = jwtUtils.getUserName(refreshToken); // Assuming jwtUtils is accessible here
            return email + ":refreshToken";
        }
        // Additional logic for other methods
        return null;
    }
}
