package com.producttagger.backend.user.api;

import com.producttagger.backend.shared.api.ClientIp;
import com.producttagger.backend.shared.security.AuthCookies;
import com.producttagger.backend.shared.security.AuthenticatedUser;
import com.producttagger.backend.shared.security.JwtService;
import com.producttagger.backend.user.application.AuthService;
import com.producttagger.backend.user.application.InvalidRefreshTokenException;
import com.producttagger.backend.user.application.RefreshTokenService;
import com.producttagger.backend.user.domain.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokens;
    private final JwtService jwtService;
    private final AuthCookies authCookies;

    AuthController(AuthService authService,
                   RefreshTokenService refreshTokens,
                   JwtService jwtService,
                   AuthCookies authCookies) {
        this.authService = authService;
        this.refreshTokens = refreshTokens;
        this.jwtService = jwtService;
        this.authCookies = authCookies;
    }

    @PostMapping("/register")
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.name(), request.email(), request.password());

        // Registration logs the user in immediately
        return withSessionCookies(ResponseEntity.status(HttpStatus.CREATED), user)
                .body(UserResponse.from(user));
    }

    @PostMapping("/login")
    ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request, @ClientIp String clientIp) {
        User user = authService.authenticate(request.email(), request.password(), clientIp);

        return withSessionCookies(ResponseEntity.ok(), user).body(UserResponse.from(user));
    }

    /**
     * Rotates the refresh token and issues a fresh access token; the client
     * calls this silently whenever the access token expires.
     */
    @PostMapping("/refresh")
    ResponseEntity<UserResponse> refresh(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, AuthCookies.REFRESH_COOKIE);

        if (cookie == null) {
            throw new InvalidRefreshTokenException();
        }

        RefreshTokenService.Rotation rotation = refreshTokens.rotate(cookie.getValue());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie(rotation.user()))
                .header(HttpHeaders.SET_COOKIE, authCookies.refresh(rotation.refreshToken()).toString())
                .body(UserResponse.from(rotation.user()));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, AuthCookies.REFRESH_COOKIE);

        // Logout revokes the session server-side, not just in the browser
        if (cookie != null) {
            refreshTokens.revoke(cookie.getValue());
        }

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookies.expired().toString())
                .header(HttpHeaders.SET_COOKIE, authCookies.expiredRefresh().toString())
                .build();
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        // Fresh read so the response reflects any profile change since the token was issued
        return UserResponse.from(authService.byId(principal.id()));
    }

    private ResponseEntity.BodyBuilder withSessionCookies(ResponseEntity.BodyBuilder builder, User user) {
        return builder
                .header(HttpHeaders.SET_COOKIE, accessCookie(user))
                .header(HttpHeaders.SET_COOKIE, authCookies.refresh(refreshTokens.issue(user)).toString());
    }

    private String accessCookie(User user) {
        String token = jwtService.issue(new AuthenticatedUser(user.getId(), user.getEmail(), user.getName()));

        return authCookies.session(token).toString();
    }
}
