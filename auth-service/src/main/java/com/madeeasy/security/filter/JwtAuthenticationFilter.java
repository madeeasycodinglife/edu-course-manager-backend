package com.madeeasy.security.filter;

import com.madeeasy.entity.Token;
import com.madeeasy.entity.User;
import com.madeeasy.exception.TokenException;
import com.madeeasy.repository.TokenRepository;
import com.madeeasy.repository.UserRepository;
import com.madeeasy.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.isEmpty(authorizationHeader)) {
            filterChain.doFilter(request, response);
            return;
        }
        String accessToken = authorizationHeader.substring(7);
        String userName = jwtUtils.getUserName(accessToken);

        User user = userRepository.findByEmail(userName).orElseThrow(() -> new UsernameNotFoundException("user not found with email " + userName));
        Token token = tokenRepository.findByToken(accessToken).orElseThrow(() -> new TokenException("Token Not found"));


        try {
            if (token.isExpired() && token.isRevoked()) {
                throw new TokenException("Token is expired or revoked");
            }
        } catch (TokenException e) {
            /**
             * here I have catch exception as we know exception thrown form filter will not be handled in the @RestControllerAdvice class
             */
            response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }


        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (
                    jwtUtils.validateToken(accessToken, userName) &&
                            user.isAccountNonExpired() &&
                            user.isAccountNonLocked() &&
                            user.isCredentialsNonExpired() &&
                            user.isEnabled()
            ) {

                List<SimpleGrantedAuthority> authorities = jwtUtils.getRolesFromToken(accessToken)
                        .stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userName, null, authorities);

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            }
        }
        filterChain.doFilter(request, response);
    }
}
