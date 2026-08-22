package com.producttagger.backend.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.List;

@Component
class JwtCookieAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    JwtCookieAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Cookie cookie = WebUtils.getCookie(request, AuthCookies.TOKEN_COOKIE);

        if (cookie != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            AuthenticatedUser user = jwtService.parse(cookie.getValue());

            // Invalid/expired tokens simply stay anonymous; the entry point
            // produces the 401 for protected endpoints
            if (user != null) {
                var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
